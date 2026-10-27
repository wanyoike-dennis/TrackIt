package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class HustleDebtBalance(
    val debtId: Long,
    val originalAmountMinor: Long,
    val settledAmountMinor: Long
) {
    val outstandingAmountMinor: Long get() = (originalAmountMinor - settledAmountMinor).coerceAtLeast(0L)
}

@Dao
interface HustleDao {
    @Query("SELECT * FROM hustles ORDER BY active DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Hustle>>

    @Query("SELECT * FROM hustles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Hustle?

    @Insert
    suspend fun insert(hustle: Hustle): Long

    @Update
    suspend fun update(hustle: Hustle)

    @Query("SELECT * FROM hustle_transactions WHERE (:hustleId IS NULL OR hustleId = :hustleId) AND dateEpochMillis >= :fromMillis AND dateEpochMillis < :untilMillis ORDER BY dateEpochMillis DESC, id DESC")
    fun observeTransactions(hustleId: Long?, fromMillis: Long, untilMillis: Long): Flow<List<HustleTransaction>>

    @Query("SELECT * FROM hustle_transactions WHERE hustleId = :hustleId ORDER BY dateEpochMillis DESC, id DESC")
    fun observeTransactionsForHustle(hustleId: Long): Flow<List<HustleTransaction>>

    @Insert
    suspend fun insertTransaction(transaction: HustleTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: HustleTransaction)

    @Query("DELETE FROM hustle_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("SELECT * FROM hustle_debts WHERE (:hustleId IS NULL OR hustleId = :hustleId) ORDER BY dueDateEpochMillis IS NULL, dueDateEpochMillis ASC, id DESC")
    fun observeDebts(hustleId: Long?): Flow<List<HustleDebt>>

    @Query("SELECT * FROM hustle_debt_settlements WHERE debtId = :debtId ORDER BY dateEpochMillis DESC, id DESC")
    fun observeSettlements(debtId: Long): Flow<List<HustleDebtSettlement>>

    @Query("SELECT d.id AS debtId, d.originalAmountMinor AS originalAmountMinor, COALESCE(SUM(s.amountMinor), 0) AS settledAmountMinor FROM hustle_debts d LEFT JOIN hustle_debt_settlements s ON s.debtId = d.id GROUP BY d.id")
    fun observeDebtBalances(): Flow<List<HustleDebtBalance>>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM hustle_debt_settlements WHERE debtId = :debtId")
    suspend fun settledAmount(debtId: Long): Long

    @Insert
    suspend fun insertDebt(debt: HustleDebt): Long

    @Insert
    suspend fun insertSettlement(settlement: HustleDebtSettlement): Long

    @Query("SELECT * FROM hustle_debts WHERE id = :id LIMIT 1")
    suspend fun getDebt(id: Long): HustleDebt?

    @Transaction
    suspend fun settleDebt(debtId: Long, amountMinor: Long, note: String, dateEpochMillis: Long) {
        require(amountMinor > 0L) { "Settlement amount must be positive." }
        val debt = getDebt(debtId) ?: error("Debt not found.")
        val settled = settledAmount(debtId)
        require(amountMinor <= debt.originalAmountMinor - settled) { "Settlement exceeds the outstanding balance." }
        insertSettlement(HustleDebtSettlement(debtId = debtId, amountMinor = amountMinor, note = note.trim(), dateEpochMillis = dateEpochMillis))
    }
}
