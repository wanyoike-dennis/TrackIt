package com.inventsolutions.trackit.data.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class FulizaDebtCalculatorTest {
    @Test
    fun borrowingAddsPrincipalAndAccessFeeButNotTheSmsCumulativeOutstanding() {
        val debt = FulizaDebtCalculator.afterBorrowing(
            currentDebtMinor = 0L,
            borrowedAmountMinor = 10_000L,
            accessFeeMinor = 100L
        )

        assertEquals(10_100L, debt)
        assertEquals(189_900L, FulizaDebtCalculator.unusedLimit(200_000L, debt))
    }

    @Test
    fun repaymentReducesOutstandingDebt() {
        val debt = FulizaDebtCalculator.afterRepayment(
            currentDebtMinor = 139_786L,
            repaymentMinor = 108_000L
        )

        assertEquals(31_786L, debt)
        assertEquals(0L, FulizaDebtCalculator.afterRepayment(5_000L, 6_000L))
    }

    @Test
    fun unusedLimitNeverBecomesNegativeOrCash() {
        assertEquals(0L, FulizaDebtCalculator.unusedLimit(20_000L, 25_000L))
    }
}
