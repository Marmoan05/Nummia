package com.example.nummia.views.settings

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading

/** Agrupa las preferencias generales, visuales y de notificaciones. */
@Composable
fun SettingsScreen(
    currencyCode: String,
    selectedTheme: AppThemePalette,
    billRemindersEnabled: Boolean,
    billPushNotificationsEnabled: Boolean,
    autoUpdateChecksEnabled: Boolean,
    monthlyReminderDaysBefore: Int,
    weeklyReminderDaysBefore: Int,
    billReminderHour: Int,
    billReminderMinute: Int,
    overdueReminderIntervalDays: Int,
    reminderSnoozeHours: Int,
    onChangeCurrency: (String) -> Unit,
    onChangeTheme: (String) -> Unit,
    onChangeBillReminders: (Boolean) -> Unit,
    onChangeBillPushNotifications: (Boolean) -> Unit,
    onChangeAutoUpdateChecks: (Boolean) -> Unit,
    onChangeReminderPreferences: (Int, Int, Int, Int, Int, Int) -> Unit,
    onOpenReleaseNotes: () -> Unit,
    onBack: () -> Unit
) {
    ScreenColumn {
        TextButton(onClick = onBack) {
            Text("‹  Volver")
        }
        ScreenHeading("Ajustes", "Personaliza tu aplicación")

        SettingsCard("Apariencia y moneda") {
            SettingsGeneral(
                currencyCode = currencyCode,
                onChangeCurrency = onChangeCurrency
            )
            HorizontalDivider()
            SettingsTheme(
                selectedTheme = selectedTheme,
                onChangeTheme = onChangeTheme
            )
        }

        SettingsNotifications(
            billRemindersEnabled = billRemindersEnabled,
            billPushNotificationsEnabled = billPushNotificationsEnabled,
            monthlyReminderDaysBefore = monthlyReminderDaysBefore,
            weeklyReminderDaysBefore = weeklyReminderDaysBefore,
            billReminderHour = billReminderHour,
            billReminderMinute = billReminderMinute,
            overdueReminderIntervalDays = overdueReminderIntervalDays,
            reminderSnoozeHours = reminderSnoozeHours,
            onChangeBillReminders = onChangeBillReminders,
            onChangeBillPushNotifications = onChangeBillPushNotifications,
            onChangeReminderPreferences = onChangeReminderPreferences
        )

        SettingsUpdates(
            autoUpdateChecksEnabled = autoUpdateChecksEnabled,
            onChangeAutoUpdateChecks = onChangeAutoUpdateChecks,
            onOpenReleaseNotes = onOpenReleaseNotes
        )
    }
}
