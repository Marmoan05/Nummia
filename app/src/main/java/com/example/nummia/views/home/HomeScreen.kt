package com.example.nummia.views.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.nummia.calculations.BudgetSummary
import com.example.nummia.model.state.AppState
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.components.BudgetProgressCard
import com.example.nummia.views.components.AmountDialog
import com.example.nummia.views.components.EmptyMessage
import com.example.nummia.views.components.MovementDialog
import com.example.nummia.views.components.MovementRow
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.plannedexpenses.BillRemindersNotice
import com.example.nummia.views.components.plannedexpenses.PlannedExpenseDialog
import com.example.nummia.views.components.plannedexpenses.PlannedExpenseRow
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.SectionTitle
import com.example.nummia.views.components.SummaryAmountCard
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.isOverdue
import com.example.nummia.views.components.isInCurrentMonth

/** Presenta saldo, resúmenes, presupuestos, pagos próximos y movimientos recientes. */
@Composable
fun HomeScreen(
    state: AppState,
    budgetSummary: BudgetSummary,
    onAddMovement: (String, String?, Long, String, TransactionType, Long) -> Unit,
    onMovementClick: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onSetInitialBalance: (Long) -> Unit,
    onAddPlannedExpense: (String, String?, Long, String, String, Long, BillingFrequency, Int, Long?, Boolean) -> Unit,
    onMarkPlannedExpensePaid: (Long, Long) -> Unit,
    onUpdatePlannedExpense: (com.example.nummia.model.plannedexpenses.PlannedExpense) -> Unit,
    onDeletePlannedExpense: (Long) -> Unit,
    onSnoozeReminder: (Long) -> Unit,
    onDisableReminders: (Long) -> Unit,
    onOpenPlannedExpenseDetails: (Long) -> Unit,
    onOpenOverviewStatistics: () -> Unit,
    onOpenStatistics: (TransactionType) -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var showMovementDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }
    var showPlannedDialog by remember { mutableStateOf(false) }
    val monthlyIncome = state.movements.filter {
        it.type == TransactionType.INCOME && it.timestamp.isInCurrentMonth()
    }.sumOf { it.amountCents }

    ScreenColumn {
        ScreenHeading("Hola, ¿cómo van tus finanzas?", "Tu resumen de un vistazo") {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenOverviewStatistics)
        ) {
            Column(Modifier.padding(22.dp)) {
                Text("SALDO ACTUAL", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Text(formatMoney(state.balanceCents, currencyCode), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Saldo inicial: ${formatMoney(state.initialBalanceCents, currencyCode)}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showBalanceDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryAmountCard(
                "Ingresos", monthlyIncome, true, Modifier.weight(1f),
                onClick = { onOpenStatistics(TransactionType.INCOME) }
            )
            SummaryAmountCard(
                "Gastos", budgetSummary.monthlySpentCents, false, Modifier.weight(1f),
                onClick = { onOpenStatistics(TransactionType.EXPENSE) }
            )
        }

        if (state.weeklyBudgetCents > 0 || state.monthlyBudgetCents > 0) {
            SectionTitle("Presupuestos")
            if (state.weeklyBudgetCents > 0) {
                BudgetProgressCard(
                    "Esta semana",
                    budgetSummary.weeklySpentCents,
                    state.weeklyBudgetCents,
                    "Quedan ${formatMoney((state.weeklyBudgetCents - budgetSummary.weeklySpentCents).coerceAtLeast(0), currencyCode)}"
                )
            }
            if (state.monthlyBudgetCents > 0) {
                BudgetProgressCard(
                    "Este mes",
                    budgetSummary.monthlySpentCents,
                    state.monthlyBudgetCents,
                    "Quedan ${formatMoney((state.monthlyBudgetCents - budgetSummary.monthlySpentCents).coerceAtLeast(0), currencyCode)}"
                )
            }
        }

        SectionTitle("Próximos pagos") {
            TextButton(onClick = { showPlannedDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Añadir")
            }
        }
        BillRemindersNotice(
            expenses = state.plannedExpenses,
            enabled = state.billRemindersEnabled,
            monthlyLeadDays = state.monthlyReminderDaysBefore,
            weeklyLeadDays = state.weeklyReminderDaysBefore,
            reminderHour = state.billReminderHour,
            reminderMinute = state.billReminderMinute,
            onOpenDetails = onOpenPlannedExpenseDetails,
            onSnooze = onSnoozeReminder,
            onDisable = onDisableReminders
        )
        if (state.plannedExpenses.isEmpty()) {
            EmptyMessage("No tienes pagos previstos.")
        } else {
            state.plannedExpenses
                .sortedByDescending { it.nextDueAt > 0L && it.nextDueAt.isOverdue() }
                .take(3)
                .forEach {
                    PlannedExpenseRow(
                        it,
                        onMarkPlannedExpensePaid,
                        onUpdatePlannedExpense,
                        onDeletePlannedExpense,
                    )
                }
        }

        SectionTitle("Movimientos recientes")
        state.movements.take(4).forEach { movement ->
            MovementRow(movement, onClick = { onMovementClick(movement.id) })
        }
        Button(onClick = { showMovementDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Añadir movimiento")
        }
    }

    if (showMovementDialog) {
        MovementDialog(
            onDismiss = { showMovementDialog = false },
            onSave = { title, description, amount, category, type, timestamp ->
                onAddMovement(title, description, amount, category, type, timestamp)
                showMovementDialog = false
            }
        )
    }
    if (showBalanceDialog) {
        AmountDialog("Editar saldo inicial", state.initialBalanceCents, "Guardar", {
            onSetInitialBalance(it)
            showBalanceDialog = false
        }, { showBalanceDialog = false })
    }
    if (showPlannedDialog) {
        PlannedExpenseDialog(
            onDismiss = { showPlannedDialog = false },
            onSave = { title, description, amount, category, date, dueAt, frequency, day, until, includeUntil ->
                onAddPlannedExpense(
                    title, description, amount, category, date, dueAt, frequency, day, until, includeUntil
                )
                showPlannedDialog = false
            }
        )
    }
}
