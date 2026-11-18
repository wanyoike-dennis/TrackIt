package com.inventsolutions.trackit.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class BankSmsParserTest {
    private val kenyaTime = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun parsesImBankToMpesaAndLinksSafaricomReference() {
        val receivedAt = 1_794_000_000_000L
        val result = BankSmsParser.parse(
            "I&MBANK",
            "Bank to M-PESA transfer of KES 100.00 to 254726240133 - David Kirwa successfully processed. " +
                "Transaction Ref ID: 5924OLOR3334. M-PESA Ref ID: UKHKU2LULA.",
            kenyaTime,
            receivedAt
        )
        assertNotNull(result)
        assertEquals("5924OLOR3334", result!!.reference)
        assertEquals("UKHKU2LULA", result.linkedMpesaReference)
        assertEquals(10_000L, result.amountMinor)
        assertTrue(result.isAccountTransfer)
        assertEquals(receivedAt, result.dateEpochMillis)
    }

    @Test
    fun parsesKcbTkashTransferAndIgnoresCost() {
        val result = BankSmsParser.parse(
            "KCB",
            "KES 10.00 sent from 120****929 to 254****519 on 2026-11-07 07:26:49 AM. " +
                "Cost KES 0.00 Incl. Tax. T-KASH REF: 915527470",
            kenyaTime
        )
        assertNotNull(result)
        assertEquals("915527470", result!!.reference)
        assertEquals(1_000L, result.amountMinor)
        assertTrue(result.isAccountTransfer)
        assertEquals("254****519", result.counterparty)
        assertEquals(2026, SimpleDateFormat("yyyy", Locale.ENGLISH).apply { timeZone = kenyaTime }
            .format(java.util.Date(result.dateEpochMillis)).toInt())
    }

    @Test
    fun parsesKcbPesaLinkAsExpenseAndIgnoresCost() {
        val result = BankSmsParser.parse(
            "KCB",
            "KES 19,977.00 SEND TO PESALINK from 120****929 to - DENNIS NJOROGE WANYOIKE " +
                "043****150 on 2026-11-06 01:54:56 PM. Cost KES 20.00 incl. Tax.",
            kenyaTime
        )
        assertNotNull(result)
        assertEquals(1_997_700L, result!!.amountMinor)
        assertFalse(result.isAccountTransfer)
        assertEquals("DENNIS NJOROGE WANYOIKE", result.counterparty)
    }

    @Test
    fun rejectsUnrecognisedBankSendersAndUnsupportedMessages() {
        assertEquals(null, BankSmsParser.parse("OTHER", "KES 100.00 sent"))
        assertEquals(null, BankSmsParser.parse("KCB", "Your balance is KES 100.00"))
    }
}
