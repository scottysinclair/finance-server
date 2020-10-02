package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QCategory
import scott.financeserver.data.query.QMonth
import scott.financeserver.data.query.QTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*

data class CategoriesResponse(val categories : List<Category>)
data class Category(val id : Long, val name : String)

data class TransactionsResponse(val transactions: List<Transaction>)
data class Transaction(val id : Long, val dayInMonth : Int, val date : Long, val category : String, val comment : String?, val important : Boolean, val amount : BigDecimal)

data class MonthResponse(val id : Long, val starting : Long, val startingBalance : BigDecimal, val finished : Boolean)

@RestController
class Controller {

    @Autowired
    private lateinit var env : Environment

    @GetMapping("/categories")
    fun getCategories() = DataEntityContext(env)
        .performQuery(QCategory()).list.map {
            Category(
                id = it.id,
                name = it.name)
        }
        .let(::CategoriesResponse)


    @GetMapping("/transactions")
    fun getTransactions() = DataEntityContext(env)
            .performQuery(QTransaction().apply { joinToCategory() } )
            .list.map { Transaction(
                id = it.id,
                dayInMonth = GregorianCalendar().let { c -> c.time = it.date; c.get(Calendar.DAY_OF_MONTH) },
                date = it.date.time,
                category = it.category.name,
                comment = it.comment,
                amount = it.amount,
                important = it.important
            ) }.let(::TransactionsResponse)

    @GetMapping("/transactions/{year}/{month}")
    fun getTransactions(@PathVariable year : Int, @PathVariable month : Int) : TransactionsResponse {
        val from = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )  ).toInstant())
        val to = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )).plusMonths(1).toInstant())

        return DataEntityContext(env)
            .performQuery(QTransaction().apply {
                joinToCategory()
                where(date().greaterOrEqual(from).and(date().lessOrEqual(to)))
            })
            .list.map {
                Transaction(
                    id = it.id,
                    dayInMonth = GregorianCalendar().let { c -> c.time = it.date; c.get(Calendar.DAY_OF_MONTH) },
                    date = it.date.time,
                    category = it.category.name,
                    comment = it.comment,
                    amount = it.amount,
                    important = it.important
                )
            }.let(::TransactionsResponse)
    }

    @GetMapping("/month/{year}/{month}")
    fun getMonth(@PathVariable year : Int, @PathVariable month : Int) : MonthResponse {
        val from = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )  ).toInstant())

        return DataEntityContext(env)
            .performQuery(QMonth().apply {
                where(starting().equal(from))
            })
            .singleResult.let {
                MonthResponse(
                    id = it.id,
                    starting = it.starting.time,
                    startingBalance = it.startingBalance,
                    finished = it.finished
                )
            }
    }


}