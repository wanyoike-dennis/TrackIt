package com.inventsolutions.trackit.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.inventsolutions.trackit.data.local.ExpenseCategory
import com.inventsolutions.trackit.data.local.FinancialAccount
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.MonthlyBudget
import com.inventsolutions.trackit.data.local.TrackItDatabase
import com.inventsolutions.trackit.data.sms.MpesaSmsReceiver
import com.inventsolutions.trackit.ui.screens.transactions.TransactionEntryScreen
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.math.BigDecimal
import com.inventsolutions.trackit.data.local.PendingMpesaTransaction

@Composable
fun MpesaSmsSettingsScreen(
    budgets: List<MonthlyBudget>,
    transactions: List<MoneyTransaction>,
    expenseCategories: List<ExpenseCategory>,
    allowances: List<FinancialRecord>,
    accounts: List<FinancialAccount>,
    isSaving: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onCreateCategory: (name: String, budgetLimitMinor: Long?, budgetType: String) -> Unit,
    onApprovePending: suspend (
        pendingId: Long,
        type: String,
        amountMinor: Long,
        category: String,
        description: String,
        paymentMethod: String,
        dateEpochMillis: Long,
        allowanceRecordId: Long?,
        accountId: Long?
    ) -> Boolean,
    onApprovePendingTransfer: suspend (pendingId: Long, fromAccountId: Long, toAccountId: Long, amountMinor: Long, dateEpochMillis: Long, note: String) -> Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { TrackItDatabase.getInstance(context) }
    val pending by database.pendingMpesaTransactionDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(MpesaSmsReceiver.PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(MpesaSmsReceiver.KEY_ENABLED, false)) }
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
    }
    var selectedPendingId by remember { mutableStateOf<Long?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        enabled = granted
        prefs.edit().putBoolean(MpesaSmsReceiver.KEY_ENABLED, granted).apply()
    }

    val selectedPending = pending.firstOrNull { it.id == selectedPendingId }
    if (selectedPending != null) {
        var treatAsTransfer by remember(selectedPending.id) { mutableStateOf(selectedPending.isAccountTransfer) }
        if (treatAsTransfer) {
            BankTransferReviewScreen(
                pending = selectedPending,
                accounts = accounts,
                isSaving = isSaving,
                errorMessage = errorMessage,
                onClearError = onClearError,
                onApprove = onApprovePendingTransfer,
                onSwitchToExpense = { treatAsTransfer = false },
                onDismiss = { selectedPendingId = null }
            )
            return
        }
        TransactionEntryScreen(
            type = if (selectedPending.type == "TRANSFER") MoneyTransaction.EXPENSE else selectedPending.type,
            isSaving = isSaving,
            errorMessage = errorMessage,
            budgets = budgets,
            transactions = transactions,
            expenseCategories = expenseCategories,
            allowances = allowances,
            accounts = accounts,
            initialAmountMinor = selectedPending.amountMinor,
            initialDescription = selectedPending.description,
            initialDateEpochMillis = selectedPending.dateEpochMillis,
            initialPaymentMethod = when (selectedPending.source) { PendingMpesaTransaction.SOURCE_KCB, PendingMpesaTransaction.SOURCE_IM -> "Bank"; else -> "M-Pesa" },
            screenTitle = "Review ${selectedPending.source} transaction",
            screenSubtitle = "Check and complete the details before adding it to TrackIt.",
            submitButtonLabel = "Approve & save transaction",
            onBack = { selectedPendingId = null },
            onClearError = onClearError,
            onCreateCategory = onCreateCategory,
            onSave = { type, amountMinor, category, description, paymentMethod, dateEpochMillis, allowanceRecordId, accountId ->
                scope.launch {
                    val saved = onApprovePending(
                        selectedPending.id,
                        type,
                        amountMinor,
                        category,
                        description,
                        paymentMethod,
                        dateEpochMillis,
                        allowanceRecordId,
                        accountId
                    )
                    if (saved) {
                        database.pendingMpesaTransactionDao().deleteById(selectedPending.id)
                        selectedPendingId = null
                    }
                }
            }
        )
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SMS transaction import", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Detect confirmations and review them before saving.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onBack) { Text("Back to profile") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Your privacy", style = MaterialTheme.typography.titleMedium)
                Text(
                    "When enabled, TrackIt checks only new incoming messages from the M-Pesa/Safaricom, KCB and I&MBANK sender IDs for supported transaction confirmations. Processing happens on this device. SMS contents are not uploaded or retained. Unrelated messages are ignored.",
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
                Text("Detect new M-Pesa and bank messages", style = MaterialTheme.typography.titleSmall)
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
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(
                            when { item.isAccountTransfer -> "Possible account transfer"; item.type == MoneyTransaction.INCOME -> "Money received"; else -> "Money sent / paid" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "KSh " + NumberFormat.getNumberInstance(Locale("en", "KE")).format(item.amountMinor / 100.0),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (item.type == MoneyTransaction.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(item.dateEpochMillis)) +
                                " · " + item.source + " · Ref " + item.reference,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(if (item.isAccountTransfer) "Choose the bank account and your mobile-money account. You can switch this to an expense if it was sent to someone else." else "Open the standard transaction form to choose an account, select or create a category, and complete the details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = { selectedPendingId = item.id }) { Text("Review details") }
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

@Composable
private fun BankTransferReviewScreen(
    pending: PendingMpesaTransaction,
    accounts: List<FinancialAccount>,
    isSaving: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit,
    onApprove: suspend (Long, Long, Long, Long, Long, String) -> Boolean,
    onSwitchToExpense: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeAccounts = accounts.filter { !it.archived }
    var amount by remember(pending.id) { mutableStateOf("%.2f".format(Locale.ROOT, pending.amountMinor / 100.0)) }
    var fromId by remember(pending.id, accounts) {
        mutableStateOf(activeAccounts.firstOrNull { it.type == FinancialAccount.BANK }?.id ?: activeAccounts.firstOrNull()?.id ?: 0L)
    }
    var toId by remember(pending.id, accounts) {
        mutableStateOf(activeAccounts.firstOrNull { it.type == (pending.transferDestinationType ?: FinancialAccount.MPESA) && it.id != fromId }?.id
            ?: activeAccounts.firstOrNull { it.id != fromId }?.id ?: 0L)
    }
    var fromExpanded by remember { mutableStateOf(false) }
    var toExpanded by remember { mutableStateOf(false) }
    var validationError by remember(pending.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Review account transfer", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(pending.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(pending.dateEpochMillis)) +
                " · " + pending.source + " · Ref " + pending.reference,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (activeAccounts.size < 2) {
            Text("Add at least two active accounts (for example, your bank and M-Pesa/T-Kash) before approving this transfer.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it; validationError = null },
            label = { Text("Transfer amount (KSh)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("From account", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { fromExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(activeAccounts.firstOrNull { it.id == fromId }?.name ?: "Select source account")
            }
            DropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
                activeAccounts.forEach { account ->
                    DropdownMenuItem(text = { Text(account.name) }, onClick = {
                        fromId = account.id
                        if (toId == account.id) toId = activeAccounts.firstOrNull { it.id != account.id }?.id ?: 0L
                        fromExpanded = false
                    })
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("To account", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { toExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(activeAccounts.firstOrNull { it.id == toId }?.name ?: "Select destination account")
            }
            DropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
                activeAccounts.filter { it.id != fromId }.forEach { account ->
                    DropdownMenuItem(text = { Text(account.name) }, onClick = { toId = account.id; toExpanded = false })
                }
            }
        }
        Text(
            "This records a movement between your own accounts, not income or an expense. Bank charges are not added to the transfer amount.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (validationError != null) {
            Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onClearError) { Text("Dismiss message") }
        }
        Button(
            enabled = !isSaving && fromId != 0L && toId != 0L && fromId != toId,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val minor = runCatching {
                    BigDecimal(amount.trim().replace(",", "")).movePointRight(2).longValueExact()
                }.getOrNull()
                if (minor == null || minor <= 0L) {
                    validationError = "Enter a valid transfer amount greater than zero."
                } else {
                    scope.launch {
                        if (onApprove(pending.id, fromId, toId, minor, pending.dateEpochMillis, pending.description)) onDismiss()
                    }
                }
            }
        ) { Text(if (isSaving) "Saving…" else "Approve transfer") }
        OutlinedButton(onClick = onSwitchToExpense, modifier = Modifier.fillMaxWidth()) {
            Text("This was a payment to someone else — record as expense")
        }
        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
