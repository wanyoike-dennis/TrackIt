package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_mpesa_transactions",
    indices = [Index(value = ["reference"], unique = true), Index("dateEpochMillis")]
)
data class PendingMpesaTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val type: String,
    val amountMinor: Long,
    val description: String,
    val counterparty: String,
    val dateEpochMillis: Long,
    val receivedAtEpochMillis: Long = System.currentTimeMillis()
)
