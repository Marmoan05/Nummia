package com.example.nummia.views.components.plannedexpenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.formatDate
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.isOverdue

/** Reúne las acciones y los avisos que se muestran en cada factura pendiente. */
@Composable
internal fun PlannedExpenseRow(
    expense: PlannedExpense,
    onMarkPaid: (Long, Long) -> Unit,
    onUpdate: (PlannedExpense) -> Unit,
    onDelete: (Long) -> Unit,
    openDetailsOnStart: Boolean = false,
    onDetailsOpened: () -> Unit = {}
) {
    var showDetails by remember(expense.id) { mutableStateOf(false) }
    LaunchedEffect(openDetailsOnStart) {
        if (openDetailsOnStart) {
            showDetails = true
            onDetailsOpened()
        }
    }
    var showPaymentDialog by remember(expense.id) { mutableStateOf(false) }
    var showEditDialog by remember(expense.id) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(expense.id) { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showDetails = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Icon(
                Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Medium)
                if (expense.nextDueAt > 0L && expense.nextDueAt.isOverdue()) {
                    Text(
                        "RETRASADA · Venció ${formatDate(expense.nextDueAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                val frequencyLabel = when (expense.frequency) {
                    BillingFrequency.NONE -> ""
                    BillingFrequency.WEEKLY -> " · Semanal"
                    BillingFrequency.MONTHLY -> " · Mensual"
                }
                Text(
                    "${expense.category} · ${expense.dueDate}$frequencyLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatMoney(expense.amountCents, LocalCurrencyCode.current),
                fontWeight = FontWeight.SemiBold
            )
        }
        androidx.compose.material3.TextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = { showPaymentDialog = true }
        ) {
            Text("Marcar pagada")
        }
    }
    if (showPaymentDialog) {
        PlannedExpensePaymentDialog(
            expense = expense,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { extraCents ->
                onMarkPaid(expense.id, extraCents)
                showPaymentDialog = false
            }
        )
    }
    if (showDetails) {
        PlannedExpenseDetailDialog(
            expense = expense,
            onDismiss = { showDetails = false },
            onMarkPaid = {
                showDetails = false
                showPaymentDialog = true
            },
            onEdit = {
                showDetails = false
                showEditDialog = true
            },
            onEnableReminders = {
                onUpdate(expense.copy(remindersDisabled = false, reminderSnoozedUntil = 0L))
            },
            onDelete = {
                showDetails = false
                showDeleteConfirmation = true
            }
        )
    }
    if (showEditDialog) {
        PlannedExpenseDialog(
            onDismiss = { showEditDialog = false },
            initialExpense = expense,
            onSave = { title, description, amount, category, dueDate, dueAt, frequency, day, until, includeUntil ->
                onUpdate(
                    expense.copy(
                        title = title,
                        description = description,
                        amountCents = amount,
                        category = category,
                        dueDate = dueDate,
                        nextDueAt = dueAt,
                        frequency = frequency,
                        billingDay = day,
                        repeatUntilAt = until,
                        includeUntilPeriod = includeUntil
                    )
                )
                showEditDialog = false
            }
        )
    }
    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar factura") },
            text = { Text("¿Seguro que quieres eliminar «${expense.title}»? Esta acción no se puede deshacer.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { onDelete(expense.id) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
