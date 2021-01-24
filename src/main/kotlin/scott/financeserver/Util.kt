package scott.financeserver

import java.time.*
import java.util.*

fun <E,T> Sequence<T>.sequenceOfLists(extract : (T) -> E) : Sequence<List<T>> {
    var currentExtract : E? = null
    var list = mutableListOf<T>()
    val i = iterator()
    return generateSequence {
        while(i.hasNext() && i.next().let { t ->
                list.add(t)
                extract(t) == currentExtract
            });
        if (i.hasNext()) {
            list.subList(0, list.size-1).let {
                it.toList().also { _ ->
                    it.clear()
                    currentExtract = extract(list.first())
                }
            }
        }
        else if (list.isNotEmpty()) list.toList().also { list.clear() } else null
    }
        .filter { it.isNotEmpty() }
}

fun <T> Sequence<T>.batchesOf(num : Int) : Sequence<List<T>> {
    var list = mutableListOf<T>()
    val i = iterator()
    return generateSequence {
        while (i.hasNext() && list.size < num) {
            list.add(i.next())
        }
        list.toList().let { result ->
            if (result.isEmpty()) null
                else result.also { list.clear() }
        }
    }
}

fun toEndOfLastMonth(year : Int, month : Int) = LocalDateTime.of(year, month, 1, 0, 0, 0, 0)
    .minusNanos(1).let {
        Date.from(it.atZone(ZoneId.of("Europe/Vienna")).toInstant())
    }

fun Date.toEndOfLastMonth() = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDateTime()
    .withDayOfMonth(1)
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0)
    .minusNanos(1).let {
        Date.from(it.atZone(ZoneId.of("Europe/Vienna")).toInstant())
    }

fun Date.toEndOfMonth() = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDateTime()
    .withDayOfMonth(1)
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0)
    .plusMonths(1)
    .minusNanos(1).let {
        Date.from(it.atZone(ZoneId.of("Europe/Vienna")).toInstant())
    }

fun Date.endOfDay() : Date  = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDate().atTime(LocalTime.MAX).let {
    Date.from(it.atZone(ZoneId.of("Europe/Vienna")).toInstant())
}
fun LocalDateTime.toDate() = Date.from(atZone(ZoneId.of("Europe/Vienna")).toInstant())

fun Date.toYearMonth() : YearMonth = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDateTime().let { YearMonth.from(it) }

fun Date.toYear() = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.YEAR)
}

fun Date.plusOneMonth() = GregorianCalendar().let {
    it.time = this
    it.add(Calendar.MONTH, 1)
    it.time
}

fun Date.plusOneDay() = GregorianCalendar().let {
    it.time = this
    it.add(Calendar.DAY_OF_YEAR, 1)
    it.time
}

fun Date.toDay()  = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.DAY_OF_MONTH)
}

fun Date.toMonth()  = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.MONTH)
}
fun Date.toWeekOfMonth()  = GregorianCalendar().let {
    it.time = this
    it.get(Calendar.WEEK_OF_MONTH)
}
