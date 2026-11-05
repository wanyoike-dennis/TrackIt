package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountReconciliationDao {
    @Query("SELECT * FROM account_reconciliations ORDER BY reconciledAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<AccountReconciliation>>

    @Insert
    suspend fun insert(item: AccountReconciliation): Long
}
