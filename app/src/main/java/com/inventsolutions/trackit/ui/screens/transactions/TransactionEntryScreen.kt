package com.inventsolutions.trackit.ui.screens.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.inventsolutions.trackit.data.local.MoneyTransaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private val paymentMethods = listOf("Cash", "M-Pesa", "Bank", "Card", "Other")

@Composable
fun TransactionEntryScreen(
    type: String,
    isSaving: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onClearError: () -> Unit,
    onSave: (
        type: String,
        amountMinor: Long,
        category: String,
        description: String,
        paymentMethod: String,
        dateEpochMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("M-Pesa") }
    var paymentMenuExpanded by remember { mutableStateOf(false) }
    var dateEpochMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val isIncome = type == MoneyTransaction.INCOME
    val dateLabel = DateFormat.getDateInstance(DateFormat.MEDIUM)
        .format(Date(dateEpochMillis))

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isIncome) "Add income" else "Add expense",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isIncome) "Record money you received." else "Record money you spent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onBack) { Text("Cancel") }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    if (isIncome) "INCOME AMOUNT" else "EXPENSE AMOUNT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter { char -> char.isDigit() || char == '.' }.take(12)
                        if (errorMessage != null) onClearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (KSh)") },
                    placeholder = { Text("e.g. 2500") },
                    prefix = { Text("KSh ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        if (errorMessage != null) onClearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Category") },
                    placeholder = {
                        Text(if (isIncome) "e.g. Side hustle, Salary" else "e.g. Food, Transport")
                    },
                    singleLine = true
                )
                Text(
                    "Quick categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val suggestions = if (isIncome) {
                        listOf("Salary", "Side hustle", "Gift")
                    } else {
                        listOf("Food", "Transport", "Bills")
                    }
                    suggestions.forEach { suggestion ->
                        OutlinedButton(onClick = { category = suggestion }) {
                            Text(suggestion)
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What was this for?") },
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateEpochMillis
                            }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    dateEpochMillis = chosen.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(dateLabel)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Payment method",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { paymentMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(paymentMethod)
                    }
                    DropdownMenu(
                        expanded = paymentMenuExpanded,
                        onDismissRequest = { paymentMenuExpanded = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    paymentMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                val amountMinor = runCatching {
                    BigDecimal(amount.trim())
                        .movePointRight(2)
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact()
                }.getOrNull() ?: 0L
                onSave(
                    type,
                    amountMinor,
                    category,
                    description,
                    paymentMethod,
                    dateEpochMillis
                )
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Save " + if (isIncome) "income" else "expense")
            }
        }
        Text(
            "Your transaction is saved on this device and works offline.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
