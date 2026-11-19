package com.example.nummia.database

import androidx.room.withTransaction
import com.example.nummia.model.state.AppState
import com.example.nummia.model.settings.FinanceSettings
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.state.createInitialFinanceState
import com.example.nummia.model.settings.normalizeAmountForCurrency

/** Traduce operaciones del modelo a consultas Room y coordina la carga inicial. */
class FinanceRepository(private val database: FinanceDatabase) {
    private val dao = database.financeDao()

    /** Lee el estado persistido o crea los ajustes vacíos para la primera configuración. */
    suspend fun loadOrCreateState(): AppState = database.withTransaction {
        val settings = dao.getSettings()
        if (settings != null) {
            AppState(
                initialBalanceCents = settings.initialBalanceCents,
                movements = dao.getMovements(),
                plannedExpenses = dao.getPlannedExpenses(),
                weeklyBudgetCents = settings.weeklyBudgetCents,
                monthlyBudgetCents = settings.monthlyBudgetCents,
                currencyCode = settings.currencyCode,
                themePalette = settings.themePalette,
                billRemindersEnabled = settings.billRemindersEnabled,
                monthlyReminderDaysBefore = settings.monthlyReminderDaysBefore,
                weeklyReminderDaysBefore = settings.weeklyReminderDaysBefore,
                billReminderHour = settings.billReminderHour,
                billReminderMinute = settings.billReminderMinute,
                overdueReminderIntervalDays = settings.overdueReminderIntervalDays,
                reminderSnoozeHours = settings.reminderSnoozeHours,
                billPushNotificationsEnabled = settings.billPushNotificationsEnabled,
                autoUpdateChecksEnabled = settings.autoUpdateChecksEnabled,
                onboardingCompleted = settings.onboardingCompleted
            )
        } else {
            val initial = createInitialFinanceState()
            dao.saveSettings(
                FinanceSettings(
                    initialBalanceCents = initial.initialBalanceCents,
                    weeklyBudgetCents = initial.weeklyBudgetCents,
                    monthlyBudgetCents = initial.monthlyBudgetCents,
                    currencyCode = initial.currencyCode,
                    themePalette = initial.themePalette,
                    billRemindersEnabled = initial.billRemindersEnabled,
                    monthlyReminderDaysBefore = initial.monthlyReminderDaysBefore,
                    weeklyReminderDaysBefore = initial.weeklyReminderDaysBefore,
                    billReminderHour = initial.billReminderHour,
                    billReminderMinute = initial.billReminderMinute,
                    overdueReminderIntervalDays = initial.overdueReminderIntervalDays,
                    reminderSnoozeHours = initial.reminderSnoozeHours,
                    billPushNotificationsEnabled = initial.billPushNotificationsEnabled,
                    autoUpdateChecksEnabled = initial.autoUpdateChecksEnabled,
                    onboardingCompleted = initial.onboardingCompleted
                )
            )
            initial
        }
    }

    /** Persiste en Room el saldo inicial y los límites presupuestarios del estado. */
    suspend fun saveSettings(state: AppState) {
        dao.saveSettings(
            FinanceSettings(
                initialBalanceCents = state.initialBalanceCents,
                weeklyBudgetCents = state.weeklyBudgetCents,
                monthlyBudgetCents = state.monthlyBudgetCents,
                currencyCode = state.currencyCode,
                themePalette = state.themePalette,
                billRemindersEnabled = state.billRemindersEnabled,
                monthlyReminderDaysBefore = state.monthlyReminderDaysBefore,
                weeklyReminderDaysBefore = state.weeklyReminderDaysBefore,
                billReminderHour = state.billReminderHour,
                billReminderMinute = state.billReminderMinute,
                overdueReminderIntervalDays = state.overdueReminderIntervalDays,
                reminderSnoozeHours = state.reminderSnoozeHours,
                billPushNotificationsEnabled = state.billPushNotificationsEnabled,
                autoUpdateChecksEnabled = state.autoUpdateChecksEnabled,
                onboardingCompleted = state.onboardingCompleted
            )
        )
    }

    /** Cambia moneda sin conversión y redondea importes a la precisión de la nueva moneda. */
    suspend fun changeCurrency(state: AppState, currencyCode: String): AppState =
        database.withTransaction {
            val updated = state.copy(
                initialBalanceCents = normalizeAmountForCurrency(state.initialBalanceCents, currencyCode),
                weeklyBudgetCents = normalizeAmountForCurrency(state.weeklyBudgetCents, currencyCode),
                monthlyBudgetCents = normalizeAmountForCurrency(state.monthlyBudgetCents, currencyCode),
                movements = state.movements.map {
                    it.copy(amountCents = normalizeAmountForCurrency(it.amountCents, currencyCode))
                },
                plannedExpenses = state.plannedExpenses.map {
                    it.copy(amountCents = normalizeAmountForCurrency(it.amountCents, currencyCode))
                },
                currencyCode = currencyCode
            )
            dao.updateMovements(updated.movements)
            dao.updatePlannedExpenses(updated.plannedExpenses)
            dao.saveSettings(
                FinanceSettings(
                    initialBalanceCents = updated.initialBalanceCents,
                    weeklyBudgetCents = updated.weeklyBudgetCents,
                    monthlyBudgetCents = updated.monthlyBudgetCents,
                    currencyCode = updated.currencyCode,
                    themePalette = updated.themePalette,
                    billRemindersEnabled = updated.billRemindersEnabled,
                    monthlyReminderDaysBefore = updated.monthlyReminderDaysBefore,
                    weeklyReminderDaysBefore = updated.weeklyReminderDaysBefore,
                    billReminderHour = updated.billReminderHour,
                    billReminderMinute = updated.billReminderMinute,
                    overdueReminderIntervalDays = updated.overdueReminderIntervalDays,
                    reminderSnoozeHours = updated.reminderSnoozeHours,
                    billPushNotificationsEnabled = updated.billPushNotificationsEnabled,
                    autoUpdateChecksEnabled = updated.autoUpdateChecksEnabled,
                    onboardingCompleted = updated.onboardingCompleted
                )
            )
            updated
        }

    /** Persiste un movimiento nuevo en la tabla del historial. */
    suspend fun addMovement(movement: MoneyMovement): Long = dao.insertMovement(movement)

    /** Actualiza un movimiento existente en la tabla del historial. */
    suspend fun updateMovement(movement: MoneyMovement) = dao.updateMovement(movement)

    /** Elimina un movimiento por su identificador. */
    suspend fun deleteMovement(movementId: Long) = dao.deleteMovement(movementId)

    /** Persiste un pago previsto nuevo en la base de datos. */
    suspend fun addPlannedExpense(expense: PlannedExpense): Long =
        dao.insertPlannedExpense(expense)

    /** Actualiza una factura prevista. */
    suspend fun updatePlannedExpense(expense: PlannedExpense) = dao.updatePlannedExpense(expense)

    /** Elimina una factura prevista. */
    suspend fun deletePlannedExpense(expenseId: Long) = dao.deletePlannedExpense(expenseId)

    /** Registra el gasto y completa o programa el siguiente vencimiento en una transacción. */
    suspend fun recordPlannedExpensePayment(
        movement: MoneyMovement,
        nextExpense: PlannedExpense?,
        paidExpenseId: Long
    ): Long = database.withTransaction {
        val movementId = dao.insertMovement(movement)
        if (nextExpense == null) {
            dao.deletePlannedExpense(paidExpenseId)
        } else {
            dao.updatePlannedExpense(nextExpense)
        }
        movementId
    }
}
