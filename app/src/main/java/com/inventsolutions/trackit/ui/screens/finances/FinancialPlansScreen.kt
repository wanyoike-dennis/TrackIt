package com.inventsolutions.trackit.ui.screens.finances

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.FinancialRecord
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
    errorMessage: String?,
    onBackToHome: () -> Unit,
    onAdd: (type: String, title: String, targetMinor: Long, notes: String) -> Unit,
    onAddProgress: (FinancialRecord, Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearError: () -> Unit
) {
    var section by remember { mutableStateOf(FinanceSection.DEBTS) }
    var showCreate by remember { mutableStateOf(false) }
    var progressRecord by remember { mutableStateOf<FinancialRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<FinancialRecord?>(null) }

    val visibleRecords = records.filter {
        when (section) {
            FinanceSection.DEBTS -> it.type == FinancialRecord.DEBT_OWED_TO_ME || it.type == FinancialRecord.DEBT_I_OWE
            FinanceSection.SAVINGS -> it.type == FinancialRecord.SAVINGS_GOAL
            FinanceSection.ALLOWANCE -> it.type == FinancialRecord.ALLOWANCE
        }
    }
    val sectionTotal = when (section) {
        FinanceSection.DEBTS -> visibleRecords.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
        FinanceSection.SAVINGS -> visibleRecords.sumOf { it.currentMinor }
        FinanceSection.ALLOWANCE -> visibleRecords.sumOf { (it.targetMinor - it.currentMinor).coerceAtLeast(0L) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Money plans", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Keep debts, savings and allowances organised.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onBackToHome) { Text("Home") }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = section == FinanceSection.DEBTS, onClick = { section = FinanceSection.DEBTS }, label = { Text("Debts") })
                FilterChip(selected = section == FinanceSection.SAVINGS, onClick = { section = FinanceSection.SAVINGS }, label = { Text("Savings") })
                FilterChip(selected = section == FinanceSection.ALLOWANCE, onClick = { section = FinanceSection.ALLOWANCE }, label = { Text("Allowance") })
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        when (section) {
                            FinanceSection.DEBTS -> "OUTSTANDING DEBT"
                            FinanceSection.SAVINGS -> "TOTAL SAVED"
                            FinanceSection.ALLOWANCE -> "ALLOWANCE REMAINING"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(financeKes(sectionTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        when (section) {
                            FinanceSection.DEBTS -> "Remaining amount across debts you owe and are owed."
                            FinanceSection.SAVINGS -> "Money contributed toward your savings goals."
                            FinanceSection.ALLOWANCE -> "Combined remaining allowance across your plans."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        item {
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth()) {
                Text(when (section) {
                    FinanceSection.DEBTS -> "Add debt"
                    FinanceSection.SAVINGS -> "Create savings goal"
                    FinanceSection.ALLOWANCE -> "Set allowance"
                })
            }
        }
        if (errorMessage != null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(errorMessage, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onClearError) { Text("Dismiss") }
                    }
                }
            }
        }
        if (visibleRecords.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            when (section) {
                                FinanceSection.DEBTS -> "Add money someone owes you or a debt you need to repay."
                                FinanceSection.SAVINGS -> "Create a target and record each contribution as you save."
                                FinanceSection.ALLOWANCE -> "Set an allowance and record spending against it."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(visibleRecords, key = { it.id }) { record ->
                val progress = if (record.targetMinor > 0L) (record.currentMinor.toFloat() / record.targetMinor).coerceIn(0f, 1f) else 0f
                val remaining = (record.targetMinor - record.currentMinor).coerceAtLeast(0L)
                val isAllowance = record.type == FinancialRecord.ALLOWANCE
                val isDebt = record.type == FinancialRecord.DEBT_OWED_TO_ME || record.type == FinancialRecord.DEBT_I_OWE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when (record.type) {
                                        FinancialRecord.DEBT_OWED_TO_ME -> "Owed to me"
                                        FinancialRecord.DEBT_I_OWE -> "I owe"
                                        FinancialRecord.SAVINGS_GOAL -> "Savings goal"
                                        else -> "Allowance"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { deleteRecord = record }) { Text("Delete") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(if (isAllowance) "Allowance" else if (isDebt) "Total" else "Target",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(financeKes(record.targetMinor), fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    when (record.type) {
                                        FinancialRecord.DEBT_OWED_TO_ME -> "Collected"
                                        FinancialRecord.DEBT_I_OWE -> "Repaid"
                                        FinancialRecord.SAVINGS_GOAL -> "Saved"
                                        else -> "Spent"
                                    },
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(financeKes(record.currentMinor), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = if (isAllowance && record.currentMinor > record.targetMinor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            when {
                                isAllowance && remaining == 0L -> "Allowance used up or exceeded"
                                isAllowance -> "${financeKes(remaining)} remaining"
                                isDebt && remaining == 0L -> "Settled"
                                isDebt -> "${financeKes(remaining)} outstanding"
                                remaining == 0L -> "Goal reached"
                                else -> "${financeKes(remaining)} to go"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAllowance && record.currentMinor > record.targetMinor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (record.notes.isNotBlank()) {
                            Text(record.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { progressRecord = record }, modifier = Modifier.fillMaxWidth()) {
                            Text(when (record.type) {
                                FinancialRecord.DEBT_OWED_TO_ME -> "Record collection"
                                FinancialRecord.DEBT_I_OWE -> "Record repayment"
                                FinancialRecord.SAVINGS_GOAL -> "Add savings"
                                else -> "Record spending"
                            })
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Saved on this device and available offline.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showCreate) {
        var title by remember(section) { mutableStateOf("") }
        var amount by remember(section) { mutableStateOf("") }
        var notes by remember(section) { mutableStateOf("") }
        var debtType by remember { mutableStateOf(FinancialRecord.DEBT_I_OWE) }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(when (section) {
                FinanceSection.DEBTS -> "Add a debt"
                FinanceSection.SAVINGS -> "Create savings goal"
                FinanceSection.ALLOWANCE -> "Set allowance"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (section == FinanceSection.DEBTS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = debtType == FinancialRecord.DEBT_I_OWE,
                                onClick = { debtType = FinancialRecord.DEBT_I_OWE }, label = { Text("I owe") })
                            FilterChip(selected = debtType == FinancialRecord.DEBT_OWED_TO_ME,
                                onClick = { debtType = FinancialRecord.DEBT_OWED_TO_ME }, label = { Text("Owed to me") })
                        }
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name / description") }, singleLine = true)
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (KSh)") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = parsed?.let { runCatching { it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull() }
                    if (title.isNotBlank() && minor != null && minor > 0L) {
                        val type = when (section) {
                            FinanceSection.DEBTS -> debtType
                            FinanceSection.SAVINGS -> FinancialRecord.SAVINGS_GOAL
                            FinanceSection.ALLOWANCE -> FinancialRecord.ALLOWANCE
                        }
                        onAdd(type, title, minor, notes)
                        showCreate = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }

    progressRecord?.let { record ->
        var amount by remember(record.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { progressRecord = null },
            title = { Text(when (record.type) {
                FinancialRecord.DEBT_OWED_TO_ME -> "Record collection"
                FinancialRecord.DEBT_I_OWE -> "Record repayment"
                FinancialRecord.SAVINGS_GOAL -> "Add savings"
                else -> "Record allowance spending"
            }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(record.title)
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (KSh)") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = amount.trim().replace(",", "").toBigDecimalOrNull()
                    val minor = parsed?.let { runCatching { it.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact() }.getOrNull() }
                    if (minor != null && minor > 0L) {
                        onAddProgress(record, minor)
                        progressRecord = null
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { progressRecord = null }) { Text("Cancel") } }
        )
    }

    deleteRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteRecord = null },
            title = { Text("Delete record?") },
            text = { Text("Delete ${record.title}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(record.id); deleteRecord = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteRecord = null }) { Text("Cancel") } }
        )
    }
}
