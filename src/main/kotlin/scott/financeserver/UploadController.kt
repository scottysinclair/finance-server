package scott.financeserver.upload

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
import scott.financeserver.*
import scott.financeserver.data.DataEntityContext

import scott.financeserver.data.query.*
import scott.financeserver.import.BankAustriaRow
import scott.financeserver.import.parseBankAustria
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.util.*
import javax.annotation.PostConstruct
import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Feed as EFeed
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.CategoryMatcher as ECategoryMatcher
import scott.financeserver.data.model.Transaction as ETransaction
import scott.financeserver.data.model.EndOfMonthStatement as EEndOfMonthStatement

data class Account(val id: UUID, val name: String, val numberOfTransactions: Long)
data class AccountResponse(val accounts: List<Account>)

data class Feed(val id: UUID, val file: String, val dateImported: Long)
data class FeedResponse(val feeds: List<Feed>)

data class Statement(val id: UUID, val accountId: String, val date: Long, val amount: BigDecimal)
data class StatementResponse(val statements: List<Statement>)


data class Category(val id: UUID, val name: String, val matchers : List<CategoryMatcher>)
data class CategoryMatcher(val id: UUID, val pattern: String)
data class CategoryResponse(val categories: List<Category>)

data class UploadResult(val count: Int? = null, val error: String? = null)

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

    @GetMapping("/statement")
    fun getStatements() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QEndOfMonthStatement().apply {
            orderBy(date(), true)
        }).list.map {
            Statement(
                id = it.id,
                accountId = it.account.id.toString(),
                date = it.date.time,
                amount = it.amount
            )}
            .let { StatementResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @GetMapping("/category")
    fun getCategories() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QCategory().apply {
            joinToMatchers()
            orderBy(name(), true)
        }).list.map {
            Category(
                id = it.id,
                name = it.name,
                matchers = it.matchers.map { m -> CategoryMatcher(
                    id = m.id,
                    pattern = m.pattern) }
            )}
            .let { CategoryResponse(it) }
    }.also { Runtime.getRuntime().gc() }

    @PutMapping("/category/{categoryName}")
    fun saveCategory(@PathVariable categoryName : String) = DataEntityContext(env).use { ctx ->
        ctx.persist(PersistRequest().save(ctx.newModel(ECategory::class.java).also { ec ->
            ec.name = categoryName
        }))
    }.also { Runtime.getRuntime().gc() }

    @PutMapping("/category/{categoryName}/matcher/{pattern}")
    fun saveCategory(@PathVariable categoryName : String, @PathVariable pattern : String) = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QCategory().apply { where(name().equal(categoryName)) }).singleResult.let { category ->
            ctx.persist(PersistRequest().save(ctx.newModel(ECategoryMatcher::class.java).also { ec ->
                ec.category = category
                ec.pattern = pattern
            }))
        }
    }.also { Runtime.getRuntime().gc() }

    @GetMapping("/feed")
    fun getFeeds() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QFeed().apply {
            orderBy(dateImported(), true)
        }).list.map {
            Feed(
                id = it.id,
                file = it.file,
                dateImported = it.dateImported.time) }
            .let { FeedResponse(it) }
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

    @DeleteMapping("/feed/{feedId}")
    fun deleteFeed(@PathVariable feedId : UUID) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        val feed = ctx.performQuery(QFeed().apply { where(id().equal(feedId)) }).singleResult
        if (ctx.performQuery(QFeed().apply {
                where(accountId().equal(feed.account.id))
                and(dateImported().greater(feed.dateImported)) }).list.isNotEmpty()) {
            throw IllegalStateException("Can only delete newest feed")
        }
        try {
            ctx.streamObjectQuery(QTransaction().apply {
                select(id(), date())
                where(feedId().equal(feedId))
                orderBy(date(), false)
            })
            .toSequence()
            .map { it.apply { entity.constraints.set(EntityConstraint.mustExistAndDontFetch()) } }
            .map {
                it.also {
               //     println("Deleting ${it.date}")
                    ctx.persist(PersistRequest().delete(it))
                }
            }
            .map { it.date.toYearMonth() }
            .distinct()
            .toList()
            .last().let { yearAndMonth ->
                    println("Last year and month $yearAndMonth")
                    ctx.performQuery(QEndOfMonthStatement().apply {
                        where(accountId().equal(feed.account.id))
                        and(date().greaterOrEqual(yearAndMonth.atDay(1).atStartOfDay().toDate()))
                    }).list.let { stmts ->
                        if (stmts.isNotEmpty()) ctx.persist(PersistRequest().apply {
                            stmts.forEach { stmt -> delete(stmt) }
                        })
                    }
                }
            ctx.persist(PersistRequest().delete(feed))
            ctx.commit()
        }
        catch(x : Exception) {
            x.printStackTrace()
            runCatching { ctx.rollback() }.onFailure { x -> x.printStackTrace() }
        }
     }.also { Runtime.getRuntime().gc() }

    @PostMapping("upload/{accountName}")
    fun uploadTransactions(@PathVariable accountName : String, @RequestParam("file") file : MultipartFile, @RequestParam("currentBalance") currentBalance : BigDecimal?) : UploadResult {
        return DataEntityContext(env).use { ctx ->
            ctx.autocommit = false
            val categories = ctx.performQuery(QCategory()).list
            val alreadyImported = { contentHash : String -> ctx.performQuery(QTransaction().apply {
                contentHash().equal(contentHash) }).list.isNotEmpty() }

            runCatching {
                val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
                val feed = ctx.newModel(EFeed::class.java).also {
                    it.account = account
                    it.dateImported = Date()
                    it.file = file.originalFilename
                }.also { ctx.persist(PersistRequest().insert(it)) }

                println("Processing bank austria upload...")
                val hashes = parseBankAustria(file.bytes) { seq ->
                    seq.map { row -> toHash(row) }.toSet()
                }
                val matchingHashes = ctx.performQuery(QTransaction().apply {
                        select(id(), contentHash())
                        where(contentHash().`in`(hashes))
                    }).list.map(ETransaction::getContentHash).toSet()
                println("Found ${matchingHashes.size} matches")

                parseBankAustria(file.bytes) { seq ->
                    seq.mapIndexed {i, row ->
                        if (row.currency != "EUR") throw IllegalStateException("only EUR please")
                        println("$i, Processing row ${row.date} ${row.amount}")
                        Triple(toContent(row), toHash(row), row)
                    }.filter { (_, hash, _) -> matchingHashes.contains(hash).not()}
                    .toList()
                    .mapIndexed { i, (content, hash, row) ->
                        ctx.persist(PersistRequest().insert(ctx.newModel(ETransaction::class.java).also { t ->
                            t.account = account
                            t.feed = feed
                            t.content = content
                            t.contentHash = hash
                            t.description = "${row.bookingText} ${row.reason} ${row.senderBic}"
                            t.date = row.date
                            t.category = analyseCategory(row, categories)
                            t.userCategorized = false
                            t.amount = row.amount
                            t.comment = null
                            t.important = false})).let { 1 }.also {
                            println("$i inserting ${row.date} ${row.amount} $hash")
                        }
                        }.sum()
                }.let { count ->
                    if (count > 0 && matchingHashes.isNotEmpty()) {
                        generateEndOfMonthStatementsFrom(account, ctx)
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
        .toList().let { list ->
                list.subList(0, list.size-1).forEach{(endOfMonth, balance) ->
                ctx.persist(PersistRequest().insert(ctx.newModel(EEndOfMonthStatement::class.java).also {
                    it.account = account
                    it.date = endOfMonth
                    it.amount = balance
                }))
            }
        }
    }

    private fun generateEndOfMonthStatementsFrom(account : EAccount, ctx : DataEntityContext) {
        val lastStmt = ctx.performQuery(QEndOfMonthStatement().apply {
            where(accountId().equal(account.id))
            orderBy(date(), false)
        }, RuntimeProperties().fetchSize(10)).list.first()

        var balance = lastStmt.amount

        ctx.streamObjectQuery(QTransaction().apply {
            where(accountId().equal(account.id))
            and(date().greater(lastStmt.date))
            orderBy(date(), true)
        }).toSequence()
            .sequenceOfMonthlyTransactions()
            .map {
                balance += it.map(ETransaction::getAmount).reduce{a, b->a+b}
                it.first().date.toEndOfMonth() to balance
            }
            .toList().let { list ->
                list.subList(0, list.size-1).forEach { (endOfMonth, balance) ->
                    ctx.persist(PersistRequest().save(ctx.newModel(EEndOfMonthStatement::class.java).also {
                        it.account = account
                        it.date = endOfMonth
                        it.amount = balance
                    }))
                }
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

