package com.example.nummia.views.budgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import com.example.nummia.views.components.BudgetProgressCard
import com.example.nummia.views.components.BudgetDialog
import com.example.nummia.views.components.EmptyMessage
import com.example.nummia.views.components.plannedexpenses.PlannedExpenseDialog
import com.example.nummia.views.components.plannedexpenses.PlannedExpenseRow
import com.example.nummia.views.components.plannedexpenses.BillRemindersNotice
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.SectionTitle
import com.example.nummia.views.components.formatMoney
import com.example.nummia.views.components.isOverdue

/** Permite consultar los presupuestos y gestionar límites y pagos pendientes. */
@Composable
fun BudgetsScreen(
    state: AppState,
    budgetSummary: BudgetSummary,
    onSetBudgets: (Long, Long) -> Unit,
    onAddPlannedExpense: (String, String?, Long, String, String, Long, BillingFrequency, Int, Long?, Boolean) -> Unit,
    onMarkPlannedExpensePaid: (Long, Long) -> Unit,
    onUpdatePlannedExpense: (com.example.nummia.model.plannedexpenses.PlannedExpense) -> Unit,
    onDeletePlannedExpense: (Long) -> Unit,
    onSnoozeReminder: (Long) -> Unit,
    onDisableReminders: (Long) -> Unit,
    expenseToOpenId: Long?,
    onDetailsOpened: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showPlannedDialog by remember { mutableStateOf(false) }
    var requestedDetailsId by remember(expenseToOpenId) { mutableStateOf(expenseToOpenId) }
    ScreenColumn {
        ScreenHeading("Presupuestos", "Planifica y controla tus límites") {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Límites de gasto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { showBudgetDialog = true }) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Text("Editar")
            }
        }
        BudgetProgressCard(
            "Presupuesto semanal", budgetSummary.weeklySpentCents, state.weeklyBudgetCents,
            if (state.weeklyBudgetCents > 0) {
                "Gastado ${formatMoney(budgetSummary.weeklySpentCents, currencyCode)} de ${formatMoney(state.weeklyBudgetCents, currencyCode)}"
            } else {
                "Gastado ${formatMoney(budgetSummary.weeklySpentCents, currencyCode)}"
            }
        )
        BudgetProgressCard(
            "Presupuesto mensual", budgetSummary.monthlySpentCents, state.monthlyBudgetCents,
            if (state.monthlyBudgetCents > 0) {
                "Gastado ${formatMoney(budgetSummary.monthlySpentCents, currencyCode)} de ${formatMoney(state.monthlyBudgetCents, currencyCode)}"
            } else {
                "Gastado ${formatMoney(budgetSummary.monthlySpentCents, currencyCode)}"
            }
        )

        SectionTitle("Pagos pendientes") {
            FilledTonalButton(onClick = { showPlannedDialog = true }) {
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
            onOpenDetails = { requestedDetailsId = it },
            onSnooze = onSnoozeReminder,
            onDisable = onDisableReminders
        )
        if (state.plannedExpenses.isEmpty()) EmptyMessage("No tienes pagos previstos.")
        state.plannedExpenses
            .sortedByDescending { it.nextDueAt > 0L && it.nextDueAt.isOverdue() }
            .forEach { expense ->
                PlannedExpenseRow(
                    expense,
                    onMarkPlannedExpensePaid,
                    onUpdatePlannedExpense,
                    onDeletePlannedExpense,
                    openDetailsOnStart = expense.id == requestedDetailsId,
                    onDetailsOpened = {
                        if (requestedDetailsId == expense.id) {
                            requestedDetailsId = null
                            onDetailsOpened(expense.id)
                        }
                    }
                )
            }
        if (state.plannedExpenses.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "Total previsto: ${formatMoney(state.plannedExpenses.sumOf { it.amountCents }, currencyCode)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
    if (showBudgetDialog) {
        BudgetDialog(
            state.weeklyBudgetCents,
            state.monthlyBudgetCents,
            onSave = { weekly, monthly ->
                onSetBudgets(weekly, monthly)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false }
        )
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
