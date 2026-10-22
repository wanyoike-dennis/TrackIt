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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val monthlyIncome = monthTransactions
        .filter { it.type == MoneyTransaction.INCOME }
        .sumOf { it.amountMinor }
    val monthlySpent = monthTransactions
        .filter { it.type == MoneyTransaction.EXPENSE }
        .sumOf { it.amountMinor }
    val monthlyRemaining = monthlyIncome - monthlySpent
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
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("Home", "Activity", "Budgets", "Reports", "Money").forEach { tab ->
                            Column(
                                modifier = Modifier.clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    when (tab) {
                                        "Home" -> "⌂"
                                        "Activity" -> "↕"
                                        "Budgets" -> "▤"
                                        "Reports" -> "▥"
                                        else -> "$"
                                    },
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 21.sp
                                )
                                Text(
                                    tab,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedTab == tab) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
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
                    onBack = {
                        entryType = null
                        viewModel.clearError()
                    },
                    onClearError = viewModel::clearError,
                    onCreateCategory = viewModel::createExpenseCategory,
                    onSave = { type, amountMinor, category, description, paymentMethod, dateEpochMillis ->
                        scope.launch {
                            val saved = viewModel.addTransaction(
                                type = type,
                                amountMinor = amountMinor,
                                category = category,
                                description = description,
                                paymentMethod = paymentMethod,
                                dateEpochMillis = dateEpochMillis
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
                    onBackToHome = { selectedTab = "Home" }
                )
            } else if (selectedTab == "Money") {
                FinancialPlansScreen(
                    records = uiState.financialRecords,
                    errorMessage = uiState.errorMessage,
                    onBackToHome = { selectedTab = "Home" },
                    onAdd = { type, title, targetMinor, notes ->
                        viewModel.addFinancialRecord(type, title, targetMinor, notes = notes)
                    },
                    onAddProgress = viewModel::addFinancialProgress,
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
                                Text("TrackIt", style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                Spacer(Modifier.height(3.dp))
                                Text("Your money, made clearer.", style = MaterialTheme.typography.bodyMedium,
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
                            remaining = monthlyRemaining
                        )
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
                            SectionTitle("Quick actions", "Keep your records up to date")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                QuickAction("＋", "Add income", TrackItEmeraldTint, TrackItEmerald,
                                    Modifier.weight(1f)) {
                                    viewModel.clearError()
                                    entryType = MoneyTransaction.INCOME
                                }
                                QuickAction("−", "Add expense", TrackItExpenseTintLight, TrackItExpense,
                                    Modifier.weight(1f)) {
                                    viewModel.clearError()
                                    entryType = MoneyTransaction.EXPENSE
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
                            SectionTitle("Recent activity", "Your latest saved transactions")
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
}

@Composable
private fun OverviewCard(
    monthLabel: String,
    income: Long,
    spent: Long,
    remaining: Long
) {
    val isDark = MaterialTheme.colorScheme.background == TrackItBackgroundDark
    val cardShape = RoundedCornerShape(26.dp)
    val cardBackground = if (isDark) TrackItNavyElevated else TrackItNavy
    val monthPillBackground = if (isDark) TrackItNavySurface else TrackItNavyElevated

    Column(
        Modifier.fillMaxWidth()
            .clip(cardShape)
            .background(cardBackground)
            .then(
                if (isDark) Modifier.border(
                    width = 1.dp,
                    color = TrackItNavySurface,
                    shape = cardShape
                ) else Modifier
            )
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MONTHLY OVERVIEW", Modifier.weight(1f), color = Color(0xFFB8C2D1),
                style = MaterialTheme.typography.labelMedium, letterSpacing = 1.1.sp)
            Surface(color = monthPillBackground, shape = RoundedCornerShape(50)) {
                Text(monthLabel, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color(0xFFE5EAF1), style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Money left this month", color = Color(0xFFD0D7E2),
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(formatKes(remaining), color = Color.White,
            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = TrackItNavySurface)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("↗  Income", color = Color(0xFF6EE7B7), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text(formatKes(income), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text("↘  Spent", color = Color(0xFFFFA3A3), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(5.dp))
                Text(formatKes(spent), color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
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
        Row(Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(tint),
                contentAlignment = Alignment.Center) {
                Text(symbol, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Medium)
            }
            Text(title, color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall)
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
            remaining = 1_842_000
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
            remaining = 1_842_000
        )
    }
}