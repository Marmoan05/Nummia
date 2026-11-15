package com.example.nummia.views.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.nummia.model.transactions.FinanceCategories

/** Permite editar un importe en euros y confirmarlo si tiene un formato válido. */
@Composable
internal fun AmountDialog(
    title: String,
    initialCents: Long,
    confirmLabel: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var amount by remember { mutableStateOf(formatInputAmount(initialCents, currencyCode)) }
    val cents = parseCents(amount, currencyCode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                amount,
                { amount = it },
                label = { Text("Importe ($currencyCode)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(enabled = cents != null && cents >= 0, onClick = { cents?.let(onConfirm) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Solicita los límites de gasto semanal y mensual y valida ambos importes. */
@Composable
internal fun BudgetDialog(
    weeklyCents: Long,
    monthlyCents: Long,
    onSave: (Long, Long) -> Unit,
    onDismiss: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var weekly by remember { mutableStateOf(formatInputAmount(weeklyCents, currencyCode)) }
    var monthly by remember { mutableStateOf(formatInputAmount(monthlyCents, currencyCode)) }
    var weeklyUnlimited by remember { mutableStateOf(weeklyCents <= 0L) }
    var monthlyUnlimited by remember { mutableStateOf(monthlyCents <= 0L) }
    val weeklyValue = parseCents(weekly, currencyCode)
    val monthlyValue = parseCents(monthly, currencyCode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar presupuestos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    weekly,
                    { weekly = it },
                    label = { Text("Límite semanal ($currencyCode)") },
                    enabled = !weeklyUnlimited,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row {
                    Checkbox(checked = weeklyUnlimited, onCheckedChange = { weeklyUnlimited = it })
                    Text("Sin límite semanal")
                }
                OutlinedTextField(
                    monthly,
                    { monthly = it },
                    label = { Text("Límite mensual ($currencyCode)") },
                    enabled = !monthlyUnlimited,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Row {
                    Checkbox(checked = monthlyUnlimited, onCheckedChange = { monthlyUnlimited = it })
                    Text("Sin límite mensual")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = (weeklyUnlimited || weeklyValue != null && weeklyValue > 0) &&
                    (monthlyUnlimited || monthlyValue != null && monthlyValue > 0),
                onClick = {
                    val weeklyAmount = if (weeklyUnlimited) 0L else weeklyValue
                    val monthlyAmount = if (monthlyUnlimited) 0L else monthlyValue
                    if (weeklyAmount != null && monthlyAmount != null) {
                        onSave(weeklyAmount, monthlyAmount)
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Presenta las categorías disponibles y devuelve la elegida por el usuario. */
@Composable
internal fun CategoryPicker(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        androidx.compose.material3.OutlinedButton(onClick = { expanded = true }) {
            Text("Categoría: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FinanceCategories.all.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}
