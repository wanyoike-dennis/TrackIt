package com.inventsolutions.trackit.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.data.local.FinancialRecordEntry
import com.inventsolutions.trackit.data.local.RecurringTransaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class ReportPeriod { TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME }
private data class ReportRange(val start: Long, val end: Long, val previousStart: Long, val previousEnd: Long)
private data class CategorySpend(val name: String, val amount: Long)

private fun csvCell(value: String): String =
    "\"" + value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""

private fun transactionCsv(
    transactions: List<MoneyTransaction>,
    financialEntries: List<FinancialRecordEntry>,
    financialRecords: List<FinancialRecord>
): String {
    val rows = mutableListOf("Date,Type,Category or plan,Description,Payment method,Amount (KES)")
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    transactions.sortedBy { it.dateEpochMillis }.forEach { transaction ->
        val date = dateFormat.format(Date(transaction.dateEpochMillis))
        val amount = "%.2f".format(Locale.US, transaction.amountMinor / 100.0)
        rows += listOf(
            date, transaction.type, transaction.category, transaction.description,
            transaction.paymentMethod, amount
        ).joinToString(",") { csvCell(it) }
    }
    val planTitles = financialRecords.associate { it.id to it.title }
    financialEntries.sortedBy { it.createdAtEpochMillis }.forEach { entry ->
        val date = dateFormat.format(Date(entry.createdAtEpochMillis))
        val amount = "%.2f".format(Locale.US, entry.amountMinor / 100.0)
        rows += listOf(
            date, entry.entryType, planTitles[entry.financialRecordId] ?: "Financial plan",
            entry.note, "Financial plan", amount
        ).joinToString(",") { csvCell(it) }
    }
    return rows.joinToString("\n")
}

private fun reportKes(minor: Long): String {
    val absolute = kotlin.math.abs(minor)
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (absolute % 100L == 0L) 0 else 2
    }
    return (if (minor < 0L) "− " else "") + "KSh " + formatter.format(absolute / 100.0)
}

private fun reportDayStart(source: Calendar): Calendar = (source.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

private fun rangeFor(
    period: ReportPeriod,
    transactions: List<MoneyTransaction>,
    financialEntries: List<FinancialRecordEntry>,
    nowMillis: Long
): ReportRange {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val todayStart = reportDayStart(now)
    val start = when (period) {
        ReportPeriod.TODAY -> todayStart
        ReportPeriod.THIS_WEEK -> (todayStart.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        ReportPeriod.THIS_MONTH -> (todayStart.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        ReportPeriod.ALL_TIME -> Calendar.getInstance().apply {
            timeInMillis = minOf(
                transactions.minOfOrNull { it.dateEpochMillis } ?: Long.MAX_VALUE,
                financialEntries.minOfOrNull { it.createdAtEpochMillis } ?: Long.MAX_VALUE
            ).takeIf { it != Long.MAX_VALUE } ?: nowMillis
        }.let(::reportDayStart)
    }
    val end = (now.clone() as Calendar).apply { add(Calendar.MILLISECOND, 1) }
    val previousStart = start.clone() as Calendar
    val previousEnd = start.clone() as Calendar
    when (period) {
        ReportPeriod.TODAY -> previousStart.add(Calendar.DAY_OF_MONTH, -1)
        ReportPeriod.THIS_WEEK -> previousStart.add(Calendar.WEEK_OF_YEAR, -1)
        ReportPeriod.THIS_MONTH -> previousStart.add(Calendar.MONTH, -1)
        ReportPeriod.ALL_TIME -> {
            val duration = (nowMillis - start.timeInMillis).coerceAtLeast(1L)
            previousStart.timeInMillis = (start.timeInMillis - duration).coerceAtLeast(0L)
        }
    }
    val elapsedPeriod = (nowMillis - start.timeInMillis).coerceAtLeast(1L)
    previousEnd.timeInMillis = minOf(start.timeInMillis, previousStart.timeInMillis + elapsedPeriod)
    return ReportRange(start.timeInMillis, end.timeInMillis, previousStart.timeInMillis, previousEnd.timeInMillis)
}

@Composable
fun ReportScreen(
    transactions: List<MoneyTransaction>,
    financialRecords: List<FinancialRecord>,
    financialEntries: List<FinancialRecordEntry>,
    recurringTransactions: List<RecurringTransaction>,
    onBackToHome: () -> Unit,
    onAddRecurringTransaction: (String, String, Long, String, String, String, String, Long) -> Unit,
    onSetRecurringTransactionActive: (RecurringTransaction, Boolean) -> Unit,
    onDeleteRecurringTransaction: (Long) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringTitle by remember { mutableStateOf("") }
    var recurringType by remember { mutableStateOf(MoneyTransaction.EXPENSE) }
    var recurringAmount by remember { mutableStateOf("") }
    var recurringCategory by remember { mutableStateOf("") }
    var recurringDescription by remember { mutableStateOf("") }
    var recurringPaymentMethod by remember { mutableStateOf("Cash") }
    var recurringFrequency by remember { mutableStateOf(RecurringTransaction.MONTHLY) }
    var recurringError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val nowMillis = System.currentTimeMillis()
    val range = rangeFor(selectedPeriod, transactions, financialEntries, nowMillis)
    val current = transactions.filter { it.dateEpochMillis >= range.start && it.dateEpochMillis < range.end }
    val previous = transactions.filter { it.dateEpochMillis >= range.previousStart && it.dateEpochMillis < range.previousEnd }
    val currentEntries = financialEntries.filter { it.createdAtEpochMillis >= range.start && it.createdAtEpochMillis < range.end }
    val previousEntries = financialEntries.filter { it.createdAtEpochMillis >= range.previousStart && it.createdAtEpochMillis < range.previousEnd }
    val income = current.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor }
    val expenses = current.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor } +
        currentEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }.sumOf { it.amountMinor }
    val net = income - expenses -
        currentEntries.filter { it.entryType == FinancialRecordEntry.CONTRIBUTION || it.entryType == FinancialRecordEntry.REPAYMENT }.sumOf { it.amountMinor } +
        currentEntries.filter { it.entryType == FinancialRecordEntry.COLLECTION || it.entryType == FinancialRecordEntry.WITHDRAWAL }.sumOf { it.amountMinor }
    val allowancePlans = financialRecords.filter { it.type == FinancialRecord.ALLOWANCE }
    val allowanceLimitTotal = allowancePlans.sumOf { it.targetMinor }
    val allowanceSpentTotal = allowancePlans.sumOf { it.currentMinor }
    val allowanceRemainingTotal = allowancePlans.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
    val previousIncome = previous.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor }
    val previousExpenses = previous.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor } +
        previousEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }.sumOf { it.amountMinor }
    val previousNet = previousIncome - previousExpenses -
        previousEntries.filter { it.entryType == FinancialRecordEntry.CONTRIBUTION || it.entryType == FinancialRecordEntry.REPAYMENT }.sumOf { it.amountMinor } +
        previousEntries.filter { it.entryType == FinancialRecordEntry.COLLECTION || it.entryType == FinancialRecordEntry.WITHDRAWAL }.sumOf { it.amountMinor }
    val allowanceTitles = financialRecords.filter { it.type == FinancialRecord.ALLOWANCE }.associate { it.id to it.title }
    val regularCategorySpending = current.filter { it.type == MoneyTransaction.EXPENSE }
        .groupBy { it.category.trim().ifBlank { "Uncategorized" } }
        .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
    val allowanceCategorySpending = currentEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }
        .groupBy { allowanceTitles[it.financialRecordId] ?: "Allowance spending" }
        .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
    val categorySpending = (regularCategorySpending.keys + allowanceCategorySpending.keys).distinct()
        .map { name -> CategorySpend(name, (regularCategorySpending[name] ?: 0L) + (allowanceCategorySpending[name] ?: 0L)) }
        .sortedByDescending { it.amount }
    val totalCategorySpend = categorySpending.sumOf { it.amount }
    val periodLabel = when (selectedPeriod) {
        ReportPeriod.TODAY -> "Today"; ReportPeriod.THIS_WEEK -> "This week"
        ReportPeriod.THIS_MONTH -> "This month"; ReportPeriod.ALL_TIME -> "All time"
    }
    val dateLabel = when (selectedPeriod) {
        ReportPeriod.TODAY -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(nowMillis))
        ReportPeriod.THIS_WEEK -> "Since " + SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(range.start))
        ReportPeriod.THIS_MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(nowMillis))
        ReportPeriod.ALL_TIME -> if (transactions.isEmpty() && financialEntries.isEmpty()) "No activity yet" else
            SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(
                minOf(
                    transactions.minOfOrNull { it.dateEpochMillis } ?: Long.MAX_VALUE,
                    financialEntries.minOfOrNull { it.createdAtEpochMillis } ?: Long.MAX_VALUE
                ).takeIf { it != Long.MAX_VALUE } ?: nowMillis
            )) + " – " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(nowMillis))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Financial reports", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Understand where your money goes.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBackToHome) { Text("Home") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(ReportPeriod.TODAY, ReportPeriod.THIS_WEEK, ReportPeriod.THIS_MONTH, ReportPeriod.ALL_TIME).forEach { period ->
                    FilterChip(selected = selectedPeriod == period, onClick = { selectedPeriod = period }, label = {
                        Text(when (period) {
                            ReportPeriod.TODAY -> "Day"; ReportPeriod.THIS_WEEK -> "Week"
                            ReportPeriod.THIS_MONTH -> "Month"; ReportPeriod.ALL_TIME -> "All"
                        })
                    })
                }
            }
            Text(dateLabel, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
            OutlinedButton(
                onClick = {
                    val csv = transactionCsv(current, currentEntries, financialRecords)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "TrackIt financial report - $periodLabel")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Export financial report"))
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Export report (CSV)")
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NET BALANCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(reportKes(net), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text("Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                            Text(reportKes(income), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Expenses", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f))
                            Text(reportKes(expenses), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Allowance plans · not extra expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Actual purchases are counted in Expenses above. Allowances are limits used to track those purchases, not separate spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (allowancePlans.isEmpty()) {
                        Text("No allowance plans yet.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        AllowanceSummaryRow("Total allowance limits", allowanceLimitTotal)
                        AllowanceSummaryRow("Used across allowances", allowanceSpentTotal)
                        AllowanceSummaryRow("Remaining allowance", allowanceRemainingTotal)
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Compared with previous period", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(when (selectedPeriod) {
                        ReportPeriod.TODAY -> "Compared with yesterday"
                        ReportPeriod.THIS_WEEK -> "Compared with last week"
                        ReportPeriod.THIS_MONTH -> "Compared with last month"
                        ReportPeriod.ALL_TIME -> "Compared with the preceding period of equal length"
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ReportComparisonRow("Income", income, previousIncome)
                    ReportComparisonRow("Expenses", expenses, previousExpenses)
                    ReportComparisonRow("Net balance", net, previousNet)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Spending by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$periodLabel expense breakdown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(reportKes(totalCategorySpend), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        if (categorySpending.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("No expenses for this period", style = MaterialTheme.typography.titleMedium)
                        Text("Expenses saved on this device will appear here, even when you're offline.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(categorySpending, key = { it.name }) { category ->
                val share = if (totalCategorySpend > 0L) category.amount.toFloat() / totalCategorySpend else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(reportKes(category.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Text("${(share * 100).toInt()}% of expenses", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Text("Reports use transactions stored locally on this device. No internet connection is required.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Recurring transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Automatically record regular income or expenses when the app is opened.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { recurringError = null; showRecurringDialog = true }) { Text("Add") }
                    }
                    if (recurringTransactions.isEmpty()) {
                        Text("No recurring transactions set up yet.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        recurringTransactions.forEach { item ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${item.frequency.lowercase().replaceFirstChar { it.uppercase() }} · ${item.category} · ${reportKes(item.amountMinor)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        if (item.active) "Next: " + SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(item.nextRunEpochMillis)) else "Paused",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(onClick = { onSetRecurringTransactionActive(item, !item.active) }) {
                                        Text(if (item.active) "Pause" else "Resume")
                                    }
                                    TextButton(onClick = { onDeleteRecurringTransaction(item.id) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                    Text("A missed schedule creates one catch-up entry, then advances to the next future date. Check the transaction history after opening the app.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

    }

    if (showRecurringDialog) {
        AlertDialog(
            onDismissRequest = { showRecurringDialog = false },
            title = { Text("New recurring transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("First occurrence is recorded now. Later occurrences are created when you open TrackIt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = recurringTitle, onValueChange = { recurringTitle = it }, label = { Text("Name (e.g. Rent)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = recurringType == MoneyTransaction.EXPENSE, onClick = { recurringType = MoneyTransaction.EXPENSE }, label = { Text("Expense") })
                        FilterChip(selected = recurringType == MoneyTransaction.INCOME, onClick = { recurringType = MoneyTransaction.INCOME }, label = { Text("Income") })
                    }
                    OutlinedTextField(value = recurringAmount, onValueChange = { recurringAmount = it }, label = { Text("Amount (KES)") }, singleLine = true)
                    OutlinedTextField(value = recurringCategory, onValueChange = { recurringCategory = it }, label = { Text("Category") }, singleLine = true)
                    OutlinedTextField(value = recurringDescription, onValueChange = { recurringDescription = it }, label = { Text("Description (optional)") }, singleLine = true)
                    OutlinedTextField(value = recurringPaymentMethod, onValueChange = { recurringPaymentMethod = it }, label = { Text("Payment method") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = recurringFrequency == RecurringTransaction.WEEKLY, onClick = { recurringFrequency = RecurringTransaction.WEEKLY }, label = { Text("Weekly") })
                        FilterChip(selected = recurringFrequency == RecurringTransaction.MONTHLY, onClick = { recurringFrequency = RecurringTransaction.MONTHLY }, label = { Text("Monthly") })
                    }
                    if (recurringError != null) Text(recurringError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                OutlinedButton(onClick = {
                    val amount = recurringAmount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = runCatching { amount?.movePointRight(2)?.longValueExact() }.getOrNull()
                    if (recurringTitle.isBlank() || minor == null || minor <= 0L || recurringCategory.isBlank() || recurringPaymentMethod.isBlank()) {
                        recurringError = "Enter a name, valid amount, category and payment method."
                    } else {
                        onAddRecurringTransaction(
                            recurringTitle, recurringType, minor, recurringCategory, recurringDescription,
                            recurringPaymentMethod, recurringFrequency, System.currentTimeMillis()
                        )
                        showRecurringDialog = false
                        recurringTitle = ""; recurringAmount = ""; recurringCategory = ""; recurringDescription = ""
                        recurringPaymentMethod = "Cash"; recurringError = null
                    }
                }) { Text("Save schedule") }
            },
            dismissButton = {
                TextButton(onClick = { showRecurringDialog = false }) { Text("Cancel") }
            }
        )
    }

}

@Composable
private fun AllowanceSummaryRow(label: String, value: Long) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(reportKes(value), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ReportComparisonRow(label: String, current: Long, previous: Long) {
    val difference = current - previous
    val direction = when {
        difference > 0L -> "↑ " + reportKes(difference)
        difference < 0L -> "↓ " + reportKes(-difference)
        else -> "No change"
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("Previous: " + reportKes(previous), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(direction, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = when {
                difference == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                label == "Expenses" && difference < 0L -> MaterialTheme.colorScheme.primary
                label == "Expenses" && difference > 0L -> MaterialTheme.colorScheme.error
                difference > 0L -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            })
    }

}
