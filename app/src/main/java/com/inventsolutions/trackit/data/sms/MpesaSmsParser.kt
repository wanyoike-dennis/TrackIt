package com.inventsolutions.trackit.data.sms

import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Parses supported Safaricom M-Pesa confirmation messages.
 *
 * Only the confirmed transaction amount is imported. Transaction fees,
 * balances, daily limits and promotional content are deliberately ignored.
 * The original SMS body is not retained by this parser.
 */
data class ParsedMpesaTransaction(
    val reference: String,
    val type: String,
    val amountMinor: Long,
    val description: String,
    val counterparty: String,
    val dateEpochMillis: Long,
    val currency: String = "KES"
)

object MpesaSmsParser {
    private val header = Regex("""^\\s*([A-Z0-9]{10})\\s+Confirmed\\.?""", RegexOption.IGNORE_CASE)
    private val amount = Regex("""Ksh\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val dateTime = Regex("""(\\d{1,2}/\\d{1,2}/\\d{2})\\s+at\\s+(\\d{1,2}:\\d{2}\\s*[AP]M)""", RegexOption.IGNORE_CASE)
    private val sent = Regex("""sent\\s+to\\s+(.+?)\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2}\\s+at""", RegexOption.IGNORE_CASE)
    private val received = Regex("""received\\s+Ksh\\s*[0-9][0-9,]*(?:\\.[0-9]{1,2})?\\s+from\\s+(.+?)\\s+\\d{4}\\*{3}\\d{3}\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2}\\s+at""", RegexOption.IGNORE_CASE)
    private val paid = Regex("""paid\\s+to\\s+(.+?)\\.?\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2}\\s+at""", RegexOption.IGNORE_CASE)

    fun parse(message: String, timeZone: TimeZone = TimeZone.getDefault()): ParsedMpesaTransaction? {
        val normalized = message.replace("\\r", " ").replace("\\n", " ").replace(Regex("""\\s+"""), " ").trim()
        val reference = header.find(normalized)?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT) ?: return null
        val amountText = amount.find(normalized)?.groupValues?.getOrNull(1)?.replace(",", "") ?: return null
        val amountMinor = try {
            BigDecimal(amountText).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        } catch (_: NumberFormatException) {
            return null
        }
        if (amountMinor <= 0L) return null

        val dateMatch = dateTime.find(normalized) ?: return null
        val date = parseDateTime(dateMatch.groupValues[1], dateMatch.groupValues[2], timeZone) ?: return null

        val sentMatch = sent.find(normalized)
        val receivedMatch = received.find(normalized)
        val paidMatch = paid.find(normalized)

        return when {
            sentMatch != null -> {
                val name = sentMatch.groupValues[1].trim().trimEnd('.')
                if (name.isBlank()) null else ParsedMpesaTransaction(
                    reference, MoneyTransaction.EXPENSE, amountMinor,
                    "Sent to $name", name, date
                )
            }
            receivedMatch != null -> {
                val name = receivedMatch.groupValues[1].trim().trimEnd('.')
                if (name.isBlank()) null else ParsedMpesaTransaction(
                    reference, MoneyTransaction.INCOME, amountMinor,
                    "Received from $name", name, date
                )
            }
            paidMatch != null -> {
                val name = paidMatch.groupValues[1].trim().trimEnd('.')
                if (name.isBlank()) null else ParsedMpesaTransaction(
                    reference, MoneyTransaction.EXPENSE, amountMinor,
                    "Paid to $name", name, date
                )
            }
            else -> null
        }
    }

    private fun parseDateTime(date: String, time: String, timeZone: TimeZone): Long? {
        val formatter = SimpleDateFormat("dd/MM/yy h:mm a", Locale.ENGLISH).apply {
            isLenient = false
            this.timeZone = timeZone
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse("$date $time".uppercase(Locale.ENGLISH), position) ?: return null
        return if (position.index == "$date $time".length) parsed.time else null
    }
}
