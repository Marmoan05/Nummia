package com.example.nummia.views.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Configura avisos dentro de la app y del sistema, solicitando permiso cuando hace falta. */
@Composable
internal fun SettingsNotifications(
    billRemindersEnabled: Boolean,
    billPushNotificationsEnabled: Boolean,
    monthlyReminderDaysBefore: Int,
    weeklyReminderDaysBefore: Int,
    billReminderHour: Int,
    billReminderMinute: Int,
    overdueReminderIntervalDays: Int,
    reminderSnoozeHours: Int,
    onChangeBillReminders: (Boolean) -> Unit,
    onChangeBillPushNotifications: (Boolean) -> Unit,
    onChangeReminderPreferences: (Int, Int, Int, Int, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    SettingsCard("Recordatorios de facturas") {
        SettingSwitchRow(
            title = "Recordatorios dentro de la app",
            subtitle = "Avisos en Inicio y Presupuesto",
            checked = billRemindersEnabled,
            onCheckedChange = onChangeBillReminders
        )
        androidx.compose.material3.HorizontalDivider()
        CompactPreferenceRow("Mensuales") {
            ReminderChoiceDropdown(
                selectedValue = monthlyReminderDaysBefore,
                values = listOf(1, 3, 7, 14),
                valueLabel = { "$it días antes" },
                onSelected = {
                    onChangeReminderPreferences(
                        it, weeklyReminderDaysBefore, billReminderHour, billReminderMinute,
                        overdueReminderIntervalDays, reminderSnoozeHours
                    )
                }
            )
        }
        CompactPreferenceRow("Semanales") {
            ReminderChoiceDropdown(
                selectedValue = weeklyReminderDaysBefore,
                values = listOf(1, 2, 3, 7),
                valueLabel = { "$it días antes" },
                onSelected = {
                    onChangeReminderPreferences(
                        monthlyReminderDaysBefore, it, billReminderHour, billReminderMinute,
                        overdueReminderIntervalDays, reminderSnoozeHours
                    )
                }
            )
        }
        CompactPreferenceRow("Hora") {
            com.example.nummia.views.components.ReminderTimePickerButton(
                hour = billReminderHour,
                minute = billReminderMinute
            ) { hour, minute ->
                onChangeReminderPreferences(
                    monthlyReminderDaysBefore, weeklyReminderDaysBefore, hour, minute,
                    overdueReminderIntervalDays, reminderSnoozeHours
                )
            }
        }
        CompactPreferenceRow("Repetición") {
            ReminderChoiceDropdown(
                selectedValue = overdueReminderIntervalDays,
                values = listOf(1, 2, 3, 7),
                valueLabel = { if (it == 1) "Cada día" else "Cada $it días" },
                onSelected = {
                    onChangeReminderPreferences(
                        monthlyReminderDaysBefore, weeklyReminderDaysBefore,
                        billReminderHour, billReminderMinute, it, reminderSnoozeHours
                    )
                }
            )
        }
        CompactPreferenceRow("Más tarde") {
            ReminderChoiceDropdown(
                selectedValue = reminderSnoozeHours,
                values = listOf(1, 6, 12, 24, 48),
                valueLabel = { if (it == 1) "1 hora" else "$it horas" },
                onSelected = {
                    onChangeReminderPreferences(
                        monthlyReminderDaysBefore, weeklyReminderDaysBefore,
                        billReminderHour, billReminderMinute, overdueReminderIntervalDays, it
                    )
                }
            )
        }
        androidx.compose.material3.HorizontalDivider()
        SettingCheckboxRow(
            title = "Notificaciones del móvil",
            subtitle = "Avisos en el panel de notificaciones",
            checked = billPushNotificationsEnabled,
            onCheckedChange = onChangeBillPushNotifications
        )
        // Android exige permiso de ejecución desde API 33; en versiones anteriores basta con Ajustes.
        val runtimePermissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (runtimePermissionMissing || !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            TextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    if (runtimePermissionMissing) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        )
                    }
                }
            ) {
                Text("Permitir notificaciones")
            }
        }
    }
}
