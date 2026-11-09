package com.inventsolutions.trackit.ui.screens.budgets

import com.inventsolutions.trackit.R
import com.inventsolutions.trackit.ui.components.TrackItIcon
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import com.inventsolutions.trackit.data.local.MonthlyBudget
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun formatKes(minor: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (minor % 100L == 0L) 0 else 2
    }
    return "KSh " + formatter.format(minor / 100.0)
}

@Composable
fun BudgetScreen(
    monthLabel: String,
    budgets: List<MonthlyBudget>,
    spendingByCategory: Map<String, Long>,
    spendingTodayByCategory: Map<String, Long>,
    onSaveBudget: (category: String, limitMinor: Long, budgetType: String) -> Unit,
    onDeleteBudget: (budgetId: Long) -> Unit,
    onBackToHome: () -> Unit
) {
    var category by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }
    var editingBudgetId by remember { mutableStateOf<Long?>(null) }
    var budgetType by remember { mutableStateOf(MonthlyBudget.ONE_TIME) }
    var showBudgetForm by remember { mutableStateOf(false) }

    val daysInMonth = remember { Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }
    val totalLimit = budgets.sumOf { it.limitMinor }
    val totalSpent = budgets.sumOf { spendingByCategory[it.category.lowercase(Locale.getDefault())] ?: 0L }
    val totalRemaining = totalLimit - totalSpent
    val usedFraction = if (totalLimit > 0L) (totalSpent.toDouble() / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val usedPercent = if (totalLimit > 0L) (totalSpent.toDouble() / totalLimit * 100).roundToInt() else 0

    fun closeForm() {
        showBudgetForm = false
        category = ""
        amount = ""
        validation = null
        editingBudgetId = null
        budgetType = MonthlyBudget.ONE_TIME
    }

    fun saveBudget() {
        val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
        val minor = try {
            parsed?.movePointRight(2)?.setScale(0, java.math.RoundingMode.HALF_UP)?.longValueExact()
        } catch (_: Exception) {
            null
        }
        when {
            category.isBlank() -> validation = "Enter a category."
            budgets.any { it.id != editingBudgetId && it.category.equals(category.trim(), ignoreCase = true) } ->
                validation = "A budget for this category already exists this month."
            minor == null || minor <= 0L -> validation = "Enter a valid amount greater than zero."
            else -> {
                onSaveBudget(category.trim(), minor, budgetType)
                closeForm()
            }
        }
    }

    if (showBudgetForm) {
        val parsedAllocation = amount.trim().replace(",", "").toBigDecimalOrNull()
        val dailyMinor = try {
            parsedAllocation?.movePointRight(2)
                ?.divide(java.math.BigDecimal(daysInMonth), 0, java.math.RoundingMode.DOWN)
                ?.longValueExact()
        } catch (_: Exception) {
            null
        }
        AlertDialog(
            onDismissRequest = { closeForm() },
            title = {
                Text(
                    if (editingBudgetId == null) "Add category budget" else "Edit category budget",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Set a monthly allocation and choose how you want to track spending.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it; validation = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Category (e.g. Food)") },
                        singleLine = true,
                        readOnly = editingBudgetId != null
                    )
                    Text("Budget style", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = budgetType == MonthlyBudget.ONE_TIME,
                            onClick = { budgetType = MonthlyBudget.ONE_TIME; validation = null },
                            label = { Text("One-time") }
                        )
                        FilterChip(
                            selected = budgetType == MonthlyBudget.RECURRING,
                            onClick = { budgetType = MonthlyBudget.RECURRING; validation = null },
                            label = { Text("Recurring") }
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it; validation = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Total monthly allocation (KSh)") },
                        singleLine = true
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (budgetType == MonthlyBudget.RECURRING) {
                                Text(
                                    if (dailyMinor != null && dailyMinor > 0L) "Daily allowance · ${formatKes(dailyMinor)}"
                                    else "Daily allowance is calculated automatically",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "The monthly allocation is divided across $daysInMonth days. Unused daily amounts do not roll over to the next day.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Monthly spending pool",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Track spending against the full allocation for the month.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    validation?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { saveBudget() }) {
                    Text(if (editingBudgetId == null) "Save budget" else "Update budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { closeForm() }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Budgets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(monthLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "PLANNED MONTHLY BUDGET",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        formatKes(totalLimit),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Total spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(formatKes(totalSpent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(if (totalRemaining >= 0L) "Remaining" else "Over budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            Text(formatKes(abs(totalRemaining)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Budget health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                when {
                                    totalLimit == 0L -> "Add a budget to start tracking"
                                    totalSpent > totalLimit -> "Your spending is above the planned limit"
                                    usedPercent >= 80 -> "You're getting close to your budget limit"
                                    else -> "You're within your planned budget"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (totalLimit == 0L) "—" else "$usedPercent%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSpent > totalLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (totalSpent > totalLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${formatKes(totalSpent)} used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formatKes(totalLimit)} planned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        val budgetAlerts = budgets.flatMap { budget ->
            val categoryKey = budget.category.lowercase(Locale.getDefault())
            val monthlySpent = spendingByCategory[categoryKey] ?: 0L
            val dailySpent = spendingTodayByCategory[categoryKey] ?: 0L
            val dailyLimit = budget.limitMinor / daysInMonth
            buildList {
                if (monthlySpent > budget.limitMinor) {
                    add("${budget.category}: monthly limit exceeded by ${formatKes(monthlySpent - budget.limitMinor)}.")
                }
                if (budget.budgetType == MonthlyBudget.RECURRING && dailySpent > dailyLimit) {
                    add("${budget.category}: daily allowance exceeded by ${formatKes(dailySpent - dailyLimit)} today.")
                }
            }
        }
        if (budgetAlerts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TrackItIcon(R.drawable.ic_lucide_alert_triangle, null, tint = MaterialTheme.colorScheme.error)
                            Text("Needs attention", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        budgetAlerts.forEach { alert ->
                            Text("• $alert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Category budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${budgets.size} categories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (budgets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrackItIcon(R.drawable.ic_lucide_wallet, null, tint = MaterialTheme.colorScheme.primary, size = 34.dp)
                        Text("No budgets yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Create your first category budget to see how your spending is progressing throughout the month.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = { showBudgetForm = true }) { Text("Create first budget") }
                    }
                }
            }
        } else {
            items(budgets, key = { it.id }) { budget ->
                val categoryKey = budget.category.lowercase(Locale.getDefault())
                val spent = spendingByCategory[categoryKey] ?: 0L
                val remaining = budget.limitMinor - spent
                val progress = (spent.toDouble() / budget.limitMinor.toDouble()).coerceIn(0.0, 1.0).toFloat()
                val percent = (spent.toDouble() / budget.limitMinor.toDouble() * 100).roundToInt()
                val todaySpent = spendingTodayByCategory[categoryKey] ?: 0L
                val dailyLimit = budget.limitMinor / daysInMonth
                val dailyExceeded = budget.budgetType == MonthlyBudget.RECURRING && todaySpent > dailyLimit
                val monthlyExceeded = spent > budget.limitMinor
                val statusColor = if (monthlyExceeded || dailyExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val statusText = when {
                    monthlyExceeded -> "Over monthly limit"
                    dailyExceeded -> "Daily allowance exceeded"
                    percent >= 80 -> "Near limit"
                    else -> "On track"
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(budget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (budget.budgetType == MonthlyBudget.RECURRING) "Recurring daily budget" else "One-time monthly budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(statusText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = statusColor)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatKes(spent), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Budget", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatKes(budget.limitMinor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = statusColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$percent% used", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (remaining >= 0L) "${formatKes(remaining)} remaining" else "${formatKes(-remaining)} over",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (remaining < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (budget.budgetType == MonthlyBudget.RECURRING) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Daily allowance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatKes(dailyLimit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Based on $daysInMonth days · unused amount doesn't roll over",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatKes(todaySpent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (dailyExceeded) "${formatKes(todaySpent - dailyLimit)} over" else "${formatKes((dailyLimit - todaySpent).coerceAtLeast(0L))} left",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (dailyExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        if (monthlyExceeded) {
                            Text(
                                "Monthly limit exceeded by ${formatKes(spent - budget.limitMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                editingBudgetId = budget.id
                                category = budget.category
                                amount = (budget.limitMinor / 100.0).toString()
                                budgetType = budget.budgetType
                                validation = null
                                showBudgetForm = true
                            }) {
                                TrackItIcon(R.drawable.ic_lucide_edit, null, size = 19.dp)
                                Text("Edit")
                            }
                            TextButton(onClick = { onDeleteBudget(budget.id) }) {
                                TrackItIcon(R.drawable.ic_lucide_trash, null, tint = MaterialTheme.colorScheme.error, size = 19.dp)
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
