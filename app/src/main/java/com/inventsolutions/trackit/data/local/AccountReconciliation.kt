package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "account_reconciliations",
    indices = [Index("accountId"), Index("reconciledAtEpochMillis")]
)
data class AccountReconciliation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val statementBalanceMinor: Long,
    val systemBalanceMinor: Long,
    val adjustmentMinor: Long,
    val note: String = "",
    val reconciledAtEpochMillis: Long = System.currentTimeMillis()
)
