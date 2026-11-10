package com.inventsolutions.trackit.data.sms

import com.inventsolutions.trackit.data.local.MoneyTransaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class MpesaSmsParserTest {
    private val kenyaTime = TimeZone.getTimeZone("Africa/Nairobi")

    @Test
    fun parsesMoneySentAndIgnoresTransactionCostAndBalance() {
        val result = MpesaSmsParser.parse(
            "UKAMF0YJDM Confirmed. Ksh120.00 sent to Peter  Kirumbu on 10/11/26 at 6:52 AM. " +
                "New M-PESA balance is Ksh0.00. Transaction cost, Ksh5.00. " +
                "Amount you can transact within the day is 499,880.00.",
            kenyaTime
        )

        assertNotNull(result)
        assertEquals("UKAMF0YJDM", result!!.reference)
        assertEquals(MoneyTransaction.EXPENSE, result.type)
        assertEquals(12_000L, result.amountMinor)
        assertEquals("Sent to Peter Kirumbu", result.description)
    }

    @Test
    fun parsesMoneyReceived() {
        val result = MpesaSmsParser.parse(
            "UK9A81I2UH Confirmed.You have received Ksh30.00 from SALOME  NUNGARI 0726***006 " +
                "on 9/11/26 at 11:30 AM New M-PESA balance is Ksh30.00.",
            kenyaTime
        )

        assertNotNull(result)
        assertEquals(MoneyTransaction.INCOME, result!!.type)
        assertEquals(3_000L, result.amountMinor)
        assertEquals("Received from SALOME NUNGARI", result.description)
    }

    @Test
    fun parsesMerchantPayment() {
        val result = MpesaSmsParser.parse(
            "UKAMF0YWDR Confirmed. Ksh20.00 paid to KIDERO SHOP. on 10/11/26 at 7:58 AM." +
                "New M-PESA balance is Ksh0.00. Transaction cost, Ksh0.00.",
            kenyaTime
        )

        assertNotNull(result)
        assertEquals(MoneyTransaction.EXPENSE, result!!.type)
        assertEquals(2_000L, result.amountMinor)
        assertEquals("Paid to KIDERO SHOP", result.description)
    }

    @Test
    fun rejectsMessagesWithoutSupportedConfirmationFormat() {
        assertNull(MpesaSmsParser.parse("Your M-PESA balance is Ksh500.00", kenyaTime))
    }
}
