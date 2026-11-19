package com.example.nummia.model.settings

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/** Entidad Room con el saldo inicial y los presupuestos semanales y mensuales. */
@Entity(tableName = "account_settings")
data class FinanceSettings(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "initial_balance_cents")
    val initialBalanceCents: Long,
    @ColumnInfo(name = "weekly_budget_cents")
    val weeklyBudgetCents: Long,
    @ColumnInfo(name = "monthly_budget_cents")
    val monthlyBudgetCents: Long,
    @ColumnInfo(name = "currency_code", defaultValue = "'EUR'")
    val currencyCode: String = "EUR",
    @ColumnInfo(name = "theme_palette", defaultValue = "'default'")
    val themePalette: String = AppThemePalette.DEFAULT.id,
    @ColumnInfo(name = "bill_reminders_enabled", defaultValue = "1")
    val billRemindersEnabled: Boolean = true,
    @ColumnInfo(name = "monthly_reminder_days_before", defaultValue = "7")
    val monthlyReminderDaysBefore: Int = 7,
    @ColumnInfo(name = "weekly_reminder_days_before", defaultValue = "2")
    val weeklyReminderDaysBefore: Int = 2,
    @ColumnInfo(name = "bill_reminder_hour", defaultValue = "9")
    val billReminderHour: Int = 9,
    @ColumnInfo(name = "bill_reminder_minute", defaultValue = "0")
    val billReminderMinute: Int = 0,
    @ColumnInfo(name = "overdue_reminder_interval_days", defaultValue = "1")
    val overdueReminderIntervalDays: Int = 1,
    @ColumnInfo(name = "reminder_snooze_hours", defaultValue = "24")
    val reminderSnoozeHours: Int = 24,
    @ColumnInfo(name = "bill_push_notifications_enabled", defaultValue = "1")
    val billPushNotificationsEnabled: Boolean = true,
    @ColumnInfo(name = "auto_update_checks_enabled", defaultValue = "1")
    val autoUpdateChecksEnabled: Boolean = true,
    @ColumnInfo(name = "onboarding_completed", defaultValue = "0")
    val onboardingCompleted: Boolean = false
) {
    /** Define el identificador fijo de la única fila de ajustes de la aplicación. */
    companion object {
        const val SINGLETON_ID = 1
    }
}
