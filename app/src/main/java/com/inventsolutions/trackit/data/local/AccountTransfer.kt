package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "account_transfers", indices = [Index("fromAccountId"), Index("toAccountId"), Index("dateEpochMillis")])
data class AccountTransfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amountMinor: Long,
    val note: String = "",
    val dateEpochMillis: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
