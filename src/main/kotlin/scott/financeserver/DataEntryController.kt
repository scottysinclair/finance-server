package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityConstraint
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QAccount
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QEndOfMonthStatement
import scott.financeserver.data.query.QTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*
import javax.annotation.PostConstruct
import scott.financeserver.data.model.Account as EAccount
import scott.financeserver.data.model.Category as ECategory
import scott.financeserver.data.model.Transaction as ETransaction

data class CategoriesResponse(val categories : List<Category>)
data class Category(val id : UUID, val name : String)

data class TransactionsResponse(val transactions: List<Transaction>)
data class Transaction(val id : UUID, val account : String, val day : Int, val month : Int, val year: Int, val category : String, val comment : String?, val important : Boolean, val amount : BigDecimal)

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


    @GetMapping("/categories")
    fun getCategories() = DataEntityContext(env).use {ctx ->
            ctx.performQuery(QCategory()).list.map {
            Category(
                id = it.id,
                name = it.name
            )
        }
        .let(::CategoriesResponse)
    }

    @PostMapping("/transaction", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun saveTransactions(@RequestBody transaction: Transaction) : Transaction {
        DataEntityContext(env).use { ctx ->
            val acc = ctx.newModel(EAccount::class.java, lookupAccount(transaction.account).id, EntityConstraint.mustExistInDatabase())
            val cat = ctx.newModel(ECategory::class.java, lookupCategory(transaction.category).id, EntityConstraint.mustExistInDatabase())
            return ctx.newModel(ETransaction::class.java, transaction.id, EntityConstraint.dontFetch()).apply {
                account = acc
                date = toDate(transaction.day, transaction.month, transaction.year)
                comment = transaction.comment
                important = transaction.important
                category = cat
                amount = transaction.amount
            }
            .also {
                ctx.persist(PersistRequest().save(it))
                println("SAVED TRANSACTION $transaction")
            }.forClient()
        }
    }


    @GetMapping("/transaction/{year}/{month}")
    fun getTransactions(@PathVariable year : Int, @PathVariable month : Int) : TransactionsResponse {
        val from = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )  ).toInstant())
        val to = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )).plusMonths(1).toInstant())

        return DataEntityContext(env).use { ctx ->
            ctx.performQuery(QTransaction().apply {
                joinToCategory()
                joinToAccount()
                where(date().greaterOrEqual(from).and(date().lessOrEqual(to)))
                orderBy(date(), true)
            })
                .list.map { it.forClient() }
                .let(::TransactionsResponse)
        }
    }

    @GetMapping("/month/{year}/{month}")
    fun getMonth(@PathVariable year : Int, @PathVariable month : Int) : ResponseEntity<MonthResponse> {
        val from = Date.from(LocalDate.of(year, Month.of(month - 1), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )  ).toInstant())

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
    @PostMapping("/month/{year}/{month}")
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
                        comment = t.comment
                        important = t.important
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

fun toDate(day : Int, month : Int, year : Int) : Date {
    return GregorianCalendar().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}

fun ETransaction.forClient() = GregorianCalendar().apply { time = date }.let { c ->
        Transaction(
            id = id,
            account = account.name,
            day = c.get(Calendar.DAY_OF_MONTH),
            month = c.get(Calendar.MONTH),
            year = c.get(Calendar.YEAR),
            category = category.name,
            comment = comment,
            amount = amount,
            important = important
        )
    }


data class MonthData(val id : Long, val starting : Long, val startingBalance : BigDecimal, val transactions: List<Transaction>)
