package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialRecordDao {
    @Query("SELECT * FROM financial_records ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<FinancialRecord>>

    @Insert
    suspend fun insert(record: FinancialRecord): Long

    @Update
    suspend fun update(record: FinancialRecord)

    @Query("DELETE FROM financial_records WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)
}
