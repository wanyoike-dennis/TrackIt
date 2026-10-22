package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialRecordDao {
    @Query("SELECT * FROM financial_records ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<FinancialRecord>>

    @Query("SELECT * FROM financial_record_entries ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeAllEntries(): Flow<List<FinancialRecordEntry>>

    @Query("SELECT * FROM financial_record_entries WHERE financialRecordId = :recordId ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeEntriesForRecord(recordId: Long): Flow<List<FinancialRecordEntry>>

    @Insert
    suspend fun insert(record: FinancialRecord): Long

    @Update
    suspend fun update(record: FinancialRecord)

    @Insert
    suspend fun insertEntry(entry: FinancialRecordEntry): Long

    @Transaction
    suspend fun saveProgress(record: FinancialRecord, entry: FinancialRecordEntry) {
        update(record)
        insertEntry(entry)
    }

    @Query("DELETE FROM financial_records WHERE id = :recordId")
    suspend fun deleteById(recordId: Long)
}
