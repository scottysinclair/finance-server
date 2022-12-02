package scott.financeserver.import

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.math.BigDecimal
import java.nio.charset.Charset
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
fun <R> parseBankAustria(bytes: ByteArray, block : (Sequence<BankAustriaRow>) -> R) : R {
    return runCatching {
        parseBankAustria(bytes, Charsets.UTF_8, { it }).count() //syntax test
        parseBankAustria(bytes, Charsets.UTF_8, block)
    }.getOrElse {
        parseBankAustria(bytes, Charsets.ISO_8859_1, block)
    }
}
fun <R> parseBankAustria(bytes: ByteArray, charset: Charset, block : (Sequence<BankAustriaRow>) -> R) : R {
    var headers : List<String>? = null
    return CSVParser(bytes.inputStream().reader(charset), CSVFormat.newFormat(';'))
        .iterator().asSequence().filter { r ->
            if (headers == null) {
                headers = r.toList().map { it.trim() }.also {
                    if (it.containsAll(listOf(DATE, CURRENCY, BOOKING_TEXT, AMOUNT, REASON, SENDER_BIC)).not())
                        println("Unexpected header row ${r.toList()}")
                }
                false
            }
            else true
        }
        .map { it.toList().mapIndexed{ i, v ->
           headers!![i] to v.trim()
        }.toMap()}
        .mapIndexed { lineNr, it ->
            BankAustriaRow(
                lineNumber = lineNr + 2,
                date = parseDate(it[DATE]!!),
                currency = it[CURRENCY]!!,
                bookingText = it[BOOKING_TEXT]!!,
                amount = it[AMOUNT]!!.let {
                    if (it.lastIndexOf(',') > it.lastIndexOf('.')) it.replace(".", "").replace(',', '.').toBigDecimal()
                    else it.replace(",", "").toBigDecimal()
                },
                reason = it[REASON],
                senderBic = it[SENDER_BIC])
        }
        .let { block(it) }
}

data class BankAustriaRow(
    val lineNumber : Int,
    val date : Date,
    val currency : String,
    val bookingText : String,
    val amount : BigDecimal,
    val reason : String?,
    val senderBic : String?)

val sdf = SimpleDateFormat("dd.MM.yyyy")
@Synchronized
fun parseDate(text : String) = sdf.parse(text)

fun print(map : Map<String,String>, key : String) = print("$key ${map[key]}   ")