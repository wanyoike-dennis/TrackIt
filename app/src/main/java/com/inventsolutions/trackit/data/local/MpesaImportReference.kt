package com.inventsolutions.trackit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Permanent M-Pesa reference ledger. A reference remains here after approval or
 * dismissal so a redelivered SMS can never recreate the same review item.
 */
@Entity(tableName = "mpesa_import_references")
data class MpesaImportReference(
    @PrimaryKey val reference: String,
    val recordedAtEpochMillis: Long = System.currentTimeMillis()
)
