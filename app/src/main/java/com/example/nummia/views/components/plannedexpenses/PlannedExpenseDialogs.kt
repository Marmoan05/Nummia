package com.example.nummia.views.components.plannedexpenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.views.components.CategoryPicker
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.formatInputAmount
import com.example.nummia.views.components.parseCents
import java.util.Calendar

/** Recoge los datos necesarios para anotar un pago pendiente de realizar. */
@Composable
internal fun PlannedExpenseDialog(
    onDismiss: () -> Unit,
    initialExpense: PlannedExpense? = null,
    onSave: (String, String?, Long, String, String, Long, BillingFrequency, Int, Long?, Boolean) -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var title by remember(initialExpense?.id) { mutableStateOf(initialExpense?.title.orEmpty()) }
    var description by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.description.orEmpty())
    }
    var amount by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.let { formatInputAmount(it.amountCents, currencyCode) }.orEmpty())
    }
    var category by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.category ?: "Facturas")
    }
    var dueDate by remember(initialExpense?.id) { mutableStateOf(initialExpense?.dueDate.orEmpty()) }
    var dueAt by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.nextDueAt?.takeIf { it > 0L })
    }
    var frequency by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.frequency ?: BillingFrequency.NONE)
    }
    val today = remember { Calendar.getInstance() }
    var billingDay by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.billingDay ?: today.get(Calendar.DAY_OF_WEEK))
    }
    var billingDayText by remember(initialExpense?.id) {
        mutableStateOf(
            (initialExpense?.takeIf { it.frequency == BillingFrequency.MONTHLY }?.billingDay
                ?: today.get(Calendar.DAY_OF_MONTH)).toString()
        )
    }
    var limitRepetition by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.repeatUntilAt != null)
    }
    var includeUntilPeriod by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.includeUntilPeriod ?: true)
    }
    var repeatUntilAt by remember(initialExpense?.id) {
        mutableStateOf(initialExpense?.repeatUntilAt)
    }
    val cents = parseCents(amount, currencyCode)
    val validBillingDay = when (frequency) {
        BillingFrequency.NONE -> true
        BillingFrequency.WEEKLY -> billingDay in Calendar.SUNDAY..Calendar.SATURDAY
        BillingFrequency.MONTHLY -> billingDayText.toIntOrNull()?.let { it in 1..31 } == true
    }
    val validEndPeriod = !limitRepetition ||
        (repeatUntilAt != null && frequency != BillingFrequency.NONE)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialExpense == null) "Nueva factura" else "Editar factura") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    title,
                    { title = it },
                    label = { Text("Nombre de la factura") },
                    singleLine = true
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = { Text("Importe ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                PlannedExpenseFrequencyPicker(
                    frequency = frequency,
                    onFrequencyChange = { selected ->
                        frequency = selected
                        if (selected == BillingFrequency.MONTHLY) {
                            billingDayText.toIntOrNull()?.let { billingDay = it }
                        }
                    }
                )
                when (frequency) {
                    BillingFrequency.NONE -> PlannedExpenseDueDateField(
                        dueAt = dueAt,
                        onDueAtChange = { dueAt = it },
                        onDueDateChange = { dueDate = it }
                    )
                    BillingFrequency.WEEKLY ->
                        WeeklyBillingDayPicker(
                            billingDay = billingDay,
                            onBillingDayChange = { billingDay = it }
                        )
                    BillingFrequency.MONTHLY -> MonthlyBillingDayField(
                        billingDayText = billingDayText,
                        onBillingDayTextChange = { text ->
                            billingDayText = text
                            text.toIntOrNull()?.let { billingDay = it }
                        }
                    )
                }
                PlannedExpenseRepeatLimitFields(
                    frequency = frequency,
                    limitRepetition = limitRepetition,
                    repeatUntilAt = repeatUntilAt,
                    includeUntilPeriod = includeUntilPeriod,
                    onLimitRepetitionChange = { limitRepetition = it },
                    onRepeatUntilAtChange = { repeatUntilAt = it },
                    onIncludeUntilPeriodChange = { includeUntilPeriod = it }
                )
                CategoryPicker(category, { category = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() &&
                    (frequency != BillingFrequency.NONE || dueAt != null) &&
                    validBillingDay && validEndPeriod && cents != null && cents > 0,
                onClick = {
                    cents?.let {
                        onSave(
                            title.trim(),
                            description.trim().takeIf { it.isNotEmpty() },
                            it,
                            category,
                            dueDate.trim(),
                            dueAt ?: 0L,
                            frequency,
                            billingDay,
                            if (limitRepetition) repeatUntilAt else null,
                            includeUntilPeriod
                        )
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
