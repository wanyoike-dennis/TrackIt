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
        // M-Pesa sender IDs vary by carrier configuration. Require a recognisable
        // M-Pesa/Safaricom sender label rather than reading unrelated SMS.
        val senderLooksLikeMpesa = sender.contains("MPESA", true) ||
            sender.contains("M-PESA", true) || sender.contains("SAFARICOM", true)
        if (!senderLooksLikeMpesa) return

        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val parsed = MpesaSmsParser.parse(body) ?: return
        val pending = PendingResultHolder(goAsync())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TrackItDatabase.getInstance(context)
                val inserted = db.withTransaction {
                    val referenceInserted = db.mpesaImportReferenceDao().insertIfNew(
                        MpesaImportReference(reference = parsed.reference)
                    )
                    if (referenceInserted == -1L) {
                        false
                    } else {
                        db.pendingMpesaTransactionDao().insertIfNew(
                            PendingMpesaTransaction(
                                reference = parsed.reference,
                                type = parsed.type,
                                amountMinor = parsed.amountMinor,
                                description = parsed.description,
                                counterparty = parsed.counterparty,
                                dateEpochMillis = parsed.dateEpochMillis
                            )
                        )
                        true
                    }
                }
                if (inserted) showReviewNotification(context, parsed.description, parsed.amountMinor)
            } finally {
                pending.finish()
            }
        }
    }

    private fun showReviewNotification(context: Context, description: String, amountMinor: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "M-Pesa transaction reviews", NotificationManager.IMPORTANCE_DEFAULT)
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
            .setContentTitle("M-Pesa transaction to review")
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
        private const val CHANNEL_ID = "mpesa_transaction_review"
        private const val NOTIFICATION_ID = 62041
    }
}
