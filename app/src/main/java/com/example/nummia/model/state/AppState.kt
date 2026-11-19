package com.example.nummia.model.state

import com.example.nummia.calculations.BalanceCalculator
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.model.transactions.MoneyMovement

/** Agrupa las finanzas, preferencias de la app y el estado de la configuración inicial. */
data class AppState(
    val initialBalanceCents: Long,
    val movements: List<MoneyMovement>,
    val plannedExpenses: List<PlannedExpense>,
    val weeklyBudgetCents: Long,
    val monthlyBudgetCents: Long,
    val currencyCode: String = "EUR",
    val themePalette: String = AppThemePalette.DEFAULT.id,
    val billRemindersEnabled: Boolean = true,
    val monthlyReminderDaysBefore: Int = 7,
    val weeklyReminderDaysBefore: Int = 2,
    val billReminderHour: Int = 9,
    val billReminderMinute: Int = 0,
    val overdueReminderIntervalDays: Int = 1,
    val reminderSnoozeHours: Int = 24,
    val billPushNotificationsEnabled: Boolean = true,
    val autoUpdateChecksEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
) {
    /** Devuelve el saldo calculado sumando ingresos y restando gastos al saldo inicial. */
    val balanceCents: Long
        get() = BalanceCalculator.calculate(this)
}
