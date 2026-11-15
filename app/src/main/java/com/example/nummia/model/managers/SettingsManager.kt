package com.example.nummia.model.managers

import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.state.AppState
import com.example.nummia.model.FinanceStateUpdater

/** Gestiona los ajustes generales y coordina su persistencia con el estado financiero. */
internal class SettingsManager(
    private val repository: FinanceRepository,
    private val updateState: FinanceStateUpdater
) {
    /** Actualiza y persiste el saldo inicial. */
    fun setInitialBalance(amountCents: Long) {
        updateState { state ->
            val updated = state.copy(initialBalanceCents = amountCents)
            repository.saveSettings(updated)
            updated
        }
    }

    /** Guarda los límites de gasto semanal y mensual. */
    fun setBudgets(weeklyCents: Long, monthlyCents: Long) {
        updateState { state ->
            val updated = state.copy(
                weeklyBudgetCents = weeklyCents,
                monthlyBudgetCents = monthlyCents
            )
            repository.saveSettings(updated)
            updated
        }
    }

    /** Cambia la preferencia global de recordatorios. */
    fun changeBillReminders(enabled: Boolean) {
        updateState { state ->
            val updated = state.copy(billRemindersEnabled = enabled)
            repository.saveSettings(updated)
            updated
        }
    }

    /** Guarda las opciones de antelación, hora, repetición y posposición. */
    fun changeBillReminderPreferences(
        monthlyDaysBefore: Int,
        weeklyDaysBefore: Int,
        hour: Int,
        minute: Int,
        repeatIntervalDays: Int,
        snoozeHours: Int
    ) {
        require(monthlyDaysBefore in 1..30)
        require(weeklyDaysBefore in 1..7)
        require(hour in 0..23 && minute in 0..59)
        require(repeatIntervalDays in 1..7)
        require(snoozeHours in 1..168)
        updateState { state ->
            val updated = state.copy(
                monthlyReminderDaysBefore = monthlyDaysBefore,
                weeklyReminderDaysBefore = weeklyDaysBefore,
                billReminderHour = hour,
                billReminderMinute = minute,
                overdueReminderIntervalDays = repeatIntervalDays,
                reminderSnoozeHours = snoozeHours
            )
            repository.saveSettings(updated)
            updated
        }
    }

    /** Activa o desactiva únicamente las notificaciones del sistema. */
    fun changeBillPushNotifications(enabled: Boolean) {
        updateState { state ->
            val updated = state.copy(billPushNotificationsEnabled = enabled)
            repository.saveSettings(updated)
            updated
        }
    }

    /** Cambia moneda conservando los valores numéricos y redondeando a su precisión. */
    fun changeCurrency(currencyCode: String) {
        updateState { state -> repository.changeCurrency(state, currencyCode) }
    }

    /** Cambia la paleta visual y la persiste en la base de datos. */
    fun changeThemePalette(themePalette: String) {
        updateState { state ->
            val updated = state.copy(themePalette = themePalette)
            repository.saveSettings(updated)
            updated
        }
    }
}
