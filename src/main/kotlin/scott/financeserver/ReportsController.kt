package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.stream.ObjectInputStream
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QMonth
import scott.financeserver.data.query.QTransaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

import scott.financeserver.data.model.Transaction as ETransaction

data class TimeSeriesReport(val data : List<TimeSeries>)

data class TimeSeries(val id : String, val data : List<TimePoint>)

data class TimePoint(val date: String, val amount : BigDecimal)

@RestController
class ReportsController  {

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/timeseries/balance")
    fun getBalanceTimeseries() = DataEntityContext(env).let { ctx ->
        ctx.performQuery(QMonth().apply {
            orderBy(starting(), true)
        }).list.map { month ->
            TimePoint(
                date = toDateString(month.starting),
                amount = month.startingBalance)
        }.let {
            TimeSeriesReport(listOf(TimeSeries("balance", it)))
        }
    }


    @GetMapping("/timeseries/categories")
    fun getCategoriesTimeseries(@RequestParam comment :String?) : TimeSeriesReport = DataEntityContext(env).let { ctx ->
        ctx.streamObjectQuery(QTransaction().apply {
            joinToCategory()
            if (comment != null) where(comment().like("%${comment.toUpperCase()}%").or(comment().like("%${comment.toLowerCase()}%")))
            orderBy(date(), true)
        }).toSequence()
            .sequenceOfMonthlyTransactions()
            .sequenceOfSummedCategories()
            .toList().let { summedCats ->
                summedCats.getCategories().map { c ->
                    summedCats.extractTimeSeriesFor(c)
                }
            }.let {
                TimeSeriesReport(it)
            }
        }
}


fun <T> ObjectInputStream<T>.toSequence() : Sequence<T> = generateSequence {
        read().also {
            if (it == null) close().also { println("CLOSING") }
        }
    }

fun Sequence<ETransaction>.sequenceOfMonthlyTransactions() : Sequence<List<ETransaction>> {
    var currentMonth = -1
    var list = mutableListOf<ETransaction>()
    val i = iterator()
    return generateSequence {
        while(i.hasNext() && i.next().let { t ->
                list.add(t)
                t.date.toMonth() == currentMonth
            });
            if (i.hasNext()) {
                list.subList(0, list.size-1).let {
                    it.toList().also { _ ->
                        it.clear()
                        currentMonth = list.first().date.toMonth()
                    }
                }
            }
            else if (list.isNotEmpty()) list.toList().also { list.clear() } else null
        }
        .filter { it.isNotEmpty() }
        .map { it.also { println("Month with ${it.size} transactions ")} }
}

fun Sequence<List<ETransaction>>.sequenceOfSummedCategories() : Sequence<Pair<Date, Map<String,BigDecimal>>> {
    return map {
        it.first().date.plusOneMonth() to
        it.groupBy { t -> t.category.name }
            .mapValues { entry ->  entry.value.map(ETransaction::getAmount).reduce{a,b -> a+b}}
    }.map {
        it.also { println("Month ${it.first} with ${it.second.size} categories") }
    }
}

fun List<Pair<Date, Map<String, BigDecimal>>>.getCategories(): SortedSet<String> {
    return map { it.second.keys }.toSet().flatten().toSortedSet()
}

fun List<Pair<Date, Map<String, BigDecimal>>>.extractTimeSeriesFor(category: String) : TimeSeries {
    return TimeSeries(
        id = category,
        data = map { TimePoint(
           date = toDateString(it.first),
           amount = it.second[category] ?: 0.toBigDecimal()
        )}
    )
}

fun Date.plusOneMonth() = GregorianCalendar().let {
    it.time = this
    it.add(Calendar.MONTH, 1)
    it.time
}

fun Date.toMonth()  = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.MONTH)
}


private fun toDateString(date: Date) = SimpleDateFormat("YYYY-MM-dd").format(date)


