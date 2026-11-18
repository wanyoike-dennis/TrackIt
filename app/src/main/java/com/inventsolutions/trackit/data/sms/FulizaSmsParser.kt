package com.inventsolutions.trackit.data.sms

import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ParsedFulizaEvent(
    val reference: String,
    val kind: String,
    val amountMinor: Long,
    val accessFeeMinor: Long = 0L,
    val outstandingMinor: Long? = null,
    val dueDateEpochMillis: Long? = null,
    val dueDateLabel: String? = null,
    val receivedAtEpochMillis: Long
)

object FulizaSmsParser {
    const val BORROWING = "BORROWING"
    const val REPAYMENT = "REPAYMENT"

    private val reference = Regex("""^\s*([A-Z0-9]{10})\s+Confirmed\.?""", RegexOption.IGNORE_CASE)
    private val borrowingAmount = Regex("""Fuliza\s+M-PESA\s+amount\s+is\s+Ksh\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val accessFee = Regex("""Access\s+Fee\s+charged\s+Ksh\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val outstanding = Regex("""Total\s+Fuliza\s+M-PESA\s+outstanding\s+amount\s+is\s+Ksh\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
    private val dueDate = Regex("""\bdue\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})""", RegexOption.IGNORE_CASE)
    private val repayment = Regex("""Ksh\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+from\s+your\s+M-PESA\s+has\s+been\s+used\s+to\s+(?:partially\s+)?pay\s+your\s+outstanding\s+Fuliza\s+M-PESA""", RegexOption.IGNORE_CASE)

    fun parse(
        message: String,
        receivedAtEpochMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ParsedFulizaEvent? {
        val normalized = message.replace("\r", " ").replace("\n", " ")
            .replace(Regex("""\s+"""), " ").trim()
        val ref = reference.find(normalized)?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT) ?: return null

        val repaymentMatch = repayment.find(normalized)
        if (repaymentMatch != null) {
            val amount = toMinor(repaymentMatch.groupValues[1]) ?: return null
            if (amount <= 0L) return null
            return ParsedFulizaEvent(
                reference = ref,
                kind = REPAYMENT,
                amountMinor = amount,
                receivedAtEpochMillis = receivedAtEpochMillis
            )
        }

        val borrowingMatch = borrowingAmount.find(normalized) ?: return null
        val amount = toMinor(borrowingMatch.groupValues[1]) ?: return null
        val fee = accessFee.find(normalized)?.groupValues?.getOrNull(1)?.let(::toMinor) ?: 0L
        val outstandingAmount = outstanding.find(normalized)?.groupValues?.getOrNull(1)?.let(::toMinor)
        if (amount <= 0L || fee < 0L) return null
        val dueLabel = dueDate.find(normalized)?.groupValues?.getOrNull(1)
        val dueEpoch = dueLabel?.let { parseDueDate(it, timeZone) }

        return ParsedFulizaEvent(
            reference = ref,
            kind = BORROWING,
            amountMinor = amount,
            accessFeeMinor = fee,
            outstandingMinor = outstandingAmount,
            dueDateEpochMillis = dueEpoch,
            dueDateLabel = dueLabel,
            receivedAtEpochMillis = receivedAtEpochMillis
        )
    }

    private fun toMinor(value: String): Long? = try {
        BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    } catch (_: NumberFormatException) {
        null
    }

    private fun parseDueDate(value: String, timeZone: TimeZone): Long? {
        val pattern = if (value.substringAfterLast("/").length == 4) "d/M/yyyy" else "d/M/yy"
        val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
            isLenient = false
            this.timeZone = timeZone
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(value, position) ?: return null
        return if (position.index == value.length) parsed.time else null
    }
}
