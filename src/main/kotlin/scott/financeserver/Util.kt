package scott.financeserver

import scott.financeserver.data.model.Transaction
import java.time.*
import java.util.*

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