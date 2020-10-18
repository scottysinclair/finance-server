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
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.model.Category
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QTransaction
import scott.financeserver.import.BankAustriaRow
import scott.financeserver.import.parseBankAustria
import java.util.*
import javax.annotation.PostConstruct

import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Transaction as ETransaction
import scott.financeserver.data.model.Category as ECategory


data class Account(val id: UUID, val name: String, val numberOfTransactions : Long)
data class AccountResponse(val accounts: List<Account>)

@RestController
class UploadController {

    @Autowired
    private lateinit var env: Environment

    private lateinit var unknownCategory : ECategory

    @PostConstruct
    fun postConstruct() {
        unknownCategory = DataEntityContext(env).use { ctx ->
            ctx.performQuery(QCategory().apply { name().equal("unknown") }).singleResult
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
    fun uploadTransactions(@PathVariable accountName : String, @RequestParam file : MultipartFile) = DataEntityContext(env).use { ctx ->
        ctx.autocommit = false
        val account = ctx.performQuery(QAccount().apply { name().equal(accountName) }).singleResult
        parseBankAustria(file.bytes) { seq ->
            seq.map { row ->
                ctx.newModel(ETransaction::class.java).let { t->
                    t.content = toContent(row)
                    t.contentHash = toHash(row)
                    t.account = account
                    t.date = row.date
                    t.category = analyseCategory(row)
                    t.userCategorized = false
                    t.amount = row.amount
                    t.comment = null
                    t.important = false
                }
            }
                .batchesOf(100)
                .forEach { listOfT ->
                    ctx.persist(PersistRequest().apply {
                        listOfT.forEach { t -> insert(t) }
                    })
                }
        }
        ctx.commit()
    }

    private fun analyseCategory(row: BankAustriaRow): ECategory {
        return unknownCategory
    }
}

fun <T> Sequence<T>.batchesOf(num : Int) = iterator().let { i ->
    generateSequence {
        val list = mutableListOf<T>()
        while (i.hasNext() && list.size < num) {
            list.add(i.next())
        }
        list
    }
}

fun toContent(row : BankAustriaRow) =
    row.run {listOf(date, currency, amount.toString(), bookingText, reason, senderBic) }
    .joinToString(" | ")

fun toHash(row : BankAustriaRow) = DigestUtils.sha1Hex(toContent(row))

