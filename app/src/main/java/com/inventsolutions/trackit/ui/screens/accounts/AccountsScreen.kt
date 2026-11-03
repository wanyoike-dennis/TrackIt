package com.inventsolutions.trackit.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
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

private fun FinancialAccount.typeLabel(): String = when (type) {
    FinancialAccount.MPESA -> "M-Pesa"
    FinancialAccount.BANK -> "Bank account"
    FinancialAccount.CASH -> "Cash"
    else -> "Other"
}

@Composable
fun AccountsScreen(
    accounts: List<FinancialAccount>,
    transactions: List<MoneyTransaction>,
    transfers: List<AccountTransfer>,
    errorMessage: String?,
    onAddAccount: (String, String, Long) -> Unit,
    onTransfer: (Long, Long, Long, Long, String) -> Unit,
    onArchive: (FinancialAccount) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    val active = accounts.filter { !it.archived }
    val balances = accounts.associate { account ->
        val transactionMovement = transactions.filter {
            it.accountId == account.id && it.createdAtEpochMillis >= account.openingDateEpochMillis
        }.sumOf { if (it.type == MoneyTransaction.INCOME) it.amountMinor else -it.amountMinor }
        val transferMovement = transfers.filter {
            it.createdAtEpochMillis >= account.openingDateEpochMillis &&
                (it.fromAccountId == account.id || it.toAccountId == account.id)
        }.sumOf { if (it.toAccountId == account.id) it.amountMinor else -it.amountMinor }
        account.id to (account.openingBalanceMinor + transactionMovement + transferMovement)
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Accounts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Track the money you hold in different places.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onBack) { Text("Close") }
        }
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
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(accounts, key = { it.id }) { account ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(account.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (account.archived) "Archived" else account.typeLabel(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(accountKes(balances[account.id] ?: account.openingBalanceMinor),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text("Opening balance: ${accountKes(account.openingBalanceMinor)}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!account.archived) {
                            TextButton(onClick = { onArchive(account) }) { Text("Archive account") }
                        }
                    }
                }
            }
        }
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
                    Column {
                        OutlinedButton(onClick = { typeExpanded = true }) { Text(when(type) {
                            FinancialAccount.MPESA -> "M-Pesa"
                            FinancialAccount.BANK -> "Bank"
                            FinancialAccount.CASH -> "Cash"
                            else -> "Other"
                        }) }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            listOf(FinancialAccount.MPESA, FinancialAccount.CASH, FinancialAccount.BANK, FinancialAccount.OTHER).forEach { item ->
                                DropdownMenuItem(text = { Text(when(item) {
                                    FinancialAccount.MPESA -> "M-Pesa"
                                    FinancialAccount.CASH -> "Cash"
                                    FinancialAccount.BANK -> "Bank"
                                    else -> "Other"
                                }) }, onClick = { type = item; typeExpanded = false })
                            }
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
