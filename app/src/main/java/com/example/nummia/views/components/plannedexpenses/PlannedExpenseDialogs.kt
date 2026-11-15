package com.example.nummia.views.components.plannedexpenses

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.views.components.CategoryPicker
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.formatInputAmount
import com.example.nummia.views.components.parseCents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Recoge los datos necesarios para anotar un pago pendiente de realizar. */
@Composable
internal fun PlannedExpenseDialog(
    onDismiss: () -> Unit,
    initialExpense: PlannedExpense? = null,
    onSave: (String, String?, Long, String, String, Long, BillingFrequency, Int, Long?, Boolean) -> Unit
) {
    val context = LocalContext.current
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
    var dayMenuExpanded by remember { mutableStateOf(false) }
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
                Text("Frecuencia")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = frequency == BillingFrequency.NONE,
                        onClick = { frequency = BillingFrequency.NONE },
                        label = { Text("Una vez") }
                    )
                    FilterChip(
                        selected = frequency == BillingFrequency.WEEKLY,
                        onClick = { frequency = BillingFrequency.WEEKLY },
                        label = { Text("Semanal") }
                    )
                    FilterChip(
                        selected = frequency == BillingFrequency.MONTHLY,
                        onClick = {
                            frequency = BillingFrequency.MONTHLY
                            billingDayText.toIntOrNull()?.let { billingDay = it }
                        },
                        label = { Text("Mensual") }
                    )
                }
                when (frequency) {
                    BillingFrequency.NONE -> OutlinedTextField(
                        value = dueAt?.let {
                            SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES")).format(Date(it))
                        } ?: "Seleccionar fecha",
                        onValueChange = {},
                        label = { Text("Fecha de vencimiento") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    val initialDate = dueAt?.let {
                                        Calendar.getInstance().apply { timeInMillis = it }
                                    } ?: Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val chosen = Calendar.getInstance().apply {
                                                set(year, month, day, 9, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            dueAt = chosen.timeInMillis
                                            dueDate = SimpleDateFormat(
                                                "d MMM yyyy",
                                                Locale.forLanguageTag("es-ES")
                                            ).format(Date(chosen.timeInMillis))
                                        },
                                        initialDate.get(Calendar.YEAR),
                                        initialDate.get(Calendar.MONTH),
                                        initialDate.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            ) { Text("Elegir") }
                        }
                    )
                    BillingFrequency.WEEKLY -> {
                        val days = listOf(
                            Calendar.MONDAY to "Lunes",
                            Calendar.TUESDAY to "Martes",
                            Calendar.WEDNESDAY to "Miércoles",
                            Calendar.THURSDAY to "Jueves",
                            Calendar.FRIDAY to "Viernes",
                            Calendar.SATURDAY to "Sábado",
                            Calendar.SUNDAY to "Domingo"
                        )
                        val selectedDay = days.first { it.first == billingDay }
                        Box {
                            OutlinedButton(onClick = { dayMenuExpanded = true }) {
                                Text("Día de facturación: ${selectedDay.second}")
                            }
                            DropdownMenu(
                                expanded = dayMenuExpanded,
                                onDismissRequest = { dayMenuExpanded = false }
                            ) {
                                days.forEach { (day, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            billingDay = day
                                            dayMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    BillingFrequency.MONTHLY -> OutlinedTextField(
                        billingDayText,
                        { text ->
                            billingDayText = text
                            text.toIntOrNull()?.let { billingDay = it }
                        },
                        label = { Text("Día del mes (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                if (frequency != BillingFrequency.NONE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = limitRepetition,
                            onCheckedChange = {
                                limitRepetition = it
                                if (!it) repeatUntilAt = null
                            }
                        )
                        Text("Repetir hasta un periodo")
                    }
                    if (limitRepetition) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val initialDate = repeatUntilAt?.let {
                                    Calendar.getInstance().apply { timeInMillis = it }
                                } ?: Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        repeatUntilAt = Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    },
                                    initialDate.get(Calendar.YEAR),
                                    initialDate.get(Calendar.MONTH),
                                    initialDate.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        ) {
                            Text(
                                repeatUntilAt?.let {
                                    val format = if (frequency == BillingFrequency.WEEKLY) {
                                        "'Semana de' d MMM yyyy"
                                    } else {
                                        "'Mes final: 'MMMM yyyy"
                                    }
                                    SimpleDateFormat(format, Locale.forLanguageTag("es-ES")).format(Date(it))
                                } ?: if (frequency == BillingFrequency.WEEKLY) {
                                    "Elegir semana final"
                                } else {
                                    "Elegir mes final"
                                }
                            )
                        }
                        if (repeatUntilAt != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeUntilPeriod,
                                    onCheckedChange = { includeUntilPeriod = it }
                                )
                                Text(
                                    if (frequency == BillingFrequency.WEEKLY) {
                                        "Incluir la última semana"
                                    } else {
                                        "Incluir el último mes"
                                    }
                                )
                            }
                        }
                    }
                }
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
