package com.inventsolutions.trackit.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class FulizaSmsParserTest {
    private val kenyaTime = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun parsesFulizaBorrowingFeeOutstandingAndDueDate() {
        val result = FulizaSmsParser.parse(
            "UKIMF1Z67V Confirmed. Fuliza M-PESA amount is Ksh 100.00. " +
                "Access Fee charged Ksh 1.00. Total Fuliza M-PESA outstanding amount is Ksh1397.86 due on 09/12/26.",
            receivedAtEpochMillis = 1_794_000_000_000L,
            timeZone = kenyaTime
        )

        assertNotNull(result)
        assertEquals("UKIMF1Z67V", result!!.reference)
        assertEquals(FulizaSmsParser.BORROWING, result.kind)
        assertEquals(10_000L, result.amountMinor)
        assertEquals(100L, result.accessFeeMinor)
        assertEquals(139_786L, result.outstandingMinor)
        assertNotNull(result.dueDateEpochMillis)
        assertEquals("09/12/26", result.dueDateLabel)
    }

    @Test
    fun parsesFulizaRepaymentWithoutTreatingAvailableLimitAsIncome() {
        val result = FulizaSmsParser.parse(
            "UKHMF1X1OD Confirmed. Ksh 1080.00 from your M-PESA has been used to partially pay " +
                "your outstanding Fuliza M-PESA. Your available Fuliza M-PESA limit is Ksh 1115.16. " +
                "Your M-PESA balance is 0.00.",
            receivedAtEpochMillis = 1_794_000_000_000L,
            timeZone = kenyaTime
        )

        assertNotNull(result)
        assertEquals("UKHMF1X1OD", result!!.reference)
        assertEquals(FulizaSmsParser.REPAYMENT, result.kind)
        assertEquals(108_000L, result.amountMinor)
        assertEquals(0L, result.accessFeeMinor)
        assertNull(result.outstandingMinor)
    }

    @Test
    fun rejectsUnrelatedMessagesAndBalanceOnlyAlerts() {
        assertNull(FulizaSmsParser.parse("Your M-PESA balance is Ksh500.00"))
        assertNull(FulizaSmsParser.parse("UKIMF1Z67V Confirmed. Your available Fuliza limit is Ksh 100.00."))
    }
}
