package com.inventsolutions.trackit.data.local

import androidx.room.ColumnInfo
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
    val receivedAtEpochMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'MPESA'") val source: String = SOURCE_MPESA,
    @ColumnInfo(defaultValue = "0") val isAccountTransfer: Boolean = false,
    val transferDestinationType: String? = null
) {
    companion object {
        const val SOURCE_MPESA = "MPESA"
        const val SOURCE_KCB = "KCB"
        const val SOURCE_IM = "I&M Bank"
    }
}
