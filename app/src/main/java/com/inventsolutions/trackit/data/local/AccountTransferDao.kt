package com.inventsolutions.trackit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountTransferDao {
    @Query("SELECT * FROM account_transfers ORDER BY dateEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<AccountTransfer>>

    @Insert
    suspend fun insert(transfer: AccountTransfer): Long
}
