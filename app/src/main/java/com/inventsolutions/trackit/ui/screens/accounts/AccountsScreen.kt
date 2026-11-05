package com.inventsolutions.trackit.ui.screens.accounts

import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.ui.components.TrackItIcon
import com.inventsolutions.trackit.ui.theme.TrackItNavy
import com.inventsolutions.trackit.ui.theme.TrackItEmeraldLight
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.AccountTransfer
import com.inventsolutions.trackit.data.local.AccountReconciliation
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
private fun AccountTypeIcon(type: String, archived: Boolean) {
    val icon = when (type) {
        FinancialAccount.MPESA, FinancialAccount.CASH -> R.drawable.ic_lucide_wallet
        FinancialAccount.BANK -> R.drawable.ic_lucide_landmark
        else -> R.drawable.ic_lucide_receipt
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
            TrackItIcon(
                icon,
                null,
                tint = if (archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                size = 24.dp
            )
        }
    }
}

@Composable
private fun AccountListCard(
    account: FinancialAccount,
    balance: Long,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit
) {
    var menuExpanded by remember(account.id) { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountTypeIcon(account.type, account.archived)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    account.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (account.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (account.archived) "${account.typeLabel()} · Archived" else account.typeLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    accountKes(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (account.archived) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                Text("Current balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                TextButton(
                    onClick = { menuExpanded = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("⋮", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("View activity") }, onClick = { menuExpanded = false; onClick() })
                    DropdownMenuItem(text = { Text("Edit account") }, onClick = { menuExpanded = false; onEdit() })
                    if (!account.archived) {
                        DropdownMenuItem(text = { Text("Archive account") }, onClick = { menuExpanded = false; onArchive() })
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountActivityCard(
    title: String,
    detail: String,
    time: Long,
    amount: Long,
    incoming: Boolean,
    balanceAfter: Long
) {
    val transfer = title.startsWith("Transfer")
    val icon = when {
        transfer -> R.drawable.ic_lucide_repeat
        incoming -> R.drawable.ic_lucide_arrow_down_left
        else -> R.drawable.ic_lucide_arrow_up_right
    }
    val movementColor = if (incoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    TrackItIcon(icon, null, tint = movementColor, size = 21.dp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(accountDate(time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text((if (incoming) "+ " else "− ") + accountKes(amount), fontWeight = FontWeight.Bold,
                    color = movementColor, style = MaterialTheme.typography.bodyMedium)
                Text("Balance " + accountKes(balanceAfter), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AccountsScreen(
    accounts: List<FinancialAccount>,
    transactions: List<MoneyTransaction>,
    transfers: List<AccountTransfer>,
    financialEntries: List<FinancialRecordEntry>,
    reconciliations: List<AccountReconciliation>,
    errorMessage: String?,
    onAddAccount: (String, String, Long) -> Unit,
    onUpdateAccount: (FinancialAccount) -> Unit,
    onTransfer: (Long, Long, Long, Long, String) -> Unit,
    onReconcile: (Long, Long, String) -> Unit,
    onArchive: (FinancialAccount) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    var selectedAccount by remember { mutableStateOf<FinancialAccount?>(null) }
    var showReconcile by remember { mutableStateOf(false) }
    var activitySearch by remember { mutableStateOf("") }
    var activityType by remember { mutableStateOf("All activity") }
    var activityPeriod by remember { mutableStateOf("All time") }
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
        val reconciliationMovement = reconciliations.filter {
            it.accountId == account.id && it.reconciledAtEpochMillis >= account.openingDateEpochMillis
        }.sumOf { it.adjustmentMinor }
        account.id to (account.openingBalanceMinor + transactionMovement + transferMovement + financialMovement + reconciliationMovement)
    }
    val detailAccount = accounts.firstOrNull { it.id == selectedAccount?.id }

    BackHandler {
        if (detailAccount != null) selectedAccount = null else onBack()
    }

    val archived = accounts.filter { it.archived }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
            .then(if (detailAccount != null) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (detailAccount == null) "Accounts" else detailAccount.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (detailAccount == null) "See where your money is held."
                else "Account balance and recent activity.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (detailAccount == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL BALANCE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
                                TrackItIcon(R.drawable.ic_lucide_wallet, null, tint = MaterialTheme.colorScheme.primary, size = 26.dp)
                            }
                        }
                    }
                    Text(
                        accountKes(active.sumOf { balances[it.id] ?: it.openingBalanceMinor }),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${active.size} active ${if (active.size == 1) "account" else "accounts"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showAdd = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    TrackItIcon(R.drawable.ic_lucide_plus, null, tint = MaterialTheme.colorScheme.onPrimary, size = 20.dp)
                    Text("Add account")
                }
                OutlinedButton(
                    onClick = { showTransfer = true },
                    modifier = Modifier.weight(1f),
                    enabled = active.size >= 2,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    TrackItIcon(R.drawable.ic_lucide_repeat, null, size = 20.dp)
                    Text("Transfer")
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${accounts.size} total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (accounts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrackItIcon(R.drawable.ic_lucide_wallet, null, tint = MaterialTheme.colorScheme.primary, size = 34.dp)
                        Text("No accounts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Add M-Pesa, cash or a bank account to start tracking where your money is held.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (active.isNotEmpty()) {
                        item {
                            Text(
                                "ACTIVE ACCOUNTS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(active, key = { it.id }) { account ->
                            AccountListCard(
                                account = account,
                                balance = balances[account.id] ?: account.openingBalanceMinor,
                                onClick = { selectedAccount = account },
                                onEdit = { editingAccount = account },
                                onArchive = { onArchive(account) }
                            )
                        }
                    }
                    if (archived.isNotEmpty()) {
                        item {
                            Text(
                                "ARCHIVED",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(archived, key = { it.id }) { account ->
                            AccountListCard(
                                account = account,
                                balance = balances[account.id] ?: account.openingBalanceMinor,
                                onClick = { selectedAccount = account },
                                onEdit = { editingAccount = account },
                                onArchive = { onArchive(account) }
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = TrackItNavy)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("AVAILABLE BALANCE", style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.72f))
                    Text(
                        accountKes(balances[detailAccount.id] ?: detailAccount.openingBalanceMinor),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccountTypeIcon(detailAccount.type, detailAccount.archived)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(detailAccount.typeLabel(), color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Opening balance · ${accountKes(detailAccount.openingBalanceMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }
                    Text(
                        "Opened ${accountDate(detailAccount.openingDateEpochMillis)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    TextButton(onClick = { editingAccount = detailAccount }) {
                        TrackItIcon(R.drawable.ic_lucide_edit, null, tint = TrackItEmeraldLight, size = 20.dp)
                        Text("Edit account details", color = Color.White)
                    }
                    TextButton(onClick = { showReconcile = true }, enabled = !detailAccount.archived) {
                        TrackItIcon(R.drawable.ic_lucide_repeat, null, tint = TrackItEmeraldLight, size = 20.dp)
                        Text("Reconcile account", color = Color.White)
                    }
                }
            }

            val accountTransactions = transactions.filter {
                it.accountId == detailAccount.id && it.createdAtEpochMillis >= detailAccount.openingDateEpochMillis
            }
            val accountTransfers = transfers.filter {
                (it.fromAccountId == detailAccount.id || it.toAccountId == detailAccount.id) &&
                    it.createdAtEpochMillis >= detailAccount.openingDateEpochMillis
            }
            val accountEntries = financialEntries.filter {
                it.accountId == detailAccount.id && it.createdAtEpochMillis >= detailAccount.openingDateEpochMillis
            }
            val accountReconciliations = reconciliations.filter {
                it.accountId == detailAccount.id && it.reconciledAtEpochMillis >= detailAccount.openingDateEpochMillis
            }
            val latestReconciliation = accountReconciliations.maxByOrNull { it.reconciledAtEpochMillis }
            data class ActivityRow(val time: Long, val title: String, val detail: String, val amount: Long,
                val incoming: Boolean, val kind: String, val balanceAfter: Long = 0L)
            val activity = buildList {
                accountTransactions.forEach {
                    val income = it.type == MoneyTransaction.INCOME
                    add(ActivityRow(it.dateEpochMillis, it.description.ifBlank { it.category },
                        if (income) "Income · ${it.category}" else "Expense · ${it.category}",
                        it.amountMinor, income, if (income) "Income" else "Expenses"))
                }
                accountTransfers.forEach {
                    val incoming = it.toAccountId == detailAccount.id
                    val other = accounts.firstOrNull { account ->
                        account.id == if (incoming) it.fromAccountId else it.toAccountId
                    }?.name ?: "Other account"
                    add(ActivityRow(it.dateEpochMillis, if (incoming) "Transfer received" else "Transfer sent",
                        other + if (it.note.isBlank()) "" else " · ${it.note}",
                        it.amountMinor, incoming, "Transfers"))
                }
                accountEntries.forEach {
                    val incoming = it.entryType == FinancialRecordEntry.WITHDRAWAL ||
                        it.entryType == FinancialRecordEntry.COLLECTION
                    add(ActivityRow(it.createdAtEpochMillis, it.label(), it.note.ifBlank { "Savings / debt record" },
                        it.amountMinor, incoming, "Savings & debt"))
                }
                accountReconciliations.forEach {
                    add(ActivityRow(it.reconciledAtEpochMillis,
                        if (it.adjustmentMinor == 0L) "Reconciled · balances matched" else "Balance adjustment",
                        it.note.ifBlank { "Account reconciliation" }, kotlin.math.abs(it.adjustmentMinor),
                        it.adjustmentMinor >= 0L, "Adjustments"))
                }
            }.sortedBy { it.time }
            var runningBalance = detailAccount.openingBalanceMinor
            val activityWithBalances = activity.map { item ->
                runningBalance += if (item.incoming) item.amount else -item.amount
                item.copy(balanceAfter = runningBalance)
            }.asReversed()
            val cutoff = when (activityPeriod) {
                "30 days" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
                "90 days" -> System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
                else -> Long.MIN_VALUE
            }
            val visibleActivity = activityWithBalances.filter { item ->
                (activityType == "All activity" || item.kind == activityType) && item.time >= cutoff &&
                    (activitySearch.isBlank() || item.title.contains(activitySearch, true) || item.detail.contains(activitySearch, true))
            }


            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("RECONCILIATION", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Text(
                        latestReconciliation?.let { "Last checked " + accountDate(it.reconciledAtEpochMillis) }
                            ?: "Not reconciled yet",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                    )
                    latestReconciliation?.let {
                        Text(
                            "Statement " + accountKes(it.statementBalanceMinor) +
                                " · TrackIt before " + accountKes(it.systemBalanceMinor),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (it.adjustmentMinor == 0L) "Balances matched"
                            else "Adjustment recorded: " + (if (it.adjustmentMinor > 0) "+" else "−") +
                                accountKes(kotlin.math.abs(it.adjustmentMinor)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transaction history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${visibleActivity.size} of ${activity.size}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(value = activitySearch, onValueChange = { activitySearch = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search account history") })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var typeMenu by remember { mutableStateOf(false) }
                var periodMenu by remember { mutableStateOf(false) }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { typeMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(activityType) }
                    DropdownMenu(typeMenu, { typeMenu = false }) {
                        listOf("All activity", "Income", "Expenses", "Transfers", "Savings & debt", "Adjustments").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { activityType = it; typeMenu = false })
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { periodMenu = true }, modifier = Modifier.fillMaxWidth()) { Text(activityPeriod) }
                    DropdownMenu(periodMenu, { periodMenu = false }) {
                        listOf("All time", "30 days", "90 days").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { activityPeriod = it; periodMenu = false })
                        }
                    }
                }
            }
            if (activity.isEmpty() || visibleActivity.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (activity.isEmpty()) "No activity yet" else "No matching transactions", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Income, expenses and transfers recorded against this account will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    visibleActivity.forEach { item ->
                        AccountActivityCard(item.title, item.detail, item.time, item.amount, item.incoming, item.balanceAfter)
                    }
                }
            }
        }
    }


    if (showReconcile && detailAccount != null) {
        var actualBalance by remember(detailAccount.id) {
            mutableStateOf(((balances[detailAccount.id] ?: detailAccount.openingBalanceMinor) / 100.0).toString())
        }
        var note by remember(detailAccount.id) { mutableStateOf("") }
        val currentBalance = balances[detailAccount.id] ?: detailAccount.openingBalanceMinor
        val parsedBalance = accountAmount(actualBalance)
        val difference = parsedBalance?.minus(currentBalance)
        AlertDialog(
            onDismissRequest = { showReconcile = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
                            TrackItIcon(
                                R.drawable.ic_lucide_repeat,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 23.dp
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Reconcile balance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(detailAccount.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Compare TrackIt with the balance shown by your bank, M-Pesa or cash count.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("TRACKIT BALANCE", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(accountKes(currentBalance), style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("STATEMENT / ACTUAL BALANCE", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = actualBalance,
                            onValueChange = { actualBalance = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter amount in KSh") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    if (difference != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    if (difference == 0L) "BALANCES MATCH" else "DIFFERENCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    if (difference == 0L) "No adjustment needed"
                                    else (if (difference > 0L) "+ " else "− ") + accountKes(kotlin.math.abs(difference)),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    if (difference == 0L) "Your recorded and actual balances are aligned."
                                    else if (difference > 0L) "TrackIt will increase this account balance by the difference."
                                    else "TrackIt will reduce this account balance by the difference.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("NOTE (OPTIONAL)", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Bank statement checked") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    Text(
                        "This records a separate adjustment. Your existing transactions are not changed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = parsedBalance != null && parsedBalance >= 0L,
                    onClick = {
                        parsedBalance?.let { onReconcile(detailAccount.id, it, note.trim()) }
                        showReconcile = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Confirm reconciliation") }
            },
            dismissButton = {
                TextButton(onClick = { showReconcile = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (editingAccount != null) {
        val account = editingAccount!!
        var name by remember(account.id) { mutableStateOf(account.name) }
        var type by remember(account.id) { mutableStateOf(account.type) }
        var typeExpanded by remember { mutableStateOf(false) }
        val selectedTypeLabel = when (type) {
            FinancialAccount.MPESA -> "M-Pesa"
            FinancialAccount.BANK -> "Bank"
            FinancialAccount.CASH -> "Cash"
            else -> "Other"
        }
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(Modifier.padding(11.dp), contentAlignment = Alignment.Center) {
                            TrackItIcon(
                                R.drawable.ic_lucide_edit,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                size = 23.dp
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Edit account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Update how this account is identified", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("ACCOUNT NAME", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. Main M-Pesa") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("ACCOUNT TYPE", style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            OutlinedButton(
                                onClick = { typeExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(selectedTypeLabel, modifier = Modifier.weight(1f))
                                Text("⌄", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleLarge)
                            }
                            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                listOf(
                                    FinancialAccount.MPESA,
                                    FinancialAccount.BANK,
                                    FinancialAccount.CASH,
                                    FinancialAccount.OTHER
                                ).forEach { item ->
                                    val label = when (item) {
                                        FinancialAccount.MPESA -> "M-Pesa"
                                        FinancialAccount.BANK -> "Bank"
                                        FinancialAccount.CASH -> "Cash"
                                        else -> "Other"
                                    }
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { type = item; typeExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("ACCOUNT RECORDS", style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Opening balance: " + accountKes(account.openingBalanceMinor),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Opened " + accountDate(account.openingDateEpochMillis),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Your opening balance and date stay unchanged, so existing history remains accurate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        if (name.isNotBlank()) {
                            onUpdateAccount(account.copy(name = name.trim(), type = type))
                            editingAccount = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Save changes") }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
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
