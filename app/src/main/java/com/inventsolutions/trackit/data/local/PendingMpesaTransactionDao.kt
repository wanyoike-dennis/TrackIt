package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMpesaTransactionDao {
    @Query("SELECT * FROM pending_mpesa_transactions ORDER BY dateEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<PendingMpesaTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(transaction: PendingMpesaTransaction): Long

    @Query("SELECT * FROM pending_mpesa_transactions WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PendingMpesaTransaction?

    @Query("DELETE FROM pending_mpesa_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
