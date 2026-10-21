package com.inventsolutions.trackit.ui.screens.budgets

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    val daysInMonth = remember { Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }
    val totalLimit = budgets.sumOf { it.limitMinor }
    val totalSpent = budgets.sumOf { spendingByCategory[it.category.lowercase(Locale.getDefault())] ?: 0L }
    val totalRemaining = totalLimit - totalSpent

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Budgets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(monthLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBackToHome) { Text("Home") }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PLANNED MONTHLY LIMIT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(formatKes(totalLimit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Spent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(formatKes(totalSpent), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column {
                            Text(if (totalRemaining >= 0) "Available" else "Over budget", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(formatKes(abs(totalRemaining)), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
        val budgetAlerts = budgets.mapNotNull { budget ->
            val categoryKey = budget.category.lowercase(Locale.getDefault())
            val monthlySpent = spendingByCategory[categoryKey] ?: 0L
            val dailySpent = spendingTodayByCategory[categoryKey] ?: 0L
            val dailyLimit = budget.limitMinor / daysInMonth
            when {
                monthlySpent > budget.limitMinor ->
                    "${budget.category}: monthly limit exceeded by ${formatKes(monthlySpent - budget.limitMinor)}."
                budget.budgetType == MonthlyBudget.DAILY_ALLOWANCE && dailySpent > dailyLimit ->
                    "${budget.category}: daily allowance exceeded by ${formatKes(dailySpent - dailyLimit)} today."
                else -> null
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("⚠", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge)
                            Text("Budget alert", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        budgetAlerts.forEach { alert ->
                            Text("• $alert", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a category budget", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = category, onValueChange = { category = it; validation = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Category (e.g. Food)") }, singleLine = true, readOnly = editingBudgetId != null)
                    Text("Budget style", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { budgetType = MonthlyBudget.ONE_TIME; validation = null }
                        ) { Text(if (budgetType == MonthlyBudget.ONE_TIME) "✓ One-time" else "One-time") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { budgetType = MonthlyBudget.DAILY_ALLOWANCE; validation = null }
                        ) { Text(if (budgetType == MonthlyBudget.DAILY_ALLOWANCE) "✓ Daily allowance" else "Daily allowance") }
                    }
                    OutlinedTextField(value = amount, onValueChange = { amount = it; validation = null }, modifier = Modifier.fillMaxWidth(), label = { Text("Total monthly allocation (KSh)") }, singleLine = true)
                    if (budgetType == MonthlyBudget.DAILY_ALLOWANCE) {
                        val parsedAllocation = amount.trim().replace(",", "").toBigDecimalOrNull()
                        val dailyMinor = try { parsedAllocation?.movePointRight(2)?.divide(java.math.BigDecimal(daysInMonth), 0, java.math.RoundingMode.DOWN)?.longValueExact() } catch (_: Exception) { null }
                        Text(
                            if (dailyMinor != null && dailyMinor > 0L) "Daily allowance: ${formatKes(dailyMinor)} per day ($daysInMonth days). Unused allowance does not roll over."
                            else "Daily allowance is calculated from the total allocation divided by $daysInMonth days.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Track spending against the full allocation for the month.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(onClick = {
                        val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                        val minor = try { parsed?.movePointRight(2)?.setScale(0, java.math.RoundingMode.HALF_UP)?.longValueExact() } catch (_: Exception) { null }
                        when {
                            category.isBlank() -> validation = "Enter a category."
                            budgets.any { it.id != editingBudgetId && it.category.equals(category.trim(), ignoreCase = true) } -> validation = "A budget for this category already exists this month."
                            minor == null || minor <= 0L -> validation = "Enter a valid amount greater than zero."
                            else -> { onSaveBudget(category.trim(), minor, budgetType); category = ""; amount = ""; editingBudgetId = null; budgetType = MonthlyBudget.ONE_TIME; validation = null }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (editingBudgetId == null) "Save budget" else "Update budget") }
                }
            }
        }
        if (budgets.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("No budgets set yet", style = MaterialTheme.typography.titleMedium)
                        Text("Create a category budget above. Expense transactions will update its progress automatically.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { Text("Category budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(budgets, key = { it.id }) { budget ->
                val spent = spendingByCategory[budget.category.lowercase(Locale.getDefault())] ?: 0L
                val remaining = budget.limitMinor - spent
                val progress = (spent.toDouble() / budget.limitMinor.toDouble()).coerceIn(0.0, 1.0).toFloat()
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(budget.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${(spent.toDouble() / budget.limitMinor * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${formatKes(spent)} spent of ${formatKes(budget.limitMinor)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val todaySpent = spendingTodayByCategory[budget.category.lowercase(Locale.getDefault())] ?: 0L
                        val dailyAllowanceMinor = budget.limitMinor / daysInMonth
                        if (budget.budgetType == MonthlyBudget.DAILY_ALLOWANCE && todaySpent > dailyAllowanceMinor) {
                            Text(
                                "Daily allowance exceeded by ${formatKes(todaySpent - dailyAllowanceMinor)} today",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (spent > budget.limitMinor) {
                            Text(
                                "Monthly limit exceeded by ${formatKes(spent - budget.limitMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (budget.budgetType == MonthlyBudget.DAILY_ALLOWANCE) {
                            val dailyAllowanceMinor = budget.limitMinor / daysInMonth
                            Text(
                                "Daily guide: ${formatKes(dailyAllowanceMinor)} · ${daysInMonth} days this month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("One-time monthly allocation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (spent > budget.limitMinor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (remaining >= 0) "${formatKes(remaining)} remaining" else "${formatKes(-remaining)} over limit", style = MaterialTheme.typography.bodySmall, color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    editingBudgetId = budget.id
                                    category = budget.category
                                    amount = (budget.limitMinor / 100.0).toString()
                                    budgetType = budget.budgetType
                                    validation = null
                                }) { Text("Edit limit") }
                                OutlinedButton(onClick = {
                                    if (editingBudgetId == budget.id) {
                                        editingBudgetId = null
                                        category = ""
                                        amount = ""
                                        budgetType = MonthlyBudget.ONE_TIME
                                    }
                                    onDeleteBudget(budget.id)
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
