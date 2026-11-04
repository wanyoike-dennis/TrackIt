package com.inventsolutions.trackit.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.AccountTransfer
import com.inventsolutions.trackit.data.local.FinancialAccount
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun accountAmount(value: String): Long? = runCatching {
    BigDecimal(value.trim().replace(",", "")).movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP).longValueExact()
}.getOrNull()

private fun accountKes(minor: Long): String {
    val f = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
        maximumFractionDigits = 2
    }
    return "KSh " + f.format(minor / 100.0)
}

private fun accountDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(epochMillis))

private fun FinancialAccount.typeLabel(): String = when (type) {
    FinancialAccount.MPESA -> "M-Pesa"
    FinancialAccount.BANK -> "Bank account"
    FinancialAccount.CASH -> "Cash"
    else -> "Other"
}

private fun FinancialRecordEntry.label(): String = when (entryType) {
    FinancialRecordEntry.CONTRIBUTION -> "Savings contribution"
    FinancialRecordEntry.WITHDRAWAL -> "Savings withdrawal"
    FinancialRecordEntry.REPAYMENT -> "Debt repayment"
    FinancialRecordEntry.COLLECTION -> "Debt collection"
    else -> "Financial movement"
}

@Composable
fun AccountsScreen(
    accounts: List<FinancialAccount>,
    transactions: List<MoneyTransaction>,
    transfers: List<AccountTransfer>,
    financialEntries: List<FinancialRecordEntry>,
    errorMessage: String?,
    onAddAccount: (String, String, Long) -> Unit,
    onUpdateAccount: (FinancialAccount) -> Unit,
    onTransfer: (Long, Long, Long, Long, String) -> Unit,
    onArchive: (FinancialAccount) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    var selectedAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    val active = accounts.filter { !it.archived }
    val balances = accounts.associate { account ->
        val transactionMovement = transactions.filter {
            it.accountId == account.id && it.createdAtEpochMillis >= account.openingDateEpochMillis
        }.sumOf { if (it.type == MoneyTransaction.INCOME) it.amountMinor else -it.amountMinor }
        val transferMovement = transfers.filter {
            it.createdAtEpochMillis >= account.openingDateEpochMillis &&
                (it.fromAccountId == account.id || it.toAccountId == account.id)
        }.sumOf { if (it.toAccountId == account.id) it.amountMinor else -it.amountMinor }
        val financialMovement = financialEntries.filter {
            it.accountId == account.id && it.createdAtEpochMillis >= account.openingDateEpochMillis
        }.sumOf {
            when (it.entryType) {
                FinancialRecordEntry.CONTRIBUTION, FinancialRecordEntry.REPAYMENT -> -it.amountMinor
                FinancialRecordEntry.WITHDRAWAL, FinancialRecordEntry.COLLECTION -> it.amountMinor
                else -> 0L
            }
        }
        account.id to (account.openingBalanceMinor + transactionMovement + transferMovement + financialMovement)
    }
    val detailAccount = accounts.firstOrNull { it.id == selectedAccount?.id }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(if (detailAccount == null) "Accounts" else detailAccount.name,
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (detailAccount == null) "Track the money you hold in different places."
                    else "Account balance and recent activity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { if (detailAccount != null) selectedAccount = null else onBack() }) {
                Text(if (detailAccount != null) "Back" else "Close")
            }
        }

        if (detailAccount == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showAdd = true }) { Text("Add account") }
                OutlinedButton(onClick = { showTransfer = true }, enabled = active.size >= 2) { Text("Transfer") }
            }
            if (active.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No accounts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Add M-Pesa, cash or a bank account and enter the balance you currently have in it.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("TOTAL ACROSS ACTIVE ACCOUNTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold)
                    Text(accountKes(active.sumOf { balances[it.id] ?: it.openingBalanceMinor }),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${active.size} active ${if (active.size == 1) "account" else "accounts"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Your accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { account ->
                    var menuExpanded by remember(account.id) { mutableStateOf(false) }
                    Card(onClick = { selectedAccount = account },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 15.dp, top = 13.dp, end = 8.dp, bottom = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (account.archived) "Archived · ${account.typeLabel()}" else account.typeLabel(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(accountKes(balances[account.id] ?: account.openingBalanceMinor),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    color = if (account.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                                Text("Balance", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box {
                                TextButton(onClick = { menuExpanded = true }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("⋮", style = MaterialTheme.typography.titleLarge)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("View activity") }, onClick = {
                                        selectedAccount = account
                                        menuExpanded = false
                                    })
                                    DropdownMenuItem(text = { Text("Edit account") }, onClick = {
                                        editingAccount = account
                                        menuExpanded = false
                                    })
                                    if (!account.archived) {
                                        DropdownMenuItem(text = { Text("Archive account") }, onClick = {
                                            onArchive(account)
                                            menuExpanded = false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Available balance", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(accountKes(balances[detailAccount.id] ?: detailAccount.openingBalanceMinor),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text("${detailAccount.typeLabel()} • Opening balance ${accountKes(detailAccount.openingBalanceMinor)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Opened ${accountDate(detailAccount.openingDateEpochMillis)}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { editingAccount = detailAccount }) { Text("Edit account details") }
                }
            }
            val accountTransactions = transactions.filter { it.accountId == detailAccount.id }
            val accountTransfers = transfers.filter {
                it.fromAccountId == detailAccount.id || it.toAccountId == detailAccount.id
            }
            val accountEntries = financialEntries.filter { it.accountId == detailAccount.id }
            data class ActivityRow(val time: Long, val title: String, val detail: String, val amount: Long, val incoming: Boolean)
            val activity = buildList {
                accountTransactions.forEach {
                    val income = it.type == MoneyTransaction.INCOME
                    add(ActivityRow(it.dateEpochMillis, it.description.ifBlank { it.category },
                        if (income) "Income • ${it.category}" else "Expense • ${it.category}",
                        it.amountMinor, income))
                }
                accountTransfers.forEach {
                    val incoming = it.toAccountId == detailAccount.id
                    val other = accounts.firstOrNull { account ->
                        account.id == if (incoming) it.fromAccountId else it.toAccountId
                    }?.name ?: "Other account"
                    add(ActivityRow(it.dateEpochMillis, if (incoming) "Transfer received" else "Transfer sent",
                        other + if (it.note.isBlank()) "" else " • ${it.note}",
                        it.amountMinor, incoming))
                }
                accountEntries.forEach {
                    val incoming = it.entryType == FinancialRecordEntry.WITHDRAWAL ||
                        it.entryType == FinancialRecordEntry.COLLECTION
                    add(ActivityRow(it.createdAtEpochMillis, it.label(), it.note.ifBlank { "Savings / debt record" },
                        it.amountMinor, incoming))
                }
            }.sortedByDescending { it.time }
            Text("Account activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (activity.isEmpty()) {
                Text("No activity has been recorded for this account yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(activity) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold)
                                    Text(item.detail, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(accountDate(item.time), style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text((if (item.incoming) "+ " else "− ") + accountKes(item.amount),
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.incoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingAccount != null) {
        val account = editingAccount!!
        var name by remember(account.id) { mutableStateOf(account.name) }
        var type by remember(account.id) { mutableStateOf(account.type) }
        var typeExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text("Edit account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true)
                    OutlinedButton(onClick = { typeExpanded = true }) { Text(when (type) {
                        FinancialAccount.MPESA -> "M-Pesa"
                        FinancialAccount.BANK -> "Bank"
                        FinancialAccount.CASH -> "Cash"
                        else -> "Other"
                    }) }
                    DropdownMenu(typeExpanded, { typeExpanded = false }) {
                        listOf(FinancialAccount.MPESA, FinancialAccount.CASH, FinancialAccount.BANK, FinancialAccount.OTHER).forEach { item ->
                            DropdownMenuItem(text = { Text(when (item) {
                                FinancialAccount.MPESA -> "M-Pesa"
                                FinancialAccount.BANK -> "Bank"
                                FinancialAccount.CASH -> "Cash"
                                else -> "Other"
                            }) }, onClick = { type = item; typeExpanded = false })
                        }
                    }
                    Text("The opening balance and opening date are kept unchanged so existing account history remains accurate.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        onUpdateAccount(account.copy(name = name.trim(), type = type))
                        editingAccount = null
                    }
                }) { Text("Save changes") }
            },
            dismissButton = { TextButton(onClick = { editingAccount = null }) { Text("Cancel") } }
        )
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(FinancialAccount.MPESA) }
        var balance by remember { mutableStateOf("") }
        var typeExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true)
                    OutlinedButton(onClick = { typeExpanded = true }) { Text(when (type) {
                        FinancialAccount.MPESA -> "M-Pesa"
                        FinancialAccount.BANK -> "Bank"
                        FinancialAccount.CASH -> "Cash"
                        else -> "Other"
                    }) }
                    DropdownMenu(typeExpanded, { typeExpanded = false }) {
                        listOf(FinancialAccount.MPESA, FinancialAccount.CASH, FinancialAccount.BANK, FinancialAccount.OTHER).forEach { item ->
                            DropdownMenuItem(text = { Text(when (item) {
                                FinancialAccount.MPESA -> "M-Pesa"
                                FinancialAccount.BANK -> "Bank"
                                FinancialAccount.CASH -> "Cash"
                                else -> "Other"
                            }) }, onClick = { type = item; typeExpanded = false })
                        }
                    }
                    OutlinedTextField(balance, { balance = it }, label = { Text("Current balance (KSh)") }, singleLine = true)
                    Text("Enter what is actually in this account today. This becomes its opening balance.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = accountAmount(balance)
                    if (name.isNotBlank() && parsed != null && parsed >= 0L) {
                        onAddAccount(name, type, parsed)
                        showAdd = false
                    }
                }) { Text("Save account") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    if (showTransfer) {
        var fromId by remember(active) { mutableStateOf(active.firstOrNull()?.id ?: 0L) }
        var toId by remember(active) { mutableStateOf(active.getOrNull(1)?.id ?: 0L) }
        var amount by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var fromExpanded by remember { mutableStateOf(false) }
        var toExpanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showTransfer = false },
            title = { Text("Transfer between accounts") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("From")
                    OutlinedButton(onClick = { fromExpanded = true }) {
                        Text(active.firstOrNull { it.id == fromId }?.name ?: "Select source")
                    }
                    DropdownMenu(fromExpanded, { fromExpanded = false }) {
                        active.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { fromId = item.id; fromExpanded = false }) }
                    }
                    Text("To")
                    OutlinedButton(onClick = { toExpanded = true }) {
                        Text(active.firstOrNull { it.id == toId }?.name ?: "Select destination")
                    }
                    DropdownMenu(toExpanded, { toExpanded = false }) {
                        active.filter { it.id != fromId }.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { toId = item.id; toExpanded = false }) }
                    }
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount (KSh)") }, singleLine = true)
                    OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
                    Text("A transfer moves money between accounts. It is not recorded as income or an expense.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = accountAmount(amount)
                    if (fromId != toId && parsed != null && parsed > 0L) {
                        onTransfer(fromId, toId, parsed, System.currentTimeMillis(), note)
                        showTransfer = false
                    }
                }) { Text("Record transfer") }
            },
            dismissButton = { TextButton(onClick = { showTransfer = false }) { Text("Cancel") } }
        )
    }

    if (errorMessage != null) {
        AlertDialog(onDismissRequest = onClearError, title = { Text("Couldn't complete action") },
            text = { Text(errorMessage) }, confirmButton = { TextButton(onClick = onClearError) { Text("OK") } })
    }
}
