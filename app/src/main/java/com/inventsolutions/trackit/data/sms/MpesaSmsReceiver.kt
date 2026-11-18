package com.inventsolutions.trackit.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.inventsolutions.trackit.MainActivity
import com.inventsolutions.trackit.data.local.PendingMpesaTransaction
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.MpesaImportReference
import androidx.room.withTransaction
import com.inventsolutions.trackit.data.local.TrackItDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MpesaSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_ENABLED, false)) return

        val messages = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent).toList() }.getOrNull()
            ?: return
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }

        val bank = when {
            sender.contains("I&MBANK", true) -> BankSmsParser.parse(sender, body, receivedAtEpochMillis = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis())
            sender.equals("KCB", true) -> BankSmsParser.parse(sender, body)
            else -> null
        }
        val isSafaricomSender = sender.contains("MPESA", true) ||
            sender.contains("M-PESA", true) || sender.contains("SAFARICOM", true)
        val receivedAt = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val fuliza = if (bank == null && isSafaricomSender) {
            FulizaSmsParser.parse(body, receivedAt)
        } else null
        val mpesa = if (bank == null && fuliza == null && isSafaricomSender) {
            MpesaSmsParser.parse(body)
        } else null

        if (bank == null && mpesa == null && fuliza == null) return
        val pending = PendingResultHolder(goAsync())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TrackItDatabase.getInstance(context)
                val inserted = db.withTransaction {
                    val primaryReference = bank?.reference ?: fuliza?.reference ?: mpesa!!.reference
                    val referenceInserted = db.mpesaImportReferenceDao().insertIfNew(
                        MpesaImportReference(reference = primaryReference)
                    )
                    if (referenceInserted == -1L) return@withTransaction false

                    if (fuliza != null) {
                        val debtDao = db.financialRecordDao()
                        var facility = debtDao.getByTypeAndTitle(
                            FinancialRecord.FULIZA_FACILITY,
                            "Fuliza M-PESA"
                        )

                        // Convert records created by the earlier Fuliza implementation without
                        // losing their already-calculated outstanding balance or activity history.
                        if (facility == null) {
                            val legacyDebt = debtDao.getByTypeAndTitle(
                                FinancialRecord.DEBT_I_OWE,
                                "Fuliza M-PESA"
                            )
                            if (legacyDebt != null) {
                                facility = legacyDebt.copy(
                                    type = FinancialRecord.FULIZA_FACILITY,
                                    targetMinor = 0L,
                                    currentMinor = (legacyDebt.targetMinor - legacyDebt.currentMinor).coerceAtLeast(0L),
                                    notes = "Fuliza credit facility. Set your credit limit manually; the limit is not cash."
                                )
                                debtDao.update(facility)
                            }
                        }

                        when (fuliza.kind) {
                            FulizaSmsParser.BORROWING -> {
                                val borrowedWithFee = fuliza.amountMinor + fuliza.accessFeeMinor
                                val debtId = if (facility == null) {
                                    debtDao.insert(
                                        FinancialRecord(
                                            type = FinancialRecord.FULIZA_FACILITY,
                                            title = "Fuliza M-PESA",
                                            targetMinor = 0L,
                                            currentMinor = borrowedWithFee,
                                            dueDateEpochMillis = fuliza.dueDateEpochMillis,
                                            notes = "Set your Fuliza credit limit manually. The limit is a credit facility, not available cash.",
                                            createdAtEpochMillis = fuliza.receivedAtEpochMillis
                                        )
                                    )
                                } else {
                                    debtDao.update(
                                        facility.copy(
                                            currentMinor = facility.currentMinor + borrowedWithFee,
                                            dueDateEpochMillis = fuliza.dueDateEpochMillis ?: facility.dueDateEpochMillis
                                        )
                                    )
                                    facility.id
                                }
                                val note = buildString {
                                    append("Fuliza borrowing KES ")
                                    append("%.2f".format(java.util.Locale.ROOT, fuliza.amountMinor / 100.0))
                                    append("; access fee KES ")
                                    append("%.2f".format(java.util.Locale.ROOT, fuliza.accessFeeMinor / 100.0))
                                    append("; debt increased by KES ")
                                    append("%.2f".format(java.util.Locale.ROOT, borrowedWithFee / 100.0))
                                    fuliza.dueDateLabel?.let { append("; due on ").append(it) }
                                    append(". Reference: ").append(fuliza.reference)
                                }
                                debtDao.insertEntry(
                                    FinancialRecordEntry(
                                        financialRecordId = debtId,
                                        entryType = FinancialRecordEntry.BORROWING,
                                        amountMinor = borrowedWithFee,
                                        note = note,
                                        createdAtEpochMillis = fuliza.receivedAtEpochMillis
                                    )
                                )
                            }
                            FulizaSmsParser.REPAYMENT -> {
                                val debtId = if (facility == null) {
                                    debtDao.insert(
                                        FinancialRecord(
                                            type = FinancialRecord.FULIZA_FACILITY,
                                            title = "Fuliza M-PESA",
                                            targetMinor = 0L,
                                            currentMinor = 0L,
                                            notes = "Set your Fuliza credit limit manually. The limit is a credit facility, not available cash.",
                                            createdAtEpochMillis = fuliza.receivedAtEpochMillis
                                        )
                                    )
                                } else {
                                    debtDao.update(
                                        facility.copy(
                                            currentMinor = (facility.currentMinor - fuliza.amountMinor).coerceAtLeast(0L)
                                        )
                                    )
                                    facility.id
                                }
                                debtDao.insertEntry(
                                    FinancialRecordEntry(
                                        financialRecordId = debtId,
                                        entryType = FinancialRecordEntry.REPAYMENT,
                                        amountMinor = fuliza.amountMinor,
                                        note = "Fuliza repayment reduced outstanding debt. Reference: ${fuliza.reference}",
                                        createdAtEpochMillis = fuliza.receivedAtEpochMillis
                                    )
                                )
                            }
                        }
                        return@withTransaction true
                    }

                    val linkedReference = bank?.linkedMpesaReference
                    if (linkedReference != null) {
                        val aliasInserted = db.mpesaImportReferenceDao().insertIfNew(
                            MpesaImportReference(reference = linkedReference)
                        )
                        if (aliasInserted == -1L) {
                            // If the Safaricom alert arrived first but is still awaiting review,
                            // replace it with the bank alert so the movement is classified once.
                            val existingPending = db.pendingMpesaTransactionDao().findByReference(linkedReference)
                                ?: return@withTransaction false
                            db.pendingMpesaTransactionDao().deleteById(existingPending.id)
                        }
                    }

                    val item = if (bank != null) {
                        PendingMpesaTransaction(
                            reference = bank.reference,
                            type = if (bank.isAccountTransfer) "TRANSFER" else com.inventsolutions.trackit.data.local.MoneyTransaction.EXPENSE,
                            amountMinor = bank.amountMinor,
                            description = bank.description,
                            counterparty = bank.counterparty,
                            dateEpochMillis = bank.dateEpochMillis,
                            source = bank.source,
                            isAccountTransfer = bank.isAccountTransfer,
                            transferDestinationType = bank.transferDestinationType
                        )
                    } else {
                        PendingMpesaTransaction(
                            reference = mpesa!!.reference,
                            type = mpesa.type,
                            amountMinor = mpesa.amountMinor,
                            description = mpesa.description,
                            counterparty = mpesa.counterparty,
                            dateEpochMillis = mpesa.dateEpochMillis,
                            source = PendingMpesaTransaction.SOURCE_MPESA
                        )
                    }
                    db.pendingMpesaTransactionDao().insertIfNew(item) != -1L
                }
                if (inserted && fuliza == null) {
                    val description = bank?.description ?: mpesa!!.description
                    val amount = bank?.amountMinor ?: mpesa!!.amountMinor
                    showReviewNotification(context, description, amount)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun showReviewNotification(context: Context, description: String, amountMinor: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "SMS transaction reviews", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = android.app.PendingIntent.getActivity(
            context, NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val amount = "KES " + "%.2f".format(java.util.Locale.ROOT, amountMinor / 100.0)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transaction to review")
            .setContentText("$description • $amount")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }

    private class PendingResultHolder(private val result: PendingResult) {
        fun finish() = result.finish()
    }

    companion object {
        const val PREFS = "trackit_mpesa_sms"
        const val KEY_ENABLED = "enabled"
        private const val CHANNEL_ID = "sms_transaction_review"
        private const val NOTIFICATION_ID = 62041
    }
}
