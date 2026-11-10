package com.inventsolutions.trackit.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.data.local.MoneyTransaction
import androidx.room.withTransaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.data.sms.MpesaSmsReceiver
import com.inventsolutions.trackit.ui.components.TrackItIcon

@Composable
fun MpesaSmsSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = remember { TrackItDatabase.getInstance(context) }
    val pending by database.pendingMpesaTransactionDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(MpesaSmsReceiver.PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(MpesaSmsReceiver.KEY_ENABLED, false)) }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        enabled = granted
        prefs.edit().putBoolean(MpesaSmsReceiver.KEY_ENABLED, granted).apply()
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                TrackItIcon(R.drawable.ic_lucide_arrow_left, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Column {
                Text("M-Pesa SMS import", style = MaterialTheme.typography.headlineSmall)
                Text("Review detected transactions before saving", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your privacy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "When enabled, TrackIt listens only for new incoming messages from a recognised M-Pesa/Safaricom sender and checks whether they match supported transaction confirmations. Processing happens on this device. SMS contents are not uploaded or retained. Unrelated messages are ignored.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "A detected transaction is held in a review queue. It will not affect your income, expenses or balances unless you approve it.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Detect new M-Pesa messages", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (permissionGranted) "SMS permission is granted" else "Android will ask for SMS permission when you enable this",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    if (!checked) {
                        enabled = false
                        prefs.edit().putBoolean(MpesaSmsReceiver.KEY_ENABLED, false).apply()
                    } else if (permissionGranted) {
                        enabled = true
                        prefs.edit().putBoolean(MpesaSmsReceiver.KEY_ENABLED, true).apply()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    }
                }
            )
        }
        if (enabled) {
            Text("Detection is active for new messages only. TrackIt does not scan your existing SMS inbox.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Text("Pending transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (pending.isEmpty()) {
            Text("No transactions waiting for review.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            pending.forEach { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(item.description, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("KES " + "%.2f".format(Locale.ROOT, item.amountMinor / 100.0),
                            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(item.dateEpochMillis)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Reference: " + item.reference, style = MaterialTheme.typography.labelSmall)
                        Text("This will be saved as " + if (item.type == MoneyTransaction.INCOME) "income" else "expense" +
                            " in the Uncategorised category, without adding any M-Pesa fee.",
                            style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    database.withTransaction {
                                        database.transactionDao().insert(
                                            MoneyTransaction(
                                                type = item.type,
                                                amountMinor = item.amountMinor,
                                                category = "Uncategorised",
                                                description = item.description,
                                                paymentMethod = "M-Pesa",
                                                dateEpochMillis = item.dateEpochMillis
                                            )
                                        )
                                        database.pendingMpesaTransactionDao().deleteById(item.id)
                                    }
                                }
                            }) { Text("Approve & save") }
                            OutlinedButton(onClick = {
                                scope.launch { database.pendingMpesaTransactionDao().deleteById(item.id) }
                            }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}
