package com.inventsolutions.trackit.ui.screens.finances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.text.NumberFormat
import java.util.Locale

private enum class FinanceSection { DEBTS, SAVINGS, ALLOWANCE }

private fun financeKes(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
    }
    return "KSh " + formatter.format(minor / 100.0)
}

@Composable
fun FinancialPlansScreen(
    records: List<FinancialRecord>,
    entries: List<FinancialRecordEntry>,
    transactions: List<MoneyTransaction>,
    errorMessage: String?,
    onBackToHome: () -> Unit,
    onAdd: (type: String, title: String, targetMinor: Long, dueDateEpochMillis: Long?, notes: String, savingsCategory: String, contributionMethod: String, plannedContributionMinor: Long) -> Unit,
    onAddProgress: (FinancialRecord, Long, String) -> Unit,
    onWithdraw: (FinancialRecord, Long, String) -> Unit,
    onEdit: (FinancialRecord) -> Unit,
    onDelete: (Long) -> Unit,
    onClearError: () -> Unit
) {
    var section by remember { mutableStateOf(FinanceSection.SAVINGS) }
    var showCreate by remember { mutableStateOf(false) }
    var progressRecord by remember { mutableStateOf<FinancialRecord?>(null) }
    var withdrawalRecord by remember { mutableStateOf<FinancialRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<FinancialRecord?>(null) }
    var editRecord by remember { mutableStateOf<FinancialRecord?>(null) }
    var historyRecord by remember { mutableStateOf<FinancialRecord?>(null) }

    val debtsOwedToMe = records.filter { it.type == FinancialRecord.DEBT_OWED_TO_ME }
    val debtsIOwe = records.filter { it.type == FinancialRecord.DEBT_I_OWE }
    val savings = records.filter { it.type == FinancialRecord.SAVINGS_GOAL }
    val allowances = records.filter { it.type == FinancialRecord.ALLOWANCE }
    val visibleRecords = when (section) {
        FinanceSection.DEBTS -> (debtsOwedToMe + debtsIOwe).sortedByDescending { it.createdAtEpochMillis }
        FinanceSection.SAVINGS -> savings
        FinanceSection.ALLOWANCE -> allowances
    }
    val savedTotal = savings.sumOf { it.currentMinor }
    val savingsTarget = savings.sumOf { it.targetMinor }
    val savingsProgress = if (savingsTarget > 0L) (savedTotal.toFloat() / savingsTarget).coerceIn(0f, 1f) else 0f
    val owedToMe = debtsOwedToMe.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
    val iOwe = debtsIOwe.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
    val allowanceLeft = allowances.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("MONEY PLANS", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Give your money a purpose.", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Small steps. Clear progress. Less money stress.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBackToHome, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
                    Text("Home")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> "YOUR DEBT POSITION"
                                    FinanceSection.SAVINGS -> "YOUR SAVINGS JOURNEY"
                                    FinanceSection.ALLOWANCE -> "YOUR SPENDING PLAN"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> financeKes(iOwe)
                                    FinanceSection.SAVINGS -> financeKes(savedTotal)
                                    FinanceSection.ALLOWANCE -> financeKes(allowances.sumOf { it.currentMinor })
                                },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> "Still to repay · excludes money owed to you"
                                    FinanceSection.SAVINGS -> "Saved across ${savings.size} ${if (savings.size == 1) "goal" else "goals"}"
                                    FinanceSection.ALLOWANCE -> "Spent across ${allowances.size} ${if (allowances.size == 1) "allowance" else "allowances"}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            Modifier.size(58.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.09f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> "↔"
                                    FinanceSection.SAVINGS -> "✦"
                                    FinanceSection.ALLOWANCE -> "◷"
                                },
                                fontSize = 30.sp, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (section == FinanceSection.SAVINGS) {
                        LinearProgressIndicator(
                            progress = { savingsProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(savingsProgress * 100).toInt()}% of target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${financeKes((savingsTarget - savedTotal).coerceAtLeast(0L))} to go",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    if (section == FinanceSection.DEBTS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroMetric(
                                modifier = Modifier.weight(1f),
                                label = "Owed to you",
                                value = financeKes(owedToMe),
                                hint = "To collect",
                                symbol = "↙"
                            )
                            HeroMetric(
                                modifier = Modifier.weight(1f),
                                label = "You owe",
                                value = financeKes(iOwe),
                                hint = "To repay",
                                symbol = "↗"
                            )
                        }
                    }
                    if (section == FinanceSection.ALLOWANCE) {
                        Text(
                            if (allowanceLeft > 0L) "${financeKes(allowanceLeft)} left in your allowance plans"
                            else "Your allowance plans are used up.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinanceTab("Debts", "↔", section == FinanceSection.DEBTS, Modifier.weight(1f)) { section = FinanceSection.DEBTS }
                FinanceTab("Savings", "✦", section == FinanceSection.SAVINGS, Modifier.weight(1f)) { section = FinanceSection.SAVINGS }
                FinanceTab("Allowance", "◷", section == FinanceSection.ALLOWANCE, Modifier.weight(1f)) { section = FinanceSection.ALLOWANCE }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (section) {
                            FinanceSection.DEBTS -> "Debt tracker"
                            FinanceSection.SAVINGS -> "Your goals"
                            FinanceSection.ALLOWANCE -> "Spending limits"
                        },
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (section) {
                            FinanceSection.DEBTS -> "Know what is coming in and what must go out."
                            FinanceSection.SAVINGS -> "Make every contribution move you closer."
                            FinanceSection.ALLOWANCE -> "A limit, not extra money. Record each purchase here or as an expense—never both."
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = { showCreate = true }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        when (section) {
                            FinanceSection.DEBTS -> "+ Debt"
                            FinanceSection.SAVINGS -> "+ Goal"
                            FinanceSection.ALLOWANCE -> "+ Allowance"
                        }
                    )
                }
            }
        }

        if (errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(errorMessage, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onClearError) { Text("Dismiss") }
                    }
                }
            }
        }

        if (visibleRecords.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier.size(62.dp).clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> "↔"
                                    FinanceSection.SAVINGS -> "✦"
                                    FinanceSection.ALLOWANCE -> "◷"
                                },
                                fontSize = 30.sp, color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            when (section) {
                                FinanceSection.DEBTS -> "Start with a clear picture"
                                FinanceSection.SAVINGS -> "Your next goal starts here"
                                FinanceSection.ALLOWANCE -> "Plan before you spend"
                            },
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                        )
                        Text(
                            when (section) {
                                FinanceSection.DEBTS -> "Add money you owe or money someone needs to pay you. Track each repayment or collection."
                                FinanceSection.SAVINGS -> "Set a target for an emergency fund, a new phone, rent, or anything you are working towards."
                                FinanceSection.ALLOWANCE -> "Set a spending limit, then record actual purchases in Add Expense. You can link each expense to an allowance so the remaining balance stays accurate."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                when (section) {
                                    FinanceSection.DEBTS -> "Add your first debt"
                                    FinanceSection.SAVINGS -> "Create your first goal"
                                    FinanceSection.ALLOWANCE -> "Set your first allowance"
                                }
                            )
                        }
                    }
                }
            }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                FinancialRecordCard(
                    record = record,
                    onProgress = { progressRecord = record },
                    onEdit = { editRecord = record },
                    onHistory = { historyRecord = record },
                    onWithdraw = { withdrawalRecord = record },
                    onDelete = { deleteRecord = record }
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("●", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                Text("Your plans are saved on this device and work offline.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showCreate) {
        var title by remember(section) { mutableStateOf(if (section == FinanceSection.SAVINGS) "Emergency fund" else "") }
        var amount by remember(section) { mutableStateOf(if (section == FinanceSection.SAVINGS) "60000" else "") }
        var dueDate by remember(section) { mutableStateOf("") }
        var notes by remember(section) { mutableStateOf("") }
        var debtType by remember { mutableStateOf(FinancialRecord.DEBT_I_OWE) }
        var savingsCategory by remember(section) { mutableStateOf(FinancialRecord.SAVINGS_EMERGENCY) }
        var contributionMethod by remember(section) { mutableStateOf(FinancialRecord.CONTRIBUTION_MONTHLY) }
        var plannedContribution by remember(section) { mutableStateOf(if (section == FinanceSection.SAVINGS) "2000" else "") }
        var validationError by remember(section) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(when (section) {
                FinanceSection.DEBTS -> "Add debt details"
                FinanceSection.SAVINGS -> "Set your savings goal"
                FinanceSection.ALLOWANCE -> "Set a spending limit"
            }, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (section == FinanceSection.DEBTS) {
                        Text("Choose the direction first", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = debtType == FinancialRecord.DEBT_I_OWE,
                                onClick = { debtType = FinancialRecord.DEBT_I_OWE },
                                label = { Text("I owe") })
                            FilterChip(selected = debtType == FinancialRecord.DEBT_OWED_TO_ME,
                                onClick = { debtType = FinancialRecord.DEBT_OWED_TO_ME },
                                label = { Text("Owed to me") })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it; validationError = null },
                        label = { Text(when (section) {
                            FinanceSection.DEBTS -> "Person / debt description"
                            FinanceSection.SAVINGS -> "What are you saving for?"
                            FinanceSection.ALLOWANCE -> "What is this allowance for?"
                        }) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = amount, onValueChange = { amount = it; validationError = null },
                        label = { Text(when (section) {
                            FinanceSection.DEBTS -> "Original debt (KSh)"
                            FinanceSection.SAVINGS -> "Savings target (KSh)"
                            FinanceSection.ALLOWANCE -> "Spending limit (KSh)"
                        }) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (section == FinanceSection.SAVINGS) {
                        OutlinedTextField(
                            value = savingsCategory,
                            onValueChange = { savingsCategory = it },
                            label = { Text("Savings category") },
                            supportingText = { Text("Examples: Emergency, goal-based, bills, long-term, business, lifestyle or general.") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Contribution plan", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_WEEKLY },
                                label = { Text("Weekly") })
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_MONTHLY },
                                label = { Text("Monthly") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_FLEXIBLE,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_FLEXIBLE },
                                label = { Text("Flexible") })
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_ONE_TIME,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_ONE_TIME },
                                label = { Text("One-time") })
                        }
                        if (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                            contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) {
                            OutlinedTextField(
                                value = plannedContribution,
                                onValueChange = { plannedContribution = it; validationError = null },
                                label = { Text("${if (contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) "Monthly" else "Weekly"} contribution (KSh)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Planning only—TrackIt will not move money automatically. Record each contribution when it happens.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (section != FinanceSection.ALLOWANCE) {
                        OutlinedTextField(value = dueDate, onValueChange = { dueDate = it; validationError = null },
                            label = { Text("Target / due date (YYYY-MM-DD, optional)") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    if (validationError != null) {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = parsed?.let { runCatching {
                        it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    }.getOrNull() }
                    val parsedDate = if (dueDate.isBlank()) null else runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                            .parse(dueDate.trim())?.time
                    }.getOrNull()
                    val parsedContribution = if (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                        contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) {
                        plannedContribution.trim().replace(",", "").toBigDecimalOrNull()?.let { value ->
                            runCatching { value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull()
                        }
                    } else 0L
                    when {
                        title.isBlank() -> validationError = "Add a name or description first."
                        minor == null || minor <= 0L -> validationError = "Enter an amount greater than zero."
                        dueDate.isNotBlank() && parsedDate == null && section != FinanceSection.ALLOWANCE ->
                            validationError = "Use a valid date in YYYY-MM-DD format."
                        section == FinanceSection.SAVINGS && savingsCategory.isBlank() ->
                            validationError = "Enter a savings category."
                        section == FinanceSection.SAVINGS &&
                            (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                                contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) &&
                            (parsedContribution == null || parsedContribution <= 0L) ->
                            validationError = "Enter a valid contribution amount."
                        else -> {
                            val type = when (section) {
                                FinanceSection.DEBTS -> debtType
                                FinanceSection.SAVINGS -> FinancialRecord.SAVINGS_GOAL
                                FinanceSection.ALLOWANCE -> FinancialRecord.ALLOWANCE
                            }
                            onAdd(
                                type, title, minor, parsedDate, notes,
                                savingsCategory.trim(), contributionMethod,
                                parsedContribution ?: 0L
                            )
                            showCreate = false
                        }
                    }
                }) { Text("Create plan") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }

    withdrawalRecord?.let { record ->
        var amount by remember(record.id) { mutableStateOf("") }
        var note by remember(record.id) { mutableStateOf("") }
        var validationError by remember(record.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { withdrawalRecord = null },
            title = { Text("Withdraw from savings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Text("${financeKes(record.currentMinor)} currently saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = amount, onValueChange = { amount = it; validationError = null },
                        label = { Text("Withdrawal amount (KSh)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it },
                        label = { Text("Reason (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    if (validationError != null) Text(validationError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = parsed?.let { runCatching {
                        it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    }.getOrNull() }
                    when {
                        minor == null || minor <= 0L -> validationError = "Enter an amount greater than zero."
                        minor > record.currentMinor -> validationError = "You cannot withdraw more than the amount saved."
                        else -> {
                            onWithdraw(record, minor, note)
                            withdrawalRecord = null
                        }
                    }
                }) { Text("Record withdrawal") }
            },
            dismissButton = { TextButton(onClick = { withdrawalRecord = null }) { Text("Cancel") } }
        )
    }

    progressRecord?.let { record ->
        var amount by remember(record.id) { mutableStateOf("") }
        var note by remember(record.id) { mutableStateOf("") }
        var validationError by remember(record.id) { mutableStateOf<String?>(null) }
        val remaining = (record.targetMinor - record.currentMinor).coerceAtLeast(0L)
        val isDebt = record.type == FinancialRecord.DEBT_I_OWE || record.type == FinancialRecord.DEBT_OWED_TO_ME
        AlertDialog(
            onDismissRequest = { progressRecord = null },
            title = { Text(when (record.type) {
                FinancialRecord.DEBT_OWED_TO_ME -> "Record a collection"
                FinancialRecord.DEBT_I_OWE -> "Record a repayment"
                FinancialRecord.SAVINGS_GOAL -> "Add to your savings"
                else -> "Record allowance spending"
            }, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        when (record.type) {
                            FinancialRecord.DEBT_OWED_TO_ME -> "${financeKes(remaining)} still to collect"
                            FinancialRecord.DEBT_I_OWE -> "${financeKes(remaining)} still to repay"
                            FinancialRecord.SAVINGS_GOAL -> "${financeKes(record.currentMinor)} saved of ${financeKes(record.targetMinor)}"
                            else -> "${financeKes(remaining)} remaining in this allowance"
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(value = amount, onValueChange = { amount = it; validationError = null },
                        label = { Text("Amount (KSh)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = note, onValueChange = { note = it },
                        label = { Text("Note (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    if (validationError != null) {
                        Text(validationError!!, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = parsed?.let { runCatching {
                        it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    }.getOrNull() }
                    when {
                        minor == null || minor <= 0L -> validationError = "Enter an amount greater than zero."
                        isDebt && minor > remaining -> validationError = "This is more than the outstanding debt."
                        else -> {
                            onAddProgress(record, minor, note)
                            progressRecord = null
                        }
                    }
                }) { Text("Save update") }
            },
            dismissButton = { TextButton(onClick = { progressRecord = null }) { Text("Cancel") } }
        )
    }

    editRecord?.let { record ->
        var title by remember(record.id) { mutableStateOf(record.title) }
        var target by remember(record.id) { mutableStateOf((record.targetMinor / 100.0).toString().removeSuffix(".0")) }
        var savingsCategory by remember(record.id) { mutableStateOf(record.savingsCategory) }
        var contributionMethod by remember(record.id) { mutableStateOf(record.contributionMethod) }
        var plannedContribution by remember(record.id) { mutableStateOf((record.plannedContributionMinor / 100.0).toString().removeSuffix(".0")) }
        var notes by remember(record.id) { mutableStateOf(record.notes) }
        var dueDate by remember(record.id) {
            mutableStateOf(record.dueDateEpochMillis?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(it))
            } ?: "")
        }
        var validationError by remember(record.id) { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { editRecord = null },
            title = { Text("Edit plan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it; validationError = null },
                        label = { Text("Name / description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = target, onValueChange = { target = it; validationError = null },
                        label = { Text("Target / original amount (KSh)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (record.type == FinancialRecord.SAVINGS_GOAL) {
                        OutlinedTextField(value = savingsCategory, onValueChange = { savingsCategory = it },
                            label = { Text("Savings category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Contribution plan", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_WEEKLY },
                                label = { Text("Weekly") })
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_MONTHLY },
                                label = { Text("Monthly") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_FLEXIBLE,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_FLEXIBLE },
                                label = { Text("Flexible") })
                            FilterChip(selected = contributionMethod == FinancialRecord.CONTRIBUTION_ONE_TIME,
                                onClick = { contributionMethod = FinancialRecord.CONTRIBUTION_ONE_TIME },
                                label = { Text("One-time") })
                        }
                        if (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                            contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) {
                            OutlinedTextField(value = plannedContribution,
                                onValueChange = { plannedContribution = it; validationError = null },
                                label = { Text("${if (contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) "Monthly" else "Weekly"} contribution (KSh)") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (record.type != FinancialRecord.ALLOWANCE) {
                        OutlinedTextField(value = dueDate, onValueChange = { dueDate = it; validationError = null },
                            label = { Text("Due date (YYYY-MM-DD, optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    if (validationError != null) Text(validationError!!, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedAmount = target.trim().replace(",", "").toBigDecimalOrNull()
                    val targetMinor = parsedAmount?.let { runCatching {
                        it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact()
                    }.getOrNull() }
                    val parsedDate = if (dueDate.isBlank()) null else runCatching {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                            .parse(dueDate.trim())?.time
                    }.getOrNull()
                    val parsedContribution = if (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                        contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) {
                        plannedContribution.trim().replace(",", "").toBigDecimalOrNull()?.let { value ->
                            runCatching { value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull()
                        }
                    } else 0L
                    when {
                        title.isBlank() -> validationError = "Enter a name or description."
                        targetMinor == null || targetMinor <= 0L -> validationError = "Enter an amount greater than zero."
                        dueDate.isNotBlank() && parsedDate == null && record.type != FinancialRecord.ALLOWANCE ->
                            validationError = "Use a valid date in YYYY-MM-DD format."
                        record.type in setOf(FinancialRecord.DEBT_I_OWE, FinancialRecord.DEBT_OWED_TO_ME) &&
                            targetMinor < record.currentMinor -> validationError = "The original amount cannot be less than what has already been recorded."
                        record.type == FinancialRecord.SAVINGS_GOAL && savingsCategory.isBlank() ->
                            validationError = "Enter a savings category."
                        record.type == FinancialRecord.SAVINGS_GOAL &&
                            (contributionMethod == FinancialRecord.CONTRIBUTION_WEEKLY ||
                                contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) &&
                            (parsedContribution == null || parsedContribution <= 0L) ->
                            validationError = "Enter a valid contribution amount."
                        else -> {
                            onEdit(record.copy(
                                title = title.trim(),
                                targetMinor = targetMinor,
                                dueDateEpochMillis = if (record.type == FinancialRecord.ALLOWANCE) null else parsedDate,
                                notes = notes.trim(),
                                savingsCategory = if (record.type == FinancialRecord.SAVINGS_GOAL) savingsCategory.trim() else record.savingsCategory,
                                contributionMethod = if (record.type == FinancialRecord.SAVINGS_GOAL) contributionMethod else record.contributionMethod,
                                plannedContributionMinor = if (record.type == FinancialRecord.SAVINGS_GOAL) parsedContribution ?: 0L else record.plannedContributionMinor
                            ))
                            editRecord = null
                        }
                    }
                }) { Text("Save changes") }
            },
            dismissButton = { TextButton(onClick = { editRecord = null }) { Text("Cancel") } }
        )
    }

    historyRecord?.let { record ->
        val recordEntries = entries.filter { it.financialRecordId == record.id }
        val linkedExpenses = if (record.type == FinancialRecord.ALLOWANCE) transactions
            .filter { it.type == MoneyTransaction.EXPENSE && it.allowanceRecordId == record.id }
            .sortedByDescending { it.dateEpochMillis } else emptyList()
        AlertDialog(
            onDismissRequest = { historyRecord = null },
            title = { Text("Activity · ${record.title}", fontWeight = FontWeight.Bold) },
            text = {
                if (recordEntries.isEmpty() && linkedExpenses.isEmpty()) {
                    Text("No activity recorded yet. Updates you make from now on will appear here.")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recordEntries, key = { "entry-${it.id}" }) { entry ->
                            Column(
                                Modifier.fillMaxWidth().background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp)
                                ).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        when (entry.entryType) {
                                            FinancialRecordEntry.REPAYMENT -> "Repayment"
                                            FinancialRecordEntry.COLLECTION -> "Collection"
                                            FinancialRecordEntry.CONTRIBUTION -> "Savings contribution"
                                            FinancialRecordEntry.WITHDRAWAL -> "Savings withdrawal"
                                            else -> "Allowance spending"
                                        },
                                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold
                                    )
                                    Text(financeKes(entry.amountMinor), style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())
                                        .format(Date(entry.createdAtEpochMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        items(linkedExpenses, key = { "transaction-${it.id}" }) { transaction ->
                            Column(
                                Modifier.fillMaxWidth().background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(14.dp)
                                ).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Linked expense · ${transaction.category}", style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold)
                                    Text(financeKes(transaction.amountMinor), style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault())
                                        .format(Date(transaction.dateEpochMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (transaction.description.isNotBlank()) {
                                    Text(transaction.description, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyRecord = null }) { Text("Close") } }
        )
    }

    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            title = { Text("Delete this plan?") },
            text = { Text("“${record.title}” and its saved progress will be removed from this device.") },
            confirmButton = {
                Button(onClick = { onDelete(record.id); deleteRecord = null }) { Text("Delete plan") }
            },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("Keep it") } }
        )
    }
}

@Composable
private fun FinanceTab(label: String, symbol: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(symbol, fontSize = 21.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun HeroMetric(modifier: Modifier = Modifier, label: String, value: String, hint: String, symbol: String) {
    Column(
        modifier.background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(symbol, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(hint, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
    }
}

@Composable
private fun FinancialRecordCard(
    record: FinancialRecord,
    onProgress: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (record.targetMinor > 0L) (record.currentMinor.toFloat() / record.targetMinor).coerceIn(0f, 1f) else 0f
    val remaining = (record.targetMinor - record.currentMinor).coerceAtLeast(0L)
    val isAllowance = record.type == FinancialRecord.ALLOWANCE
    val isDebt = record.type == FinancialRecord.DEBT_OWED_TO_ME || record.type == FinancialRecord.DEBT_I_OWE
    val overAllowance = isAllowance && record.currentMinor > record.targetMinor
    val isComplete = remaining == 0L && !overAllowance
    val accent = when {
        overAllowance -> MaterialTheme.colorScheme.error
        isComplete -> MaterialTheme.colorScheme.primary
        record.type == FinancialRecord.SAVINGS_GOAL -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        when (record.type) {
                            FinancialRecord.DEBT_OWED_TO_ME -> "↙"
                            FinancialRecord.DEBT_I_OWE -> "↗"
                            FinancialRecord.SAVINGS_GOAL -> "✦"
                            else -> "◷"
                        },
                        color = accent, fontSize = 25.sp
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when (record.type) {
                            FinancialRecord.DEBT_OWED_TO_ME -> "MONEY OWED TO YOU"
                            FinancialRecord.DEBT_I_OWE -> "YOUR DEBT TO REPAY"
                            FinancialRecord.SAVINGS_GOAL -> record.savingsCategory.uppercase()
                            else -> "ALLOWANCE"
                        },
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("Edit")
                    }
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                        Text("Delete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when (record.type) {
                            FinancialRecord.SAVINGS_GOAL -> "SAVED SO FAR"
                            FinancialRecord.DEBT_OWED_TO_ME -> "COLLECTED"
                            FinancialRecord.DEBT_I_OWE -> "REPAID"
                            else -> "SPENT"
                        },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(financeKes(record.currentMinor), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when {
                            record.type == FinancialRecord.SAVINGS_GOAL -> "GOAL"
                            isDebt -> "ORIGINAL"
                            else -> "LIMIT"
                        },
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(financeKes(record.targetMinor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        overAllowance -> "${financeKes(record.currentMinor - record.targetMinor)} over limit"
                        isAllowance && remaining == 0L -> "Limit reached"
                        isDebt && remaining == 0L -> "Fully settled"
                        isDebt && record.type == FinancialRecord.DEBT_OWED_TO_ME -> "${financeKes(remaining)} left to collect"
                        isDebt -> "${financeKes(remaining)} left to repay"
                        record.type == FinancialRecord.SAVINGS_GOAL && remaining == 0L -> "Goal reached — well done!"
                        record.type == FinancialRecord.SAVINGS_GOAL -> "${financeKes(remaining)} left to reach goal"
                        else -> "${financeKes(remaining)} available"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overAllowance) MaterialTheme.colorScheme.error else if (isComplete) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isComplete || overAllowance) FontWeight.SemiBold else FontWeight.Normal
                )
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold, color = accent)
            }

            if (record.dueDateEpochMillis != null) {
                val dueDateText = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    .format(Date(record.dueDateEpochMillis))
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val overdue = record.dueDateEpochMillis < todayStart && remaining > 0L &&
                    (isDebt || record.type == FinancialRecord.SAVINGS_GOAL)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (overdue) "!" else "◷",
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (overdue) "Overdue · $dueDateText" else "Target date · $dueDateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            if (record.type == FinancialRecord.SAVINGS_GOAL &&
                record.contributionMethod in setOf(FinancialRecord.CONTRIBUTION_WEEKLY, FinancialRecord.CONTRIBUTION_MONTHLY)) {
                Text(
                    "${if (record.contributionMethod == FinancialRecord.CONTRIBUTION_MONTHLY) "Monthly" else "Weekly"} plan · ${financeKes(record.plannedContributionMinor)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (record.notes.isNotBlank()) {
                Text(record.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) { Text("View activity history") }

            if (record.type == FinancialRecord.SAVINGS_GOAL && record.currentMinor > 0L) {
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) { Text("Withdraw savings") }
            }

            if (!isAllowance) {
                Button(
                    onClick = onProgress,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDebt || remaining > 0L,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(when (record.type) {
                        FinancialRecord.DEBT_OWED_TO_ME -> if (remaining == 0L) "Debt collected" else "Record collection"
                        FinancialRecord.DEBT_I_OWE -> if (remaining == 0L) "Debt settled" else "Record repayment"
                        FinancialRecord.SAVINGS_GOAL -> if (remaining == 0L) "Add more savings" else "Add contribution"
                        else -> "Record spending"
                    })
                }
            }
        }
    }
}
