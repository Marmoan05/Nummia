package com.example.nummia.views.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.model.settings.currencyMinorDigits
import com.example.nummia.model.settings.normalizeAmountForCurrency
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.formatInputAmount
import com.example.nummia.views.components.parseCents

/** Reúne las preferencias básicas antes de abrir la aplicación por primera vez. */
@Composable
fun FirstRunSetupScreen(
    modifier: Modifier = Modifier,
    initialBalanceCents: Long,
    currencyCode: String,
    themePalette: String,
    onChangeTheme: (String) -> Unit,
    billRemindersEnabled: Boolean,
    monthlyReminderDaysBefore: Int,
    weeklyReminderDaysBefore: Int,
    billReminderHour: Int,
    billReminderMinute: Int,
    overdueReminderIntervalDays: Int,
    reminderSnoozeHours: Int,
    billPushNotificationsEnabled: Boolean,
    onCompleteSetup: (Long, String, String, Boolean, Boolean, Int, Int, Int, Int, Int, Int) -> Unit
) {
    var balanceInput by remember(initialBalanceCents) {
        mutableStateOf(formatInputAmount(initialBalanceCents, currencyCode))
    }
    var selectedCurrency by remember(currencyCode) { mutableStateOf(currencyCode) }
    var remindersEnabled by remember(billRemindersEnabled) {
        mutableStateOf(billRemindersEnabled)
    }
    var validationRequested by remember { mutableStateOf(false) }
    var monthlyDaysBefore by remember(monthlyReminderDaysBefore) {
        mutableStateOf(monthlyReminderDaysBefore)
    }
    var weeklyDaysBefore by remember(weeklyReminderDaysBefore) {
        mutableStateOf(weeklyReminderDaysBefore)
    }
    var reminderHour by remember(billReminderHour) { mutableStateOf(billReminderHour) }
    var reminderMinute by remember(billReminderMinute) { mutableStateOf(billReminderMinute) }
    var repeatIntervalDays by remember(overdueReminderIntervalDays) {
        mutableStateOf(overdueReminderIntervalDays)
    }
    var snoozeHours by remember(reminderSnoozeHours) { mutableStateOf(reminderSnoozeHours) }
    var pushNotificationsEnabled by remember(billPushNotificationsEnabled) {
        mutableStateOf(billPushNotificationsEnabled)
    }
    val parsedBalance = parseCents(balanceInput, selectedCurrency)
    val balanceError = initialBalanceError(balanceInput, selectedCurrency)

    CompositionLocalProvider(LocalCurrencyCode provides selectedCurrency) {
        ScreenColumn(modifier = modifier) {
            ScreenHeading(
                title = "¡Bienvenido a Nummia!",
                subtitle = "Personaliza tu cuenta antes de empezar."
            )
            Text(
                "Estos ajustes se guardan en este dispositivo y podrás cambiarlos más adelante.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SettingsCard("Saldo inicial") {
                Text(
                    "Indica cuánto dinero tenías al empezar. Puedes dejarlo en cero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Importe ($selectedCurrency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = validationRequested && balanceError != null,
                    supportingText = {
                        if (validationRequested && balanceError != null) {
                            Text(balanceError, color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            SettingsCard("Apariencia y moneda") {
                SettingsGeneral(
                    currencyCode = selectedCurrency,
                    onChangeCurrency = { newCurrency ->
                        val currentBalance = parseCents(balanceInput, selectedCurrency)
                        selectedCurrency = newCurrency
                        if (currentBalance != null) {
                            balanceInput = formatInputAmount(
                                normalizeAmountForCurrency(currentBalance, newCurrency),
                                newCurrency
                            )
                        }
                    }
                )
                androidx.compose.material3.HorizontalDivider()
                SettingsTheme(
                    selectedTheme = AppThemePalette.from(themePalette),
                    onChangeTheme = onChangeTheme
                )
            }

            SettingsNotifications(
                billRemindersEnabled = remindersEnabled,
                billPushNotificationsEnabled = pushNotificationsEnabled,
                monthlyReminderDaysBefore = monthlyDaysBefore,
                weeklyReminderDaysBefore = weeklyDaysBefore,
                billReminderHour = reminderHour,
                billReminderMinute = reminderMinute,
                overdueReminderIntervalDays = repeatIntervalDays,
                reminderSnoozeHours = snoozeHours,
                onChangeBillReminders = { remindersEnabled = it },
                onChangeBillPushNotifications = { pushNotificationsEnabled = it },
                onChangeReminderPreferences = { monthly, weekly, hour, minute, repeat, snooze ->
                    monthlyDaysBefore = monthly
                    weeklyDaysBefore = weekly
                    reminderHour = hour
                    reminderMinute = minute
                    repeatIntervalDays = repeat
                    snoozeHours = snooze
                }
            )

            Button(
                onClick = {
                    validationRequested = true
                    if (balanceError == null && parsedBalance != null) {
                        onCompleteSetup(
                            parsedBalance,
                            selectedCurrency,
                            themePalette,
                            remindersEnabled,
                            pushNotificationsEnabled,
                            monthlyDaysBefore,
                            weeklyDaysBefore,
                            reminderHour,
                            reminderMinute,
                            repeatIntervalDays,
                            snoozeHours
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Empezar")
            }
        }
    }
}

private fun initialBalanceError(input: String, currencyCode: String): String? {
    val value = input.trim()
    if (value.isEmpty()) {
        return "El saldo inicial es obligatorio. Escribe 0 si quieres empezar sin saldo."
    }
    if (value.startsWith("-")) {
        return "El saldo inicial no puede ser negativo."
    }

    val fractionDigits = currencyMinorDigits(currencyCode)
    val pattern = if (fractionDigits == 0) Regex("""\d+""")
    else Regex("""\d+([.,]\d{1,$fractionDigits})?""")
    if (!pattern.matches(value)) {
        return if (fractionDigits == 0) {
            "Introduce un importe válido en $currencyCode, sin decimales."
        } else {
            "Introduce un importe válido en $currencyCode, con hasta $fractionDigits decimales."
        }
    }
    if (parseCents(value, currencyCode) == null) {
        return "El importe es demasiado grande."
    }
    return null
}
