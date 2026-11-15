package com.example.nummia.views.components.plannedexpenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.notifications.BillReminderCalculator
import com.example.nummia.views.components.formatDate
import kotlinx.coroutines.delay

/** Muestra los recordatorios elegibles junto a la lista de facturas. */
@Composable
internal fun BillRemindersNotice(
    expenses: List<PlannedExpense>,
    enabled: Boolean,
    monthlyLeadDays: Int,
    weeklyLeadDays: Int,
    reminderHour: Int,
    reminderMinute: Int,
    onOpenDetails: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onDisable: (Long) -> Unit
) {
    val currentTime = rememberCurrentTime()
    val reminders = BillReminderCalculator.remindersToShowInApp(
        expenses, currentTime, enabled, monthlyLeadDays, weeklyLeadDays, reminderHour, reminderMinute
    )
    if (reminders.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Recordatorios de facturas", fontWeight = FontWeight.SemiBold)
                reminders.forEach { expense ->
                    ReminderExpenseItem(
                        expense = expense,
                        now = currentTime,
                        onOpenDetails = { onOpenDetails(expense.id) },
                        onSnooze = { onSnooze(expense.id) },
                        onDisable = { onDisable(expense.id) }
                    )
                }
            }
        }
    }
}

/** Presenta al iniciar la app las facturas próximas o vencidas que admiten recordatorios. */
@Composable
internal fun BillRemindersDialog(
    expenses: List<PlannedExpense>,
    enabled: Boolean,
    monthlyLeadDays: Int,
    weeklyLeadDays: Int,
    reminderHour: Int,
    reminderMinute: Int,
    onDismiss: () -> Unit,
    onOpenDetails: (Long) -> Unit,
    onSnooze: (Long) -> Unit,
    onDisable: (Long) -> Unit
) {
    val currentTime = rememberCurrentTime()
    val reminders = BillReminderCalculator.remindersToShowInApp(
        expenses, currentTime, enabled, monthlyLeadDays, weeklyLeadDays, reminderHour, reminderMinute
    )
    if (reminders.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Facturas pendientes") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Estas facturas están próximas a vencer o ya vencieron:")
                    reminders.forEach { expense ->
                        ReminderExpenseItem(
                            expense = expense,
                            now = currentTime,
                            onOpenDetails = { onOpenDetails(expense.id) },
                            onSnooze = { onSnooze(expense.id) },
                            onDisable = { onDisable(expense.id) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun rememberCurrentTime(): Long {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            currentTime = System.currentTimeMillis()
        }
    }
    return currentTime
}

@Composable
private fun ReminderExpenseItem(
    expense: PlannedExpense,
    now: Long,
    onOpenDetails: () -> Unit,
    onSnooze: () -> Unit,
    onDisable: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            expense.title,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Medium
        )
        Text(
            if (BillReminderCalculator.isOverdue(expense, now)) {
                "Vencida · ${formatDate(expense.nextDueAt, now)}"
            } else {
                "Vence ${formatDate(expense.nextDueAt, now)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpenDetails) { Text("Ver detalles") }
            TextButton(onClick = onSnooze) { Text("Más tarde") }
            TextButton(onClick = onDisable) { Text("No recordar") }
        }
    }
}
