package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY active DESC, nextRunEpochMillis ASC, id DESC")
    fun observeAll(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND nextRunEpochMillis <= :nowMillis ORDER BY nextRunEpochMillis ASC")
    suspend fun due(nowMillis: Long): List<RecurringTransaction>

    @Insert
    suspend fun insert(item: RecurringTransaction): Long

    @Update
    suspend fun update(item: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
