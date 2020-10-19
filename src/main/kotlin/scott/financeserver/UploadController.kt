package scott.financeserver

import org.apache.commons.codec.digest.DigestUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.multipart.MultipartFile
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.persist.PersistRequest
import scott.barleydb.api.query.RuntimeProperties
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Feed
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QEndOfMonthStatement
import scott.financeserver.data.query.QTransaction
import scott.financeserver.import.BankAustriaRow
import scott.financeserver.import.parseBankAustria
import java.math.BigDecimal
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct
import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.Transaction as ETransaction
import scott.financeserver.data.model.EndOfMonthStatement as EEndOfMonthStatement


data class Account(val id: UUID, val name: String, val numberOfTransactions : Long)
data class AccountResponse(val accounts: List<Account>)

data class UploadResult(val count : Int? = null, val error : String? = null)

@RestController
class UploadController {

    @Autowired
    private lateinit var env: Environment

    @PostConstruct
    fun postConstruct() {
        DataEntityContext(env).let { ctx ->
            ctx.performQuery(QAccount().apply { name().equal("Bank Austria") }).list.firstOrNull()
                ?: ctx.newModel(EAccount::class.java).also {
                    it.name = "Bank Austria"
                    ctx.persist(PersistRequest().insert(it))
                }

            ctx.performQuery(QCategory().apply { name().equal("unknown") }).list.firstOrNull()
                ?: ctx.newModel(ECategory::class.java).also {
                    it.name = "unknown"
                    ctx.persist(PersistRequest().insert(it))
                }
        }
    }

    @GetMapping("/account")
    fun getAccounts() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QAccount().apply {
            orderBy(name(), true)
        }).list.map {
            Account(
                id = it.id,
                name = it.name,
                numberOfTransactions = ctx.performQuery(QTransaction().apply{
                       where(accountId().equal(it.id))
                }).list.size.toLong()
            ) }
            .let { AccountResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @PutMapping("/account/{accountName}")
    fun addAccount(@PathVariable accountName : String) = DataEntityContext(env).use { ctx ->
        ctx.persist(PersistRequest().insert(ctx.newModel(EAccount::class.java).apply {
            name = accountName
        }))
    }.also { Runtime.getRuntime().gc() }

    @DeleteMapping("/account/{accountId}")
    fun deleteAccount(@PathVariable accountId : UUID) = DataEntityContext(env).use { ctx ->
        if (ctx.performQuery(QTransaction().apply {
            select(id())
            where(accountId().equal(accountId))
        }).list.isNotEmpty()) throw HttpClientErrorException(HttpStatus.BAD_REQUEST, "account has transactions...")

        ctx.persist(PersistRequest().delete(ctx.newModel(EAccount::class.java, accountId, EntityConstraint.mustExistAndDontFetch())))
    }.also { Runtime.getRuntime().gc() }

    @PostMapping("upload/{accountName}")
    fun uploadTransactions(@PathVariable accountName : String, @RequestParam("file") file : MultipartFile, @RequestParam("currentBalance") currentBalance : BigDecimal?) : UploadResult {
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            val categories = ctx.performQuery(QCategory()).list
            val lastTransaction = ctx.performQuery(QTransaction().apply {
                orderBy(date(), false)
            }, RuntimeProperties().fetchSize(2)).list.firstOrNull()

            if (lastTransaction == null && currentBalance == null) {
                return UploadResult(error = "current balance must be specififed")
            }
            runCatching {
                val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
                val feed = ctx.newModel(Feed::class.java).also {
                    it.account = account
                    it.file = file.name
                }.also { ctx.persist(PersistRequest().insert(it)) }

                var foundMatch = false
                println("Processing bank austria upload...")
                parseBankAustria(file.bytes) { seq ->
                    seq.filter { !foundMatch  }
                        .mapIndexed {i, row ->
                            println("$i, Processing row ${row.date} ${row.amount}")
                            Triple(toContent(row), toHash(row), row)
                        }.mapIndexed { i, (content, hash, row) ->
                            foundMatch =  lastTransaction != null && hash == lastTransaction.contentHash
                            if (!foundMatch)
                                ctx.persist(PersistRequest().insert(ctx.newModel(ETransaction::class.java).also { t ->
                                    t.account = account
                                    t.feed = feed
                                    t.content = content
                                    t.contentHash = hash
                                    t.date = row.date
                                    t.category = analyseCategory(row, categories)
                                    t.userCategorized = false
                                    t.amount = row.amount
                                    t.comment = null
                                    t.important = false
                                })).let { 1 }.also { println("$i inserting ${row.date} ${row.amount} $hash")  }
                            else 0.also { println("$i found content hash $hash") }
                        }.sum()
                }.let { count ->
                    if (count > 0 && foundMatch) {
                        println("Found existing record ${lastTransaction!!.date} with content hash ${lastTransaction!!.contentHash}")
                        generateEndOfMonthStatementsFrom(account, lastTransaction!!, ctx)
                        UploadResult(count = count)
                    } else if (count > 0 && currentBalance != null){
                        generateEndOfMonthStatementsGoingBackwards(account, currentBalance, ctx)
                        UploadResult(count = count)
                    }
                    else if (count == 0) UploadResult(error = "no records")
                    else UploadResult(error = "current balance required")
                }.apply { if (count != null) ctx.commit() }
            }
                .getOrElse { x ->
                    x.printStackTrace()
                    runCatching { ctx.rollback() }.onFailure { x2 -> println(x2) }
                    UploadResult(error = x.stackTraceToString())
                }
        }
    }

    private fun generateEndOfMonthStatementsGoingBackwards(account : EAccount, newstBalance: BigDecimal, ctx : DataEntityContext) {
        var balance = newstBalance
        ctx.streamObjectQuery(QTransaction().apply {
            where(accountId().equal(account.id))
            orderBy(date(), false)
        })
        .toSequence()
        .sequenceOfMonthlyTransactions()
        .map {
            balance -= it.map(ETransaction::getAmount)
                .reduce { a, b -> a + b }
            it.first().date.toEndOfLastMonth() to balance
        }
        .forEach { (endOfMonth, balance) ->
            ctx.persist(PersistRequest().insert(ctx.newModel(EEndOfMonthStatement::class.java).also {
                it.account = account
                it.date = endOfMonth
                it.amount = balance
            }))
        }

    }

    private fun generateEndOfMonthStatementsFrom(account : EAccount, transaction : ETransaction, ctx : DataEntityContext) {
        val fromEndOfMonth = transaction.date.toEndOfLastMonth()
        val endOfLastMonthBalance = ctx.performQuery(QEndOfMonthStatement().apply {
            where(accountId().equal(account.id))
            and(date().equal(fromEndOfMonth))
        }).singleResult.amount

        var balance = endOfLastMonthBalance

        ctx.streamObjectQuery(QTransaction().apply {
            where(accountId().equal(account.id))
            and(date().greater(fromEndOfMonth))
            orderBy(date(), true)
        }).toSequence()
            .sequenceOfMonthlyTransactions()
            .map {
                balance += it.map(ETransaction::getAmount).reduce{a, b->a+b}
                it.first().date.toEndOfMonth() to balance
            }
            .forEach { (endOfMonth, balance) ->
                ctx.persist(PersistRequest().save(ctx.newModel(EEndOfMonthStatement::class.java).also {
                    it.account = account
                    it.date = endOfMonth
                    it.amount = balance
                }))
            }
    }

    private fun analyseCategory(row: BankAustriaRow, categories : List<ECategory>): ECategory {
        return categories.filter { it.name == "unknown" }.first()
    }
}


fun toContent(row : BankAustriaRow) =
    row.run {listOf(date, currency, amount.toString(), bookingText, reason, senderBic) }
    .joinToString(" | ")

fun toHash(row : BankAustriaRow) = DigestUtils.sha1Hex(toContent(row))

