package com.inventsolutions.trackit.data.sms

/**
 * Arithmetic for the revolving Fuliza facility.
 *
 * The assigned limit is a credit ceiling, not cash. Borrowing increases the
 * liability by the amount borrowed plus its access fee; repayments reduce it.
 */
object FulizaDebtCalculator {
    fun afterBorrowing(
        currentDebtMinor: Long,
        borrowedAmountMinor: Long,
        accessFeeMinor: Long
    ): Long = currentDebtMinor + borrowedAmountMinor + accessFeeMinor

    fun afterRepayment(currentDebtMinor: Long, repaymentMinor: Long): Long =
        (currentDebtMinor - repaymentMinor).coerceAtLeast(0L)

    fun unusedLimit(limitMinor: Long, currentDebtMinor: Long): Long =
        (limitMinor - currentDebtMinor).coerceAtLeast(0L)
}
