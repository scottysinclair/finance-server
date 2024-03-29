package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import scott.barleydb.api.core.Environment
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.accounts.Transaction
import scott.financeserver.accounts.TransactionsResponse
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.*
import java.math.BigDecimal
import java.time.*
import java.util.*
import javax.annotation.PostConstruct
import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.Transaction as ETransaction

data class CategoriesResponse(val categories : List<Category>)
data class Category(val id : UUID, val name : String)

data class MonthResponse(val id : UUID, val date : Long, val startingBalance : BigDecimal)


@RestController
class DataEntryController {

    @Autowired
    private lateinit var env: Environment

    private lateinit var categories: List<ECategory>
    private lateinit var accounts: List<EAccount>

    private val lookupCategory = { name: String -> categories.first { it.name == name } }
    private val lookupAccount = { name: String -> accounts.first { it.name == name } }

    @PostConstruct
    fun postConstruct() {
        DataEntityContext(env).use { ctx ->
            categories = ctx.performQuery(QCategory()).list
            accounts = ctx.performQuery(QAccount()).list
        }
    }


    @GetMapping("/api/categories")
    fun getCategories() = DataEntityContext(env).use {ctx ->
            ctx.performQuery(QCategory()).list.map {
            Category(
                id = it.id,
                name = it.name
            )
        }
        .let(::CategoriesResponse)
    }

    @PostMapping("/api/transaction", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun saveTransactions(@RequestBody transaction: Transaction) : Transaction {
        DataEntityContext(env).use { ctx ->
            ctx.performQuery(QCategory().apply { where(name().equal(transaction.category)) }).singleResult.let { cat ->
                    return ctx.performQuery(QTransaction().apply { where(id().equal(transaction.id)) }).singleResult.apply {
                        if (cat.name.equals("unknown")) {
                            this.userCategorized = false
                        }
                        else if (this.category.id != cat.id) {
                            this.userCategorized = true
                        }
                        this.description = transaction.description
                        this.category = cat
                    }
                        .also {
                            ctx.persist(PersistRequest().save(it))
                            println("SAVED TRANSACTION $transaction")
                        }.forClient()
                }
            }
    }


    @GetMapping("/api/transaction/{year}")
    fun getTransactionsForYear(@PathVariable year : Int) : TransactionsResponse {
        val from = YearMonth.of(year, 1).atDay(1).atStartOfDay().toDate()
        val to = YearMonth.of(year, 12).atEndOfMonth().atTime(LocalTime.MAX).toDate()

        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QTransaction().apply {
                joinToCategory()
                joinToAccount()
                where(duplicate().equal(false).and(date().greaterOrEqual(from).and(date().lessOrEqual(to))))
                orderBy(date(), true)
            })
                .list.map { it.forClient() }
                .let(::TransactionsResponse)
        }
    }

    @GetMapping("/api/transaction/{year}/{month}")
    fun getTransactionsForMonth(@PathVariable year : Int, @PathVariable month : Int) : TransactionsResponse {
        val from = YearMonth.of(year, month).atDay(1).atStartOfDay().toDate()
        val to = YearMonth.of(year, month).atEndOfMonth().atTime(LocalTime.MAX).toDate()

        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QTransaction().apply {
                joinToCategory()
                joinToAccount()
                where(duplicate().equal(false).and(date().greaterOrEqual(from).and(date().lessOrEqual(to))))
                orderBy(date(), true)
            })
                .list.map { it.forClient() }
                .let(::TransactionsResponse)
        }
    }

    @GetMapping("/api/month/{year}/{month}")
    fun getMonth(@PathVariable year : Int, @PathVariable month : Int) : ResponseEntity<MonthResponse> {
        val from = toEndOfLastMonth(year, month)
        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QEndOfMonthStatement().apply {
                where(date().equal(from))
            })
                .singleResult?.let {
                    ResponseEntity.ok(
                        MonthResponse(
                            id = it.id,
                            date = it.date.time,
                            startingBalance = it.amount)
                    )
                } ?: ResponseEntity.notFound().build()
        }
    }
/*
    @PostMapping("/api/month/{year}/{month}")
    fun postMonth(@PathVariable year : Int, @PathVariable month : Int, @RequestBody monthData : MonthData) {
        DataEntityContext(env).let { ctx ->
            ctx.autocommit = false
            val toDate = {startOfMonth : Long, dayInMonth : Int ->
                GregorianCalendar().apply {
                    timeInMillis = startOfMonth
                    set(Calendar.DAY_OF_MONTH, dayInMonth)
                }.time
            }

            PersistRequest().apply {
                save(ctx.newModel(EMonth::class.java, monthData.id).apply {
                    starting = toStartOfMonth(month, year)
                    startingBalance = monthData.startingBalance
                    finished = monthData.finished
                })
                monthData.transactions.forEach { t ->
                    save(ctx.newModel(ETransaction::class.java, t.id).apply {
                        account = lookupAccount(t.account)
                        date = toDate(monthData.starting, t.dayInMonth)
                        category = lookupCategory(t.category)
                    })
                }
            }.let {
                ctx.persist(it)
                ctx.commit()
            }
    }

 */
}

fun toDate(day : Int, month : Int, year : Int) = LocalDate.of(year, month + 1, day).atStartOfDay().toDate()

fun ETransaction.forClient() = GregorianCalendar().apply { time = date }.let { c ->
        Transaction(
            id = id,
            feed = feed.id,
            account = account.name,
            description = description,
            day = c.get(Calendar.DAY_OF_MONTH),
            month = c.get(Calendar.MONTH),
            year = c.get(Calendar.YEAR),
            category = category.name,
            amount = amount,
            duplicate = duplicate
        )
    }
