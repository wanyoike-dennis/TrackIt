package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<MoneyTransaction>>

    @Insert
    suspend fun insert(transaction: MoneyTransaction): Long

    @Update
    suspend fun update(transaction: MoneyTransaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)

    @Query("UPDATE transactions SET allowanceRecordId = NULL WHERE allowanceRecordId = :recordId")
    suspend fun clearAllowanceLink(recordId: Long)
}
