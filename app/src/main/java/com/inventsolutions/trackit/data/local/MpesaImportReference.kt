package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Permanent SMS import reference ledger shared by M-Pesa and supported banks.
 * References remain after approval or dismissal so a redelivered message cannot
 * recreate the same review item or duplicate a cross-channel transfer.
 */
@Entity(tableName = "mpesa_import_references")
data class MpesaImportReference(
    @PrimaryKey val reference: String,
    val recordedAtEpochMillis: Long = System.currentTimeMillis()
)
