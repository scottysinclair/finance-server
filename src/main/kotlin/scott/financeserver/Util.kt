package scott.financeserver

import java.time.*
import java.util.*

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