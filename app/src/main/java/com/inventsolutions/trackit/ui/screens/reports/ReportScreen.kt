package com.inventsolutions.trackit.ui.screens.reports

import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.ui.components.TrackItIcon
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
import com.inventsolutions.trackit.data.local.FinancialAccount
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
        if (financialRecords.firstOrNull { it.id == entry.financialRecordId }?.type == FinancialRecord.CLIENT_RECEIVABLE) return@forEach
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
    accounts: List<FinancialAccount>,
    onBackToHome: () -> Unit,
    onAddRecurringTransaction: (String, String, Long, String, String, String, String, Long, Long?) -> Unit,
    onSetRecurringTransactionActive: (RecurringTransaction, Boolean) -> Unit,
    onDeleteRecurringTransaction: (Long) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.THIS_MONTH) }
    val context = LocalContext.current
    var pendingCsv by remember { mutableStateOf("") }
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(pendingCsv) }
        }
    }
    val nowMillis = System.currentTimeMillis()
    val range = rangeFor(selectedPeriod, transactions, financialEntries, nowMillis)
    val current = transactions.filter { it.dateEpochMillis >= range.start && it.dateEpochMillis < range.end }
    val previous = transactions.filter { it.dateEpochMillis >= range.previousStart && it.dateEpochMillis < range.previousEnd }
    val currentEntries = financialEntries.filter { it.createdAtEpochMillis >= range.start && it.createdAtEpochMillis < range.end }
    val previousEntries = financialEntries.filter { it.createdAtEpochMillis >= range.previousStart && it.createdAtEpochMillis < range.previousEnd }
    val income = current.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor }
    val expenses = current.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor } +
        currentEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }.sumOf { it.amountMinor }
    val currentCashEntries = currentEntries.filterNot {
        it.entryType == FinancialRecordEntry.COLLECTION &&
            financialRecords.firstOrNull { record -> record.id == it.financialRecordId }?.type == FinancialRecord.CLIENT_RECEIVABLE
    }
    val net = income - expenses -
        currentCashEntries.filter { it.entryType == FinancialRecordEntry.CONTRIBUTION || it.entryType == FinancialRecordEntry.REPAYMENT }.sumOf { it.amountMinor } +
        currentCashEntries.filter { it.entryType == FinancialRecordEntry.COLLECTION || it.entryType == FinancialRecordEntry.WITHDRAWAL }.sumOf { it.amountMinor }
    val allowancePlans = financialRecords.filter { it.type == FinancialRecord.ALLOWANCE }
    val allowanceLimitTotal = allowancePlans.sumOf { it.targetMinor }
    val allowanceSpentTotal = allowancePlans.sumOf { it.currentMinor }
    val allowanceRemainingTotal = allowancePlans.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
    val previousIncome = previous.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor }
    val previousExpenses = previous.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor } +
        previousEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }.sumOf { it.amountMinor }
    val previousCashEntries = previousEntries.filterNot {
        it.entryType == FinancialRecordEntry.COLLECTION &&
            financialRecords.firstOrNull { record -> record.id == it.financialRecordId }?.type == FinancialRecord.CLIENT_RECEIVABLE
    }
    val previousNet = previousIncome - previousExpenses -
        previousCashEntries.filter { it.entryType == FinancialRecordEntry.CONTRIBUTION || it.entryType == FinancialRecordEntry.REPAYMENT }.sumOf { it.amountMinor } +
        previousCashEntries.filter { it.entryType == FinancialRecordEntry.COLLECTION || it.entryType == FinancialRecordEntry.WITHDRAWAL }.sumOf { it.amountMinor }
    val monthTrends = (5 downTo 0).map { offset ->
        val start = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -offset)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
        val monthTransactions = transactions.filter { it.dateEpochMillis >= start.timeInMillis && it.dateEpochMillis < end }
        val monthEntries = financialEntries.filter { it.createdAtEpochMillis >= start.timeInMillis && it.createdAtEpochMillis < end }
        Triple(
            SimpleDateFormat("MMM", Locale.getDefault()).format(Date(start.timeInMillis)),
            monthTransactions.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor },
            monthTransactions.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor } +
                monthEntries.filter { it.entryType == FinancialRecordEntry.SPENDING }.sumOf { it.amountMinor }
        )
    }
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Reports", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Financial performance at a glance.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("REPORTING PERIOD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(ReportPeriod.TODAY, ReportPeriod.THIS_WEEK, ReportPeriod.THIS_MONTH, ReportPeriod.ALL_TIME).forEach { period ->
                            FilterChip(modifier = Modifier.weight(1f), selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period }, label = { Text(when(period) {
                                    ReportPeriod.TODAY -> "Day"; ReportPeriod.THIS_WEEK -> "Week"
                                    ReportPeriod.THIS_MONTH -> "Month"; ReportPeriod.ALL_TIME -> "All"
                                }) })
                        }
                    }
                    Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Financial overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NET CASH MOVEMENT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(reportKes(net), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Income less expenses, savings and debt outflows, plus collections and withdrawals.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("Income", color = MaterialTheme.colorScheme.onPrimaryContainer); Text(reportKes(income), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Column { Text("Expenses", color = MaterialTheme.colorScheme.onPrimaryContainer); Text(reportKes(expenses), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    }
                }
            }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Spending analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Category breakdown · $periodLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(reportKes(totalCategorySpend), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (categorySpending.isEmpty()) {
            item { Text("No spending recorded for this period.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(categorySpending, key = { it.name }) { category ->
                val share = if (totalCategorySpend > 0L) category.amount.toFloat() / totalCategorySpend else 0f
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text(reportKes(category.amount), fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Text("${(share * 100).toInt()}% of spending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Income & expense trends", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Monthly activity over the last six months.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val maxValue = monthTrends.maxOfOrNull { maxOf(it.second, it.third) }?.coerceAtLeast(1L) ?: 1L
                    monthTrends.forEach { month ->
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(month.first, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ReportTrendBar("Income", month.second, maxValue, MaterialTheme.colorScheme.primary)
                            ReportTrendBar("Expenses", month.third, maxValue, MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Period comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(when(selectedPeriod) {
                        ReportPeriod.TODAY -> "Compared with yesterday"; ReportPeriod.THIS_WEEK -> "Compared with last week"
                        ReportPeriod.THIS_MONTH -> "Compared with last month"; ReportPeriod.ALL_TIME -> "Compared with the preceding period"
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ReportComparisonRow("Income", income, previousIncome)
                    ReportComparisonRow("Expenses", expenses, previousExpenses)
                    ReportComparisonRow("Net cash movement", net, previousNet)
                }
            }
            }
        }
        if (allowancePlans.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Allowance overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Planning limits, not additional expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AllowanceSummaryRow("Total limits", allowanceLimitTotal)
                        AllowanceSummaryRow("Used", allowanceSpentTotal)
                        AllowanceSummaryRow("Remaining", allowanceRemainingTotal)
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Export report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Save the selected period as a CSV file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = {
                        pendingCsv = transactionCsv(current, currentEntries, financialRecords)
                        exportLauncher.launch("TrackIt_Report_${periodLabel.replace(" ", "_")}.csv")
                    }, modifier = Modifier.fillMaxWidth()) {
                        TrackItIcon(R.drawable.ic_lucide_download, null); Text("Save CSV report")
                    }
                }
            }
        }
        item {
            Text("Reports use information stored on this device and work offline.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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

@Composable
private fun ReportTrendBar(label: String, amount: Long, maximum: Long, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, Modifier.weight(0.75f), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(progress = { (amount.toFloat() / maximum.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.weight(2f).height(7.dp), color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant)
        Text(reportKes(amount), Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
