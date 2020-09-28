package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*

data class TransactionResponse(val transactions: List<Transaction>)
data class Transaction(val id : Long, val date : Date, val category : String, val comment : String?, val important : Boolean, val amount : BigDecimal)

@RestController
class Controller {

    @Autowired
    private lateinit var env : Environment

    @GetMapping("/transactions")
    fun getTransactions() = DataEntityContext(env)
            .performQuery(QTransaction().apply { joinToCategory() } )
            .list.map { Transaction(
                id = it.id,
                date = it.date,
                category = it.category.name,
                comment = it.comment,
                amount = it.amount,
                important = it.important
            ) }.let(::TransactionResponse)

    @GetMapping("/transactions/{year}/{month}")
    fun getTransactions(@PathVariable year : Int, @PathVariable month : Int) : TransactionResponse {
        val from = Date.from(LocalDate.of(year, Month.of(month), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )  ).toInstant())
        val to = Date.from(LocalDate.of(year, Month.of(month + 1), 1).atStartOfDay(ZoneId.of( "Europe/Vienna" )).toInstant())

        return DataEntityContext(env)
            .performQuery(QTransaction().apply {
                joinToCategory()
                where(date().greaterOrEqual(from).and(date().lessOrEqual(to)))
            })
            .list.map {
                Transaction(
                    id = it.id,
                    date = it.date,
                    category = it.category.name,
                    comment = it.comment,
                    amount = it.amount,
                    important = it.important
                )
            }.let(::TransactionResponse)
    }
}