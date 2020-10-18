package scott.financeserver.import

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.File
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

const val DATE = "Buchungsdatum"
const val CURRENCY = "Währung"
const val AMOUNT = "Betrag"
const val BOOKING_TEXT = "Buchungstext"
const val REASON = "Zahlungsgrund"
const val SENDER_BIC = "Auftraggeber BIC"


/*
fun main() {
    parseBankAustria(File("data/29163929-Umsatzliste-20201017-1602938806667-AT861200050260051500.csv")) { seq ->
        seq.forEach {
            print(it, DATE)
            print(it, CURRENCY)
            print(it, AMOUNT)
            print(it, BOOKING_TEXT)
            print(it, REASON)
            println()
        }
    }
}
*/
fun <R> parseBankAustria(bytes: ByteArray, block : (Sequence<BankAustriaRow>) -> R)  {
    var headers : List<String>? = null
    CSVParser(bytes.inputStream().reader(Charsets.UTF_8), CSVFormat.newFormat(';'))
        .iterator().asSequence().filter { r ->
            if (headers == null) {
                headers = r.toList().map { it.trim() }
                false
            }
            else true
        }
        .map { it.toList().mapIndexed{ i, v ->
           headers!![i] to v.trim()
        }.toMap()}
        .map {
            BankAustriaRow(
                date = parseDate(it[DATE]!!),
                currency = it[CURRENCY]!!,
                bookingText = it[BOOKING_TEXT]!!,
                amount = it[AMOUNT]!!.toBigDecimal(),
                reason = it[REASON],
                senderBic = it[SENDER_BIC])
        }
        .let { block(it) }
}

data class BankAustriaRow(
    val date : Date,
    val currency : String,
    val bookingText : String,
    val amount : BigDecimal,
    val reason : String?,
    val senderBic : String?)

fun parseDate(text : String) = SimpleDateFormat("").parse(text)

fun print(map : Map<String,String>, key : String) = print("$key ${map[key]}   ")