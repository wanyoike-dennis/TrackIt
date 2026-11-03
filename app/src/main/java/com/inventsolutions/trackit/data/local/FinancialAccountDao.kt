package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAccountDao {
    @Query("SELECT * FROM financial_accounts ORDER BY archived ASC, createdAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<FinancialAccount>>

    @Insert
    suspend fun insert(account: FinancialAccount): Long

    @Update
    suspend fun update(account: FinancialAccount)
}
