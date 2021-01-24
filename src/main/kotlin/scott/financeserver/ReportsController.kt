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
import scott.financeserver.upload.toLocalDate
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
        }).list.flatMap { endOfMonth ->
            var balance = endOfMonth.amount
            listOf(TimePoint(
                date = toDateString(endOfMonth.date.plusOneDay()),
                amount = endOfMonth.amount).also { println("TSS $it") }) +
              ctx.performQuery(QTransaction().apply {
                select(id(), date(), amount())
                where(date().greaterOrEqual(endOfMonth.date.plusOneDay()))
                and(date().less(endOfMonth.date.plusOneDay().plusOneMonth()))
                orderBy(date(), true)
            }).list.also {/*
                  println("---------------")
                  println("${endOfMonth.date.plusOneDay()} <-> ${endOfMonth.date.plusOneDay().plusOneMonth()}")
                  it.forEach{ println("${it.date} ${it.amount}")}
                  println("---------------")*/
            }.asSequence()
                .sequenceOfLists { t -> t.date.toDay()  }
                .map { tsInWeek ->
//                    println("====")
  //                  tsInWeek.forEach { t -> print("${t.date} ")}
    //                println("====")
                    balance += tsInWeek.map{ it.amount }.reduceOrNull{a, b -> a + b} ?: 0.toBigDecimal()
                    TimePoint(date = toDateString(tsInWeek.last().date), amount = balance).also { println("TS $it") }
                }
        }
        .let {
            TimeSeriesReport(listOf(TimeSeries("balance", it)))
        }
    }.also { Runtime.getRuntime().gc() }


    @GetMapping("/timeseries/categories")
    fun getCategoriesTimeseries(@RequestParam description :String?) : TimeSeriesReport = DataEntityContext(env).use { ctx ->
        ctx.streamObjectQuery(QTransaction().apply {
            select(id(), categoryId(), amount())
            joinToCategory()
            where(duplicate().equal(false))
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
            where(duplicate().equal(false))
            and(date().greaterOrEqual(startOfYear(year)))
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



fun Sequence<ETransaction>.sequenceOfMonthlyTransactions() = sequenceOfLists { t -> t.date.toMonth()  }
    .map { it.also { println("Month ${it.get(0).date.toMonth()} with ${it.size} transactions ")} }


fun Sequence<List<ETransaction>>.sequenceOfSummedCategories() : Sequence<Pair<Date, Map<String,BigDecimal>>> {
    return map {
        //TODO: start of next month????
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


fun <T> Iterator<T>.next(predicate : (T) -> Boolean) : T? {
    while (hasNext()) {
        val n = next()
        if (predicate(n)) return n
    }
    return null
}


private fun toDateString(date: Date) = SimpleDateFormat("yyyy-MM-dd").format(date)


