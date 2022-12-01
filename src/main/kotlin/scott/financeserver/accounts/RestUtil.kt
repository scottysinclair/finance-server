package scott.financeserver.accounts

import org.apache.commons.codec.digest.DigestUtils
import scott.barleydb.api.persist.Operation
import scott.barleydb.api.persist.PersistRequest
import scott.financeserver.import.BankAustriaRow
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

fun List<Operation>.toPersistRequest() = PersistRequest().also { pr ->forEach{ op -> pr.add(op) } }

fun Date.toLocalDate() = toInstant().atZone(ZoneId.of("Europe/Vienna")).toLocalDate()

fun LocalDateTime.toDate() = Date.from(atZone(ZoneId.of("Europe/Vienna")).toInstant())

fun toContent(row : BankAustriaRow) =
   row.run {listOf(date, currency, amount.toString(), bookingText, reason, senderBic) }
      .joinToString(" | ")

fun toHash(row : BankAustriaRow) = DigestUtils.sha1Hex(toContent(row))

fun toHash(data : ByteArray) = DigestUtils.sha1Hex(data)
