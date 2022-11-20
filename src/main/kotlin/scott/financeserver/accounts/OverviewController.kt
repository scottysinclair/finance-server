package scott.financeserver.accounts

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.query.QProperty
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QEndOfMonthStatement
import scott.financeserver.data.query.QTransaction
import scott.financeserver.toSequence

data class AcountOverview(val fromDate : Long, val toDate : Long, val numberOfTransactions : Int, val numberOfStatements : Int)

@RestController
class OverviewController {

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/api/account/{accountName}/overview")
    fun getOverview(@PathVariable accountName : String) = DataEntityContext(env).use { ctx ->
        AcountOverview(
            fromDate = 0L,
            toDate = 0L,
            numberOfTransactions = 0,
            numberOfStatements = 0
        ).let { overview ->
            ctx.streamObjectQuery(QTransaction().apply {
                select(id(), date())
                whereExists(existsAccount().apply {
                    where(name() equals accountName)
                })
                orderBy(date(), true)
            }).toSequence()
                .fold(overview) { r, t ->
                    when (r.fromDate) {
                        0L -> r.copy(
                            fromDate = t.date.time,
                            numberOfTransactions = 1
                        )
                        else -> r.copy(
                            numberOfTransactions = r.numberOfTransactions + 1,
                            toDate = t.date.time
                        )
                    }
                }
        }.let { overview ->
            ctx.streamObjectQuery(QEndOfMonthStatement().apply {
                select(id())
                whereExists(existsAccount().apply {
                    where(name() equals accountName)
                })
                orderBy(date(), true)
            }).toSequence()
                .fold(overview) { r, _ ->
                    r.copy(numberOfStatements = r.numberOfStatements + 1)
                }
        }
    }.also { Runtime.getRuntime().gc() }
}

infix fun <VAL> QProperty<VAL>.equals(value : VAL) = this.equal(value)