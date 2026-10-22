package com.inventsolutions.trackit.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inventsolutions.trackit.data.local.MoneyTransaction
import com.inventsolutions.trackit.data.local.FinancialRecord
import com.inventsolutions.trackit.ui.theme.TrackItAmber
import com.inventsolutions.trackit.ui.theme.TrackItAmberTint
import com.inventsolutions.trackit.ui.theme.TrackItBackgroundDark
import com.inventsolutions.trackit.ui.theme.TrackItEmerald
import com.inventsolutions.trackit.ui.theme.TrackItEmeraldTint
import com.inventsolutions.trackit.ui.theme.TrackItExpense
import com.inventsolutions.trackit.ui.theme.TrackItExpenseTintLight
import com.inventsolutions.trackit.ui.theme.TrackItNavy
import com.inventsolutions.trackit.ui.theme.TrackItNavyElevated
import com.inventsolutions.trackit.ui.theme.TrackItNavySurface
import com.inventsolutions.trackit.ui.theme.TrackItTheme
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

import com.inventsolutions.trackit.ui.screens.budgets.BudgetScreen
import com.inventsolutions.trackit.ui.screens.transactions.TransactionEntryScreen
import com.inventsolutions.trackit.ui.screens.transactions.TransactionHistoryScreen
import com.inventsolutions.trackit.ui.screens.reports.ReportScreen
import com.inventsolutions.trackit.ui.screens.finances.FinancialPlansScreen
import com.inventsolutions.trackit.ui.viewmodel.TransactionViewModel

private data class ActivityItem(
    val title: String,
    val category: String,
    val amount: String,
    val income: Boolean
)

private fun formatKes(amountMinor: Long): String {
    val absoluteAmount = abs(amountMinor)
    val wholeAmount = NumberFormat.getIntegerInstance(Locale("en", "KE"))
        .format(absoluteAmount / 100L)
    val cents = absoluteAmount % 100L
    val sign = if (amountMinor < 0) "− " else ""
    return if (cents == 0L) {
        "KSh $sign$wholeAmount"
    } else {
        "KSh $sign$wholeAmount.${cents.toString().padStart(2, '0')}"
    }
}

private fun formatTransactionDate(epochMillis: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMillis))

@Composable
fun DashboardScreen(viewModel: TransactionViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf("Home") }
    var entryType by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf(false) }
    var noticeMessage by remember { mutableStateOf("") }

    val now = remember { Calendar.getInstance() }
    val monthStart = remember {
        (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val nextMonthStart = remember {
        (now.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthTransactions = uiState.transactions.filter {
        it.dateEpochMillis >= monthStart && it.dateEpochMillis < nextMonthStart
    }
    val monthFinancialEntries = uiState.financialEntries.filter {
        it.createdAtEpochMillis >= monthStart && it.createdAtEpochMillis < nextMonthStart
    }
    val allIncome = uiState.transactions
        .filter { it.type == MoneyTransaction.INCOME }
        .sumOf { it.amountMinor }
    val linkedAllowanceSpending = uiState.transactions
        .filter { it.type == MoneyTransaction.EXPENSE && it.allowanceRecordId != null }
        .sumOf { it.amountMinor }
    val allRecordedExpenses = uiState.transactions
        .filter { it.type == MoneyTransaction.EXPENSE }
        .sumOf { it.amountMinor } +
        (uiState.financialRecords
            .filter { it.type == FinancialRecord.ALLOWANCE }
            .sumOf { it.currentMinor } - linkedAllowanceSpending).coerceAtLeast(0L)
    val savingsGoals = uiState.financialRecords
        .filter { it.type == FinancialRecord.SAVINGS_GOAL }
        .sortedByDescending { it.currentMinor }
    val savingsTargetTotal = savingsGoals.sumOf { it.targetMinor }
    val savingsContributions = savingsGoals.sumOf { it.currentMinor }
    val savingsGoalCount = savingsGoals.size
    val debtRepayments = uiState.financialRecords
        .filter { it.type == FinancialRecord.DEBT_I_OWE }
        .sumOf { it.currentMinor }
    val debtCollections = uiState.financialRecords
        .filter { it.type == FinancialRecord.DEBT_OWED_TO_ME }
        .sumOf { it.currentMinor }
    val availableBalance = allIncome - allRecordedExpenses - savingsContributions - debtRepayments + debtCollections
    val todayStart = remember {
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val tomorrowStart = remember {
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
    val todayTransactions = uiState.transactions.filter {
        it.type == MoneyTransaction.EXPENSE &&
            it.dateEpochMillis >= todayStart && it.dateEpochMillis < tomorrowStart
    }
    val todaySpent = todayTransactions.sumOf { it.amountMinor }
    val todayIncome = uiState.transactions
        .filter {
            it.type == MoneyTransaction.INCOME &&
                it.dateEpochMillis >= todayStart && it.dateEpochMillis < tomorrowStart
        }
        .sumOf { it.amountMinor }

    val weeklyCashFlow = (6 downTo 0).map { daysAgo ->
        val dayStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_MONTH, -daysAgo)
        }
        val start = dayStart.timeInMillis
        val end = (dayStart.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val dayTransactions = uiState.transactions.filter {
            it.dateEpochMillis >= start && it.dateEpochMillis < end
        }
        WeeklyCashFlow(
            label = SimpleDateFormat("EE", Locale.getDefault()).format(Date(start)),
            income = dayTransactions.filter { it.type == MoneyTransaction.INCOME }.sumOf { it.amountMinor },
            spending = dayTransactions.filter { it.type == MoneyTransaction.EXPENSE }.sumOf { it.amountMinor }
        )
    }

    val monthlyIncome = monthTransactions
        .filter { it.type == MoneyTransaction.INCOME }
        .sumOf { it.amountMinor }
    val monthlySpent = monthTransactions
        .filter { it.type == MoneyTransaction.EXPENSE }
        .sumOf { it.amountMinor } +
        monthFinancialEntries
            .filter { it.entryType == com.inventsolutions.trackit.data.local.FinancialRecordEntry.SPENDING }
            .sumOf { it.amountMinor }
    val monthlyRemaining = monthlyIncome - monthlySpent -
        monthFinancialEntries
            .filter {
                it.entryType == com.inventsolutions.trackit.data.local.FinancialRecordEntry.CONTRIBUTION ||
                    it.entryType == com.inventsolutions.trackit.data.local.FinancialRecordEntry.REPAYMENT
            }
            .sumOf { it.amountMinor } +
        monthFinancialEntries
            .filter {
                it.entryType == com.inventsolutions.trackit.data.local.FinancialRecordEntry.COLLECTION ||
                    it.entryType == com.inventsolutions.trackit.data.local.FinancialRecordEntry.WITHDRAWAL
            }
            .sumOf { it.amountMinor }
    val dailyBudgetDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todaySpendByCategory = todayTransactions
        .groupBy { it.category.lowercase(Locale.getDefault()) }
        .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
    val monthSpendByCategory = monthTransactions
        .filter { it.type == MoneyTransaction.EXPENSE }
        .groupBy { it.category.lowercase(Locale.getDefault()) }
        .mapValues { (_, items) -> items.sumOf { it.amountMinor } }
    val budgetAlerts = uiState.budgets.flatMap { budget ->
        val key = budget.category.lowercase(Locale.getDefault())
        val monthlyCategorySpend = monthSpendByCategory[key] ?: 0L
        val dailyCategorySpend = todaySpendByCategory[key] ?: 0L
        val dailyLimit = budget.limitMinor / dailyBudgetDays
        buildList {
            if (monthlyCategorySpend > budget.limitMinor) {
                add("${budget.category}: monthly limit exceeded by ${formatKes(monthlyCategorySpend - budget.limitMinor)}.")
            }
            if (budget.budgetType == com.inventsolutions.trackit.data.local.MonthlyBudget.RECURRING &&
                dailyCategorySpend > dailyLimit
            ) {
                add("${budget.category}: recurring budget exceeded by ${formatKes(dailyCategorySpend - dailyLimit)} today.")
            }
        }
    }
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        .format(Date(now.timeInMillis)).uppercase(Locale.getDefault())
    val recentItems = uiState.transactions.take(3).map { transaction ->
        val income = transaction.type == MoneyTransaction.INCOME
        ActivityItem(
            title = transaction.description.ifBlank { transaction.category },
            category = "${transaction.category} · ${formatTransactionDate(transaction.dateEpochMillis)}",
            amount = (if (income) "+ " else "− ") + formatKes(transaction.amountMinor).removePrefix("KSh "),
            income = income
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (entryType == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 9.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Home", "Activity", "Budgets", "Reports", "Money").forEach { tab ->
                            val selected = selectedTab == tab
                            val activeColor = MaterialTheme.colorScheme.primary
                            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                            Column(
                                modifier = Modifier.weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 2.dp, vertical = 9.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                TrackItNavigationIcon(
                                    destination = tab,
                                    color = if (selected) activeColor else inactiveColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = tab,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) activeColor else inactiveColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (entryType != null) {
                TransactionEntryScreen(
                    type = entryType!!,
                    isSaving = uiState.isSaving,
                    errorMessage = uiState.errorMessage,
                    budgets = uiState.budgets,
                    transactions = uiState.transactions,
                    expenseCategories = uiState.expenseCategories,
                    allowances = uiState.financialRecords.filter { it.type == FinancialRecord.ALLOWANCE },
                    onBack = {
                        entryType = null
                        viewModel.clearError()
                    },
                    onClearError = viewModel::clearError,
                    onCreateCategory = viewModel::createExpenseCategory,
                    onSave = { type, amountMinor, category, description, paymentMethod, dateEpochMillis, allowanceRecordId ->
                        scope.launch {
                            val saved = viewModel.addTransaction(
                                type = type,
                                amountMinor = amountMinor,
                                category = category,
                                description = description,
                                paymentMethod = paymentMethod,
                                dateEpochMillis = dateEpochMillis,
                                allowanceRecordId = allowanceRecordId
                            )
                            if (saved) {
                                entryType = null
                                noticeMessage = "Transaction saved successfully."
                                notice = true
                            }
                        }
                    }
                )
            } else if (selectedTab == "Activity") {
                TransactionHistoryScreen(
                    transactions = uiState.transactions,
                    errorMessage = uiState.errorMessage,
                    onBack = { selectedTab = "Home"; viewModel.clearError() },
                    onUpdate = viewModel::updateTransaction,
                    onDelete = viewModel::deleteTransaction
                )
            } else if (selectedTab == "Budgets") {
                BudgetScreen(
                    monthLabel = monthLabel,
                    budgets = uiState.budgets,
                    spendingByCategory = monthTransactions
                        .filter { it.type == MoneyTransaction.EXPENSE }
                        .groupBy { it.category.lowercase(Locale.getDefault()) }
                        .mapValues { (_, items) -> items.sumOf { it.amountMinor } },
                    spendingTodayByCategory = todayTransactions
                        .groupBy { it.category.lowercase(Locale.getDefault()) }
                        .mapValues { (_, items) -> items.sumOf { it.amountMinor } },
                    onSaveBudget = viewModel::saveBudget,
                    onDeleteBudget = viewModel::deleteBudget,
                    onBackToHome = { selectedTab = "Home" }
                )
            } else if (selectedTab == "Reports") {
                ReportScreen(
                    transactions = uiState.transactions,
                    financialRecords = uiState.financialRecords,
                    financialEntries = uiState.financialEntries,
                    onBackToHome = { selectedTab = "Home" }
                )
            } else if (selectedTab == "Money") {
                FinancialPlansScreen(
                    records = uiState.financialRecords,
                    entries = uiState.financialEntries,
                    transactions = uiState.transactions,
                    errorMessage = uiState.errorMessage,
                    onBackToHome = { selectedTab = "Home" },
                    onAdd = { type, title, targetMinor, dueDateEpochMillis, notes, savingsCategory, contributionMethod, plannedContributionMinor ->
                        viewModel.addFinancialRecord(
                            type, title, targetMinor,
                            dueDateEpochMillis = dueDateEpochMillis,
                            notes = notes,
                            savingsCategory = savingsCategory,
                            contributionMethod = contributionMethod,
                            plannedContributionMinor = plannedContributionMinor
                        )
                    },
                    onAddProgress = viewModel::addFinancialProgress,
                    onWithdraw = viewModel::withdrawSavings,
                    onEdit = viewModel::updateFinancialRecord,
                    onDelete = viewModel::deleteFinancialRecord,
                    onClearError = viewModel::clearError
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                                    in 5..11 -> "Good morning"
                                    in 12..16 -> "Good afternoon"
                                    else -> "Good evening"
                                }
                                Text(greeting, style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text("Your money at a glance", style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(Modifier.height(3.dp))
                                Text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                Modifier.size(46.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                    item {
                        OverviewCard(
                            monthLabel = monthLabel,
                            income = monthlyIncome,
                            spent = monthlySpent,
                            available = availableBalance,
                            monthlyNet = monthlyRemaining
                        )
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DashboardMetricCard(
                                title = "Spent today",
                                value = formatKes(todaySpent),
                                detail = if (todayTransactions.isEmpty()) "No spending yet" else "${todayTransactions.size} transaction${if (todayTransactions.size == 1) "" else "s"}",
                                symbol = "↘",
                                accent = TrackItExpense,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardMetricCard(
                                title = "Income today",
                                value = formatKes(todayIncome),
                                detail = if (todayIncome == 0L) "Nothing recorded yet" else "Money received",
                                symbol = "↗",
                                accent = TrackItEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SectionTitle("Cash flow", "Income vs spending · last 7 days")
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("7 DAYS", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            WeeklyCashFlowCard(weeklyCashFlow)
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedTab = "Money" },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    Modifier.size(48.dp).clip(RoundedCornerShape(15.dp))
                                        .background(TrackItEmeraldTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↗", color = TrackItEmerald, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Savings progress", style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    if (savingsGoals.isEmpty()) {
                                        Text("Set a savings goal and track your progress here.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("${formatKes(savingsContributions)} saved of ${formatKes(savingsTargetTotal)}",
                                            style = MaterialTheme.typography.bodyMedium)
                                        val savingsProgress = if (savingsTargetTotal > 0L)
                                            (savingsContributions.toDouble() / savingsTargetTotal).coerceIn(0.0, 1.0).toFloat()
                                        else 0f
                                        LinearProgressIndicator(
                                            progress = { savingsProgress },
                                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                                            color = TrackItEmerald,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text("$savingsGoalCount goal${if (savingsGoalCount == 1) "" else "s"} · ${(savingsProgress * 100).toInt()}% saved",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Debts & commitments", "Keep track of money owed in both directions")
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                DashboardMetricCard(
                                    title = "You owe",
                                    value = formatKes(debtRepayments),
                                    detail = "Outstanding debts",
                                    symbol = "↗",
                                    accent = TrackItExpense,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardMetricCard(
                                    title = "Owed to you",
                                    value = formatKes(debtCollections),
                                    detail = "Money to collect",
                                    symbol = "↙",
                                    accent = TrackItEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTab = "Money" },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Manage your financial plans", Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Text("→", style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                    if (budgetAlerts.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("⚠", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                                        Text("Budget alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                    budgetAlerts.forEach { alert ->
                                        Text("• $alert", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                    TextButton(onClick = { selectedTab = "Budgets" }) { Text("Review budgets") }
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Quick actions", "What would you like to do?")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickAction("income", "Add income", TrackItEmeraldTint, TrackItEmerald,
                                    Modifier.weight(1f)) {
                                    viewModel.clearError()
                                    entryType = MoneyTransaction.INCOME
                                }
                                QuickAction("expense", "Add expense", TrackItExpenseTintLight, TrackItExpense,
                                    Modifier.weight(1f)) {
                                    viewModel.clearError()
                                    entryType = MoneyTransaction.EXPENSE
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickAction("budget", "Budgets", MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                                    selectedTab = "Budgets"
                                }
                                QuickAction("savings", "Savings & debts", TrackItAmberTint, TrackItAmber,
                                    Modifier.weight(1f)) {
                                    selectedTab = "Money"
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Budget progress", "Your saved monthly category limits")
                            Card(
                                modifier = Modifier.clickable { selectedTab = "Budgets" },
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (uiState.budgets.isEmpty()) {
                                        Text("No budgets set yet", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Set monthly limits for food, transport, rent and other categories.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("Create your first budget  →", color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge)
                                    } else {
                                        val budgetLimit = uiState.budgets.sumOf { it.limitMinor }
                                        val budgetSpent = uiState.budgets.sumOf { budget ->
                                            monthTransactions.filter {
                                                it.type == MoneyTransaction.EXPENSE &&
                                                    it.category.equals(budget.category, ignoreCase = true)
                                            }.sumOf { it.amountMinor }
                                        }
                                        val progress = if (budgetLimit > 0L) {
                                            (budgetSpent.toDouble() / budgetLimit.toDouble()).coerceIn(0.0, 1.0).toFloat()
                                        } else 0f
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text("Spent this month", style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${formatKes(budgetSpent)} of ${formatKes(budgetLimit)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
                                        }
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                            color = if (budgetSpent > budgetLimit) MaterialTheme.colorScheme.error else TrackItEmerald,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            if (budgetLimit >= budgetSpent) "${formatKes(budgetLimit - budgetSpent)} budget remaining"
                                            else "${formatKes(budgetSpent - budgetLimit)} over planned budget",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (budgetSpent > budgetLimit) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text("Manage budgets  →", color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    SectionTitle("Recent activity", "Your latest saved transactions")
                                }
                                TextButton(onClick = { selectedTab = "Activity" }) {
                                    Text("View all  →")
                                }
                            }
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                if (recentItems.isEmpty()) {
                                    Column(Modifier.fillMaxWidth().padding(22.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("No transactions yet", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Add your first income or expense and it will appear here.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column {
                                        recentItems.forEachIndexed { index, item ->
                                            ActivityRow(item)
                                            if (index != recentItems.lastIndex) {
                                                HorizontalDivider(Modifier.padding(start = 68.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (notice) {
                Surface(
                    Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(noticeMessage, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { notice = false }) { Text("OK") }
                    }
                }
            }
        }
    }



@Composable
private fun OverviewCard(
    monthLabel: String,
    income: Long,
    spent: Long,
    available: Long,
    monthlyNet: Long
) {
    val isDark = MaterialTheme.colorScheme.background == TrackItBackgroundDark
    val cardShape = RoundedCornerShape(28.dp)
    val cardBackground = if (isDark) TrackItNavyElevated else TrackItNavy
    val monthPillBackground = if (isDark) TrackItNavySurface else TrackItNavyElevated
    val mutedText = Color(0xFFD0D7E2)

    Column(
        Modifier.fillMaxWidth().clip(cardShape).background(cardBackground)
            .then(if (isDark) Modifier.border(1.dp, TrackItNavySurface, cardShape) else Modifier)
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.10f)), contentAlignment = Alignment.Center) {
                    DashboardGlyph("savings", Color(0xFF6EE7B7), Modifier.size(21.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("MY MONEY", color = Color(0xFFB8C2D1),
                        style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
                    Text("Financial overview", color = mutedText, style = MaterialTheme.typography.labelSmall)
                }
            }
            Surface(color = monthPillBackground, shape = RoundedCornerShape(50)) {
                Text(monthLabel, Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Color(0xFFE5EAF1), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("AVAILABLE MONEY", color = Color(0xFFB8C2D1),
            style = MaterialTheme.typography.labelMedium, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(5.dp))
        Text(formatKes(available), color = Color.White,
            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Surface(color = Color.White.copy(alpha = 0.09f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape)
                    .background(if (monthlyNet >= 0L) Color(0xFF6EE7B7) else Color(0xFFFFA3A3)))
                Text(
                    "This month  ${if (monthlyNet < 0L) "− " else "+ "}${formatKes(kotlin.math.abs(monthlyNet)).removePrefix("KSh ")} net",
                    color = mutedText, style = MaterialTheme.typography.labelMedium
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.16f))
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OverviewStat("INCOME", income, "income", Color(0xFF6EE7B7), Modifier.weight(1f))
            OverviewStat("SPENDING", spent, "expense", Color(0xFFFFB86B), Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewStat(
    title: String,
    amount: Long,
    symbol: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.07f))
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        DashboardGlyph(symbol, accent, Modifier.size(19.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color(0xFFB8C2D1),
                style = MaterialTheme.typography.labelSmall, letterSpacing = 0.5.sp)
            Text(formatKes(amount), color = Color.White,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private data class WeeklyCashFlow(
    val label: String,
    val income: Long,
    val spending: Long
)

@Composable
private fun WeeklyCashFlowCard(days: List<WeeklyCashFlow>) {
    val maxAmount = (days.maxOfOrNull { maxOf(it.income, it.spending) } ?: 0L).coerceAtLeast(1L)
    val totalIncome = days.sumOf { it.income }
    val totalSpending = days.sumOf { it.spending }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Money movement", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text("Compare what came in and went out",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Net", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        (if (totalIncome - totalSpending < 0L) "− " else "+ ") +
                            formatKes(abs(totalIncome - totalSpending)).removePrefix("KSh "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totalIncome >= totalSpending) TrackItEmerald else MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().height(112.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            Modifier.fillMaxWidth().height(82.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(
                                Modifier.width(9.dp).height(
                                    (82f * (day.income.toDouble() / maxAmount.toDouble()).toFloat())
                                        .coerceAtLeast(if (day.income > 0L) 4f else 2f).dp
                                ).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(TrackItEmerald)
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                Modifier.width(9.dp).height(
                                    (82f * (day.spending.toDouble() / maxAmount.toDouble()).toFloat())
                                        .coerceAtLeast(if (day.spending > 0L) 4f else 2f).dp
                                ).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(TrackItAmber)
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(day.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(TrackItEmerald))
                    Text("Income  ${formatKes(totalIncome)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(TrackItAmber))
                    Text("Spent  ${formatKes(totalSpending)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    detail: String,
    symbol: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(symbol, color = accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Text(title, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun QuickAction(
    symbol: String, title: String, tint: Color, accent: Color,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(tint),
                contentAlignment = Alignment.Center) {
                DashboardGlyph(symbol, accent, Modifier.size(23.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("↗", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TrackItNavigationIcon(
    destination: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = size.minDimension * 0.085f
        val w = size.width
        val h = size.height
        val x = w / 2f
        val y = h / 2f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val join = androidx.compose.ui.graphics.StrokeJoin.Round
        when (destination) {
            "Home" -> {
                val roof = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.15f, h * 0.47f)
                    lineTo(x, h * 0.15f)
                    lineTo(w * 0.85f, h * 0.47f)
                }
                drawPath(roof, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = cap, join = join))
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.43f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.43f), androidx.compose.ui.geometry.CornerRadius(stroke), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                drawLine(color, androidx.compose.ui.geometry.Offset(x, h * 0.64f), androidx.compose.ui.geometry.Offset(x, h * 0.86f), strokeWidth = stroke, cap = cap)
            }
            "Activity" -> {
                // Transaction history: a simple clock with clear hands.
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.34f,
                    center = androidx.compose.ui.geometry.Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(x, h * 0.29f),
                    androidx.compose.ui.geometry.Offset(x, y),
                    strokeWidth = stroke,
                    cap = cap
                )
                drawLine(
                    color,
                    androidx.compose.ui.geometry.Offset(x, y),
                    androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.61f),
                    strokeWidth = stroke,
                    cap = cap
                )
            }
            "Budgets" -> {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.14f), androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.72f), androidx.compose.ui.geometry.CornerRadius(stroke * 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                listOf(0.37f, 0.52f, 0.67f).forEach { lineY ->
                    drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.32f, h * lineY), androidx.compose.ui.geometry.Offset(w * 0.68f, h * lineY), strokeWidth = stroke, cap = cap)
                }
            }
            "Reports" -> {
                drawLine(color, androidx.compose.ui.geometry.Offset(w * 0.17f, h * 0.84f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.84f), strokeWidth = stroke, cap = cap)
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.53f), androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.31f), androidx.compose.ui.geometry.CornerRadius(stroke), style = androidx.compose.ui.graphics.drawscope.Fill)
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.44f, h * 0.34f), androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.5f), androidx.compose.ui.geometry.CornerRadius(stroke), style = androidx.compose.ui.graphics.drawscope.Fill)
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.66f, h * 0.17f), androidx.compose.ui.geometry.Size(w * 0.14f, h * 0.67f), androidx.compose.ui.geometry.CornerRadius(stroke), style = androidx.compose.ui.graphics.drawscope.Fill)
            }
            "Money" -> {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.59f), androidx.compose.ui.geometry.CornerRadius(stroke * 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.43f), androidx.compose.ui.geometry.Size(w * 0.33f, h * 0.23f), androidx.compose.ui.geometry.CornerRadius(stroke * 1.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                drawCircle(color, radius = stroke * 0.9f, center = androidx.compose.ui.geometry.Offset(w * 0.67f, h * 0.545f))
            }
        }
    }
}

@Composable
private fun DashboardGlyph(name: String, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val stroke = size.minDimension * 0.075f
        val left = size.width * 0.15f
        val right = size.width * 0.85f
        val top = size.height * 0.30f
        val bottom = size.height * 0.78f
        val midX = size.width / 2f
        val midY = size.height / 2f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val join = androidx.compose.ui.graphics.StrokeJoin.Round

        when (name) {
            "income" -> {
                // Simple upward arrow over a baseline: money coming in.
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, bottom),
                    androidx.compose.ui.geometry.Offset(midX, top), strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, top),
                    androidx.compose.ui.geometry.Offset(size.width * 0.27f, size.height * 0.39f),
                    strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, top),
                    androidx.compose.ui.geometry.Offset(size.width * 0.73f, size.height * 0.39f),
                    strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.20f, bottom),
                    androidx.compose.ui.geometry.Offset(size.width * 0.80f, bottom),
                    strokeWidth = stroke, cap = cap)
            }
            "expense" -> {
                // Simple downward arrow ending at a baseline: money going out.
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, top),
                    androidx.compose.ui.geometry.Offset(midX, bottom), strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, bottom),
                    androidx.compose.ui.geometry.Offset(size.width * 0.27f, size.height * 0.61f),
                    strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, bottom),
                    androidx.compose.ui.geometry.Offset(size.width * 0.73f, size.height * 0.61f),
                    strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.20f, top),
                    androidx.compose.ui.geometry.Offset(size.width * 0.80f, top),
                    strokeWidth = stroke, cap = cap)
            }
            "budget" -> {
                drawRoundRect(color, androidx.compose.ui.geometry.Offset(left, top - size.height * 0.10f), androidx.compose.ui.geometry.Size(right - left, bottom - top + size.height * 0.20f), androidx.compose.ui.geometry.CornerRadius(stroke * 1.8f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                listOf(0.36f, 0.5f, 0.64f).forEach { y ->
                    drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.32f, size.height * y), androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * y), strokeWidth = stroke, cap = cap)
                }
            }
            "savings" -> {
                // My Money: a wallet with a visible coin, not a generic plus sign.
                drawRoundRect(
                    color,
                    androidx.compose.ui.geometry.Offset(size.width * 0.12f, size.height * 0.28f),
                    androidx.compose.ui.geometry.Size(size.width * 0.76f, size.height * 0.53f),
                    androidx.compose.ui.geometry.CornerRadius(stroke * 2.2f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                drawRoundRect(
                    color,
                    androidx.compose.ui.geometry.Offset(size.width * 0.54f, size.height * 0.43f),
                    androidx.compose.ui.geometry.Size(size.width * 0.34f, size.height * 0.20f),
                    androidx.compose.ui.geometry.CornerRadius(stroke * 1.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                drawCircle(color, radius = stroke * 0.85f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.53f))
            }
            else -> {
                drawCircle(color, radius = size.minDimension * 0.34f, center = androidx.compose.ui.geometry.Offset(midX, midY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
                drawLine(color, androidx.compose.ui.geometry.Offset(midX, size.height * 0.29f), androidx.compose.ui.geometry.Offset(midX, size.height * 0.71f), strokeWidth = stroke, cap = cap)
                drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.29f, midY), androidx.compose.ui.geometry.Offset(size.width * 0.71f, midY), strokeWidth = stroke, cap = cap)
            }
        }
    }
}
@Composable
private fun MiniBudget(
    title: String, spent: String, budget: String, progress: Float,
    accent: Color, modifier: Modifier = Modifier
) {
    Card(modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(spent, style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(budget, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = accent, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val tint = if (item.income) TrackItEmeraldTint else TrackItExpenseTintLight
    val accent = if (item.income) TrackItEmerald else TrackItExpense
    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(tint),
            contentAlignment = Alignment.Center) {
            Text(if (item.income) "↗" else "↓", color = accent, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(3.dp))
            Text(item.category, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(item.amount, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (item.income) TrackItEmerald else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "TrackIt Dashboard - Light", showBackground = true)
@Composable
private fun DashboardLightPreview() {
    TrackItTheme(darkTheme = false) {
        OverviewCard(
            monthLabel = "OCTOBER 2026",
            income = 3_200_000,
            spent = 1_358_000,
            available = 1_842_000,
            monthlyNet = 1_842_000
        )
    }
}

@Preview(name = "TrackIt Dashboard - Dark", showBackground = true)
@Composable
private fun DashboardDarkPreview() {
    TrackItTheme(darkTheme = true) {
        OverviewCard(
            monthLabel = "OCTOBER 2026",
            income = 3_200_000,
            spent = 1_358_000,
            available = 1_842_000,
            monthlyNet = 1_842_000
        )
    }
}