package com.inventsolutions.trackit.data.sms

import com.inventsolutions.trackit.data.local.PendingMpesaTransaction
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.security.MessageDigest
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class ParsedBankTransaction(
    val reference: String,
    val source: String,
    val amountMinor: Long,
    val description: String,
    val counterparty: String,
    val dateEpochMillis: Long,
    val isAccountTransfer: Boolean,
    val transferDestinationType: String? = null,
    val linkedMpesaReference: String? = null
)

object BankSmsParser {
    private val imTransfer = Regex(
        """Bank to M-PESA transfer of KES\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+to\s+(\d+)\s+-\s+(.+?)\s+successfully processed\. Transaction Ref ID:\s*([A-Z0-9]+)\.\s*M-PESA Ref ID:\s*([A-Z0-9]+)""",
        RegexOption.IGNORE_CASE
    )
    private val kcbPesaLink = Regex(
        """KES\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+SEND TO PESALINK from\s+\S+\s+to\s+-\s+(.+?)\s+(\d+\*+\d+)\s+on\s+(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+[AP]M)""",
        RegexOption.IGNORE_CASE
    )
    private val kcbMobile = Regex(
        """KES\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s+sent from\s+\S+\s+to\s+(\d+\*+\d+)\s+on\s+(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\s+[AP]M).*?T-KASH REF:\s*([A-Z0-9]+)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(sender: String, message: String, timeZone: TimeZone = TimeZone.getDefault()): ParsedBankTransaction? {
        val normalized = message.replace("\r", " ").replace("\n", " ")
            .replace(Regex("""\s+"""), " ").trim()
        val amountAndMinor: (String) -> Long? = { value ->
            runCatching { BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact() }.getOrNull()
                ?.takeIf { it > 0L }
        }

        if (sender.contains("I&MBANK", ignoreCase = true)) {
            val match = imTransfer.find(normalized) ?: return null
            val amount = amountAndMinor(match.groupValues[1]) ?: return null
            return ParsedBankTransaction(
                reference = match.groupValues[4].uppercase(Locale.ROOT),
                source = PendingMpesaTransaction.SOURCE_IM,
                amountMinor = amount,
                description = "Bank to M-Pesa transfer",
                counterparty = match.groupValues[3].trim(),
                dateEpochMillis = System.currentTimeMillis(),
                isAccountTransfer = true,
                transferDestinationType = "MPESA",
                linkedMpesaReference = match.groupValues[5].uppercase(Locale.ROOT)
            )
        }

        if (!sender.equals("KCB", ignoreCase = true)) return null

        kcbPesaLink.find(normalized)?.let { match ->
            val amount = amountAndMinor(match.groupValues[1]) ?: return null
            val date = parseKcbDate(match.groupValues[4], timeZone) ?: return null
            val name = match.groupValues[2].trim()
            val stableReference = "KCB-" + sha256(normalized).take(32)
            return ParsedBankTransaction(
                stableReference, PendingMpesaTransaction.SOURCE_KCB, amount,
                "PesaLink sent to $name", name, date, false
            )
        }

        kcbMobile.find(normalized)?.let { match ->
            val amount = amountAndMinor(match.groupValues[1]) ?: return null
            val date = parseKcbDate(match.groupValues[3], timeZone) ?: return null
            return ParsedBankTransaction(
                match.groupValues[4].uppercase(Locale.ROOT),
                PendingMpesaTransaction.SOURCE_KCB,
                amount,
                "KCB to T-Kash transfer",
                match.groupValues[2],
                date,
                true,
                "OTHER"
            )
        }
        return null
    }

    private fun parseKcbDate(value: String, timeZone: TimeZone): Long? {
        val input = value.uppercase(Locale.ENGLISH)
        val formatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH).apply {
            isLenient = false
            this.timeZone = timeZone
        }
        val position = ParsePosition(0)
        val parsed = formatter.parse(input, position) ?: return null
        return if (position.index == input.length) parsed.time else null
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
