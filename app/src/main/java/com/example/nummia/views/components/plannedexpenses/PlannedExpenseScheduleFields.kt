package com.example.nummia.views.components.plannedexpenses

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Permite elegir si una factura vence una vez o se repite. */
@Composable
internal fun PlannedExpenseFrequencyPicker(
    frequency: BillingFrequency,
    onFrequencyChange: (BillingFrequency) -> Unit
) {
    Text("Frecuencia")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = frequency == BillingFrequency.NONE,
            onClick = { onFrequencyChange(BillingFrequency.NONE) },
            label = { Text("Una vez") }
        )
        FilterChip(
            selected = frequency == BillingFrequency.WEEKLY,
            onClick = { onFrequencyChange(BillingFrequency.WEEKLY) },
            label = { Text("Semanal") }
        )
        FilterChip(
            selected = frequency == BillingFrequency.MONTHLY,
            onClick = { onFrequencyChange(BillingFrequency.MONTHLY) },
            label = { Text("Mensual") }
        )
    }
}

/** Muestra el selector de fecha para una factura que vence una sola vez. */
@Composable
internal fun PlannedExpenseDueDateField(
    dueAt: Long?,
    onDueAtChange: (Long) -> Unit,
    onDueDateChange: (String) -> Unit
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = dueAt?.let {
            SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES"))
                .format(Date(it))
        } ?: "Seleccionar fecha",
        onValueChange = {},
        label = { Text("Fecha de vencimiento") },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            androidx.compose.material3.TextButton(
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
                            onDueAtChange(chosen.timeInMillis)
                            onDueDateChange(
                                SimpleDateFormat(
                                    "d MMM yyyy",
                                    Locale.forLanguageTag("es-ES")
                                ).format(Date(chosen.timeInMillis))
                            )
                        },
                        initialDate.get(Calendar.YEAR),
                        initialDate.get(Calendar.MONTH),
                        initialDate.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) { Text("Elegir") }
        }
    )
}

/** Permite elegir el día de semana de una factura recurrente semanal. */
@Composable
internal fun WeeklyBillingDayPicker(
    billingDay: Int,
    onBillingDayChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
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
        OutlinedButton(onClick = { expanded = true }) {
            Text("Día de facturación: ${selectedDay.second}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            days.forEach { (day, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onBillingDayChange(day)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Permite introducir el día natural de una factura mensual. */
@Composable
internal fun MonthlyBillingDayField(
    billingDayText: String,
    onBillingDayTextChange: (String) -> Unit
) {
    OutlinedTextField(
        billingDayText,
        onBillingDayTextChange,
        label = { Text("Día del mes (1-31)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

/** Configura el periodo final opcional de una factura recurrente. */
@Composable
internal fun PlannedExpenseRepeatLimitFields(
    frequency: BillingFrequency,
    limitRepetition: Boolean,
    repeatUntilAt: Long?,
    includeUntilPeriod: Boolean,
    onLimitRepetitionChange: (Boolean) -> Unit,
    onRepeatUntilAtChange: (Long?) -> Unit,
    onIncludeUntilPeriodChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column {
        if (frequency != BillingFrequency.NONE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = limitRepetition,
                    onCheckedChange = {
                        onLimitRepetitionChange(it)
                        if (!it) onRepeatUntilAtChange(null)
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
                                onRepeatUntilAtChange(
                                    Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                )
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
                            SimpleDateFormat(format, Locale.forLanguageTag("es-ES"))
                                .format(Date(it))
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
                            onCheckedChange = onIncludeUntilPeriodChange
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
    }
}
