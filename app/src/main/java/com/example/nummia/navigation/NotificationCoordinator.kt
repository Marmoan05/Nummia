package com.example.nummia.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.nummia.model.state.AppState
import com.example.nummia.notifications.BillReminderCalculator
import com.example.nummia.notifications.BillReminderScheduler

/** Coordina permisos, programación y avisos de facturas, además de los errores globales. */
@Composable
internal fun NotificationCoordinator(
    context: Context,
    appState: AppState,
    isInitialized: Boolean,
    expenseToOpenId: Long?,
    error: String?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit,
    onStartupRemindersChanged: (Boolean) -> Unit
) {
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var startupReminderChecked by remember { mutableStateOf(false) }

    LaunchedEffect(
        isInitialized,
        appState.billRemindersEnabled,
        appState.billPushNotificationsEnabled
    ) {
        if (isInitialized && appState.onboardingCompleted && appState.billRemindersEnabled &&
            appState.billPushNotificationsEnabled
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!notificationPermissionGranted) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    LaunchedEffect(
        isInitialized,
        appState.onboardingCompleted,
        appState.plannedExpenses,
        appState.billRemindersEnabled,
        appState.billPushNotificationsEnabled,
        appState.monthlyReminderDaysBefore,
        appState.weeklyReminderDaysBefore,
        appState.billReminderHour,
        appState.billReminderMinute,
        appState.overdueReminderIntervalDays
    ) {
        if (!isInitialized) return@LaunchedEffect

        BillReminderScheduler.scheduleAll(
            context,
            appState.plannedExpenses,
            appState.onboardingCompleted && appState.billRemindersEnabled &&
                appState.billPushNotificationsEnabled,
            monthlyLeadDays = appState.monthlyReminderDaysBefore,
            weeklyLeadDays = appState.weeklyReminderDaysBefore,
            reminderHour = appState.billReminderHour,
            reminderMinute = appState.billReminderMinute,
            repeatIntervalDays = appState.overdueReminderIntervalDays
        )
    }

    LaunchedEffect(isInitialized, expenseToOpenId) {
        if (isInitialized && appState.onboardingCompleted && !startupReminderChecked) {
            startupReminderChecked = true
            onStartupRemindersChanged(
                expenseToOpenId == null &&
                    BillReminderCalculator.remindersToShowInApp(
                        appState.plannedExpenses,
                        System.currentTimeMillis(),
                        appState.billRemindersEnabled,
                        appState.monthlyReminderDaysBefore,
                        appState.weeklyReminderDaysBefore,
                        appState.billReminderHour,
                        appState.billReminderMinute
                    ).isNotEmpty()
            )
        }
    }
}
