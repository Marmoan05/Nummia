package com.example.nummia.model.managers

import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.state.AppState
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.FinanceStateUpdater
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.transactions.TransactionType

/** Gestiona facturas previstas, recurrencias, recordatorios y registro de pagos. */
internal class PlannedExpenseManager(
    private val repository: FinanceRepository,
    private val updateState: FinanceStateUpdater
) {
    /** Crea una factura pendiente, opcionalmente recurrente, sin descontarla del saldo. */
    fun addPlannedExpense(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        dueDate: String,
        dueAt: Long,
        frequency: BillingFrequency,
        billingDay: Int,
        repeatUntilAt: Long?,
        includeUntilPeriod: Boolean
    ) {
        updateState { state ->
            val nextDueAt = if (frequency == BillingFrequency.NONE) {
                require(dueAt > 0L) { "Selecciona la fecha de vencimiento." }
                dueAt
            } else {
                PlannedExpenseSchedule.requireValidBillingDay(frequency, billingDay)
                PlannedExpenseSchedule.nextBillingDate(
                    frequency,
                    billingDay,
                    System.currentTimeMillis(),
                    includeToday = true
                )
            }
            require(
                repeatUntilAt == null ||
                    (frequency != BillingFrequency.NONE &&
                        PlannedExpenseSchedule.isAllowedPeriod(
                            nextDueAt,
                            repeatUntilAt,
                            frequency,
                            includeUntilPeriod
                        ))
            ) { "La fecha de fin no permite ningún cobro con la frecuencia elegida."             }
            val expense = PlannedExpense(
                title = title,
                description = description,
                amountCents = amountCents,
                dueDate = if (frequency == BillingFrequency.NONE) dueDate
                else PlannedExpenseSchedule.formatBillingDate(nextDueAt),
                category = category,
                frequency = frequency,
                billingDay = billingDay,
                nextDueAt = nextDueAt,
                repeatUntilAt = repeatUntilAt,
                includeUntilPeriod = includeUntilPeriod
            )
            val savedExpense = expense.copy(id = repository.addPlannedExpense(expense))
            state.copy(plannedExpenses = listOf(savedExpense) + state.plannedExpenses)
        }
    }

    /** Actualiza la descripción, el importe y la programación de una factura pendiente. */
    fun updatePlannedExpense(expense: PlannedExpense) {
        updateState { state ->
            val current = state.plannedExpenses.firstOrNull { it.id == expense.id }
                ?: error("La factura pendiente ya no existe.")
            val nextDueAt = if (expense.frequency == BillingFrequency.NONE) {
                require(expense.nextDueAt > 0L) { "Selecciona la fecha de vencimiento." }
                expense.nextDueAt
            } else if (
                expense.frequency == current.frequency &&
                expense.billingDay == current.billingDay &&
                current.nextDueAt > 0L
            ) {
                // Editar el resto de datos no debe adelantar el vencimiento ya programado.
                current.nextDueAt
            } else {
                PlannedExpenseSchedule.requireValidBillingDay(
                    expense.frequency,
                    expense.billingDay
                )
                PlannedExpenseSchedule.nextBillingDate(
                    expense.frequency,
                    expense.billingDay,
                    System.currentTimeMillis(),
                    includeToday = true
                )
            }
            require(
                expense.repeatUntilAt == null ||
                    (expense.frequency != BillingFrequency.NONE &&
                        PlannedExpenseSchedule.isAllowedPeriod(
                            nextDueAt,
                            expense.repeatUntilAt,
                            expense.frequency,
                            expense.includeUntilPeriod
                        ))
            ) { "La fecha de fin no permite ningún cobro con la frecuencia elegida." }
            val updated = current.copy(
                title = expense.title,
                description = expense.description,
                amountCents = expense.amountCents,
                dueDate = if (expense.frequency == BillingFrequency.NONE) expense.dueDate
                else PlannedExpenseSchedule.formatBillingDate(nextDueAt),
                category = expense.category,
                frequency = expense.frequency,
                billingDay = expense.billingDay,
                nextDueAt = nextDueAt,
                repeatUntilAt = expense.repeatUntilAt,
                includeUntilPeriod = expense.includeUntilPeriod,
                remindersDisabled = expense.remindersDisabled,
                reminderSnoozedUntil = expense.reminderSnoozedUntil
            )
            repository.updatePlannedExpense(updated)
            state.copy(
                plannedExpenses = state.plannedExpenses.map {
                    if (it.id == expense.id) updated else it
                }
            )
        }
    }

    /** Elimina una factura pendiente de la base y del estado visible. */
    fun deletePlannedExpense(expenseId: Long) {
        updateState { state ->
            repository.deletePlannedExpense(expenseId)
            state.copy(plannedExpenses = state.plannedExpenses.filterNot { it.id == expenseId })
        }
    }

    /** Pospone los avisos de una factura durante el intervalo configurado. */
    fun snoozePlannedExpenseReminder(expenseId: Long) {
        updateReminderPreferences(expenseId) { expense, snoozeHours ->
            expense.copy(
                reminderSnoozedUntil = System.currentTimeMillis() + snoozeHours * 60 * 60 * 1000L
            )
        }
    }

    /** Desactiva los recordatorios de una factura sin eliminarla. */
    fun disablePlannedExpenseReminders(expenseId: Long) {
        updateReminderPreferences(expenseId) { expense, _ -> expense.copy(remindersDisabled = true) }
    }

    /** Persiste el cambio de recordatorio y deja el estado intacto si la factura ya no existe. */
    private fun updateReminderPreferences(
        expenseId: Long,
        transform: (PlannedExpense, Int) -> PlannedExpense
    ) {
        updateState { state ->
            val current = state.plannedExpenses.firstOrNull { it.id == expenseId }
                ?: return@updateState state
            val updated = transform(current, state.reminderSnoozeHours)
            repository.updatePlannedExpense(updated)
            state.copy(
                plannedExpenses = state.plannedExpenses.map {
                    if (it.id == expenseId) updated else it
                }
            )
        }
    }

    /** Registra el pago como gasto y elimina o programa la siguiente factura recurrente. */
    fun markPlannedExpensePaid(expenseId: Long, extraCents: Long) {
        updateState { state ->
            require(extraCents >= 0) { "El extra no puede ser negativo." }
            val current = state.plannedExpenses.firstOrNull { it.id == expenseId }
                ?: error("La factura pendiente ya no existe.")
            val totalCents = Math.addExact(current.amountCents, extraCents)
            val now = System.currentTimeMillis()
            val nextDueAt = if (current.frequency == BillingFrequency.NONE) {
                null
            } else {
                // Si la factura estaba atrasada, un pago no genera una cadena de pagos omitidos.
                PlannedExpenseSchedule.nextBillingDate(
                    current.frequency,
                    current.billingDay,
                    maxOf(now, current.nextDueAt),
                    includeToday = false
                ).takeIf {
                    current.repeatUntilAt == null ||
                        PlannedExpenseSchedule.isAllowedPeriod(
                            it,
                            current.repeatUntilAt,
                            current.frequency,
                            current.includeUntilPeriod
                        )
                }
            }
            val nextExpense = nextDueAt?.let {
                current.copy(
                    nextDueAt = it,
                    dueDate = PlannedExpenseSchedule.formatBillingDate(it),
                    remindersDisabled = false,
                    reminderSnoozedUntil = 0L
                )
            }
            val movement = MoneyMovement(
                title = current.title,
                description = when {
                    !current.description.isNullOrBlank() && extraCents > 0 ->
                        "${current.description} · Pago con extra por retraso."
                    !current.description.isNullOrBlank() -> current.description
                    extraCents > 0 -> "Pago previsto con extra por retraso."
                    else -> "Pago previsto."
                },
                amountCents = totalCents,
                category = current.category,
                type = TransactionType.EXPENSE,
                timestamp = now
            )
            val savedMovement = movement.copy(
                id = repository.recordPlannedExpensePayment(movement, nextExpense, current.id)
            )
            state.copy(
                movements = (listOf(savedMovement) + state.movements)
                    .sortedByDescending { it.timestamp },
                plannedExpenses = state.plannedExpenses.mapNotNull {
                    when {
                        it.id != current.id -> it
                        nextExpense != null -> nextExpense
                        else -> null
                    }
                }
            )
        }
    }
}
