package com.example.nummia.views.components.plannedexpenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.isOverdue
import com.example.nummia.views.components.parseCents

/** Presenta los detalles de una factura pendiente y sus acciones disponibles. */
@Composable
internal fun PlannedExpenseDetailDialog(
    expense: PlannedExpense,
    onDismiss: () -> Unit,
    onMarkPaid: () -> Unit,
    onEdit: () -> Unit,
    onEnableReminders: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(expense.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Importe: ${formatMoney(expense.amountCents, currencyCode)}")
                if (!expense.description.isNullOrBlank()) {
                    Text("Descripción: ${expense.description}")
                }
                Text("Categoría: ${expense.category}")
                val frequency = when (expense.frequency) {
                    BillingFrequency.NONE -> "Pago único"
                    BillingFrequency.WEEKLY -> "Semanal"
                    BillingFrequency.MONTHLY -> "Mensual"
                }
                Text("Frecuencia: $frequency")
                Text("Vencimiento: ${expense.dueDate}")
                if (expense.nextDueAt > 0L && expense.nextDueAt.isOverdue()) {
                    Text(
                        "Factura retrasada",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
                if (expense.remindersDisabled) {
                    TextButton(onClick = onEnableReminders) {
                        Text("Volver a recordar")
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onMarkPaid) { Text("Marcar pagada") }
                TextButton(onClick = onEdit) { Text("Editar") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
}

/** Confirma un pago pendiente y permite añadir un extra por retraso. */
@Composable
internal fun PlannedExpensePaymentDialog(
    expense: PlannedExpense,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var extra by remember(expense.id) { mutableStateOf("0") }
    val extraCents = parseCents(extra, currencyCode)
    val canAddExtra = extraCents != null && extraCents >= 0 &&
        extraCents <= Long.MAX_VALUE - expense.amountCents
    val total = if (canAddExtra) expense.amountCents + (extraCents ?: 0L) else expense.amountCents
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marcar como pagada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Importe de la factura: ${formatMoney(expense.amountCents, currencyCode)}")
                OutlinedTextField(
                    extra,
                    { extra = it },
                    label = { Text("Extra por retraso ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("Se añadirá un gasto de ${formatMoney(total, currencyCode)}.")
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAddExtra,
                onClick = { extraCents?.let(onConfirm) }
            ) { Text("Confirmar pago") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
