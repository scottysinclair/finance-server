package scott.financeserver

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import scott.barleydb.api.core.Environment
import scott.barleydb.api.core.entity.EntityContext
import scott.barleydb.api.stream.ObjectInputStream
import scott.financeserver.data.DataEntityContext
import scott.financeserver.data.query.QEndOfMonthStatement
import scott.financeserver.data.query.QTransaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

import scott.financeserver.data.model.EndOfMonthStatement as EEndOfMonthStatement
import scott.financeserver.data.model.Transaction as ETransaction



data class TimeSeriesReport(val data : List<TimeSeries>)

data class TimeSeries(val id : String, val data : List<TimePoint>)

data class TimePoint(val date: String, val amount : BigDecimal)

data class Years(val years: List<Int>)

data class CategoriesForYear(val data : List<CategoryTotal>)
data class CategoryTotal(val name : String, val total : BigDecimal)

@RestController
class ReportsController  {

    @Autowired
    private lateinit var env: Environment

    @GetMapping("/years")
    fun getYears()  = DataEntityContext(env).use { ctx ->
        ctx.streamObjectQuery(QEndOfMonthStatement().apply {
            orderBy(date(), true)
        }).toSequence()
            .toSequenceOfYears()
            .toList()
            .let {
                Years(it)
            }
    }.also { Runtime.getRuntime().gc() }

    @GetMapping("/timeseries/balance")
    fun getBalanceTimeseries() = DataEntityContext(env).use { ctx ->
        ctx.performQuery(QEndOfMonthStatement().apply {
            orderBy(date(), true)
        }).list.map { endOfMonth ->
            TimePoint(
                date = toDateString(endOfMonth.date.plusOneDay()),
                amount = endOfMonth.amount)
        }.let {
            TimeSeriesReport(listOf(TimeSeries("balance", it)))
        }
    }.also { Runtime.getRuntime().gc() }


    @GetMapping("/timeseries/categories")
    fun getCategoriesTimeseries(@RequestParam description :String?, @RequestParam comment :String?) : TimeSeriesReport = DataEntityContext(env).use { ctx ->
        ctx.streamObjectQuery(QTransaction().apply {
            select(id(), categoryId(), amount())
            joinToCategory()
            if (comment != null) where(comment().like("%${comment.toUpperCase()}%").or(comment().like("%${comment.toLowerCase()}%")))
            if (description != null) and(description().like("%${description.toUpperCase()}%").or(description().like("%${description.toLowerCase()}%")))
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
        }.also { Runtime.getRuntime().gc() }

    @GetMapping("/year/{year}/categories")
    fun getCategoryTotalsForYear(@PathVariable year :String) = DataEntityContext(env).use { ctx ->
        ctx.streamObjectQuery(QTransaction().apply {
            joinToCategory()
            where(date().greaterOrEqual(startOfYear(year)))
            and(date().less(startOfYear(year + 1)))
            orderBy(date(), true)
        }).toSequence()
            .sequenceOfMonthlyTransactions()
            .sequenceOfSummedCategories()
            .map { it.second }
            .reduce {monthA, monthB -> merge(monthA, monthB) { va, vb -> va + vb } }
            }.let {
                CategoriesForYear(it.map { e ->
                    CategoryTotal(name = e.key, total = e.value)
                }.sortedBy { s -> s.name })
            }.also { Runtime.getRuntime().gc() }

    private fun startOfYear(year: String): Date {
        return GregorianCalendar().let {
            it.set(Calendar.YEAR, year.toInt())
            it.set(Calendar.DAY_OF_YEAR, 1)
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
            it.time
        }
    }
}


/**
 * Merge maps reducing conflicting values
 */
fun <K,V> merge(a : Map<K,V>, b : Map<K,V>, valueReducer : (V, V) -> V) = (a.asSequence() + b.asSequence())
    .groupBy({ it.key }, { it.value })
    .mapValues { (_, values) -> values.reduce(valueReducer)  }


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

fun Sequence<EEndOfMonthStatement>.toSequenceOfYears() : Sequence<Int> {
    return map { it.date.toYear() }
        .distinct()
}

/*
fun <T> Sequence<T>.deduplicate() : Sequence<T> {
    return iterator().let { i ->
            var lastEmitted : T? = null
            generateSequence {
                i.next{ v -> v != lastEmitted }.also {
                    lastEmitted = it
                }
            }
        }
}*/

fun Date.toYear() = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.YEAR)
}

fun <T> Iterator<T>.next(predicate : (T) -> Boolean) : T? {
    while (hasNext()) {
        val n = next()
        if (predicate(n)) return n
    }
    return null
}

fun Date.plusOneMonth() = GregorianCalendar().let {
    it.time = this
    it.add(Calendar.MONTH, 1)
    it.time
}

fun Date.plusOneDay() = GregorianCalendar().let {
    it.time = this
    it.add(Calendar.DAY_OF_MONTH, 1)
    it.time
}

fun Date.toMonth()  = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.MONTH)
}


private fun toDateString(date: Date) = SimpleDateFormat("YYYY-MM-dd").format(date)


