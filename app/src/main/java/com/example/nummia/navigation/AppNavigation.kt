package com.example.nummia.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import com.example.nummia.controller.BalanceController
import com.example.nummia.controller.BudgetController
import com.example.nummia.controller.MovementController
import com.example.nummia.controller.PlannedExpenseController
import com.example.nummia.controller.StatisticsController
import com.example.nummia.database.FinanceDatabase
import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.model.FinanceModel
import com.example.nummia.ui.theme.MyApplicationTheme
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.plannedexpenses.BillRemindersDialog
import com.example.nummia.views.settings.FirstRunSetupScreen

/** Construye la base Room, conecta el modelo y presenta las pantallas navegables. */
@Composable
fun AppNavigation(
    expenseToOpenId: Long? = null,
    pendingCrashReport: String? = null
) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) { FinanceDatabase.open(context) }
    val model = remember {
        FinanceModel(FinanceRepository(database))
    }
    val appState by model.state.collectAsStateWithLifecycle()
    val isInitialized by model.isInitialized.collectAsStateWithLifecycle()
    val initializationError by model.initializationError.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var crashReport by remember(pendingCrashReport) {
        mutableStateOf(pendingCrashReport)
    }
    var splashMinimumElapsed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        splashMinimumElapsed = true
    }
    var selectedExpenseId by remember(expenseToOpenId) { mutableStateOf(expenseToOpenId) }
    var showStartupReminders by remember { mutableStateOf(false) }
    var onboardingTheme by remember(appState.themePalette) {
        mutableStateOf(appState.themePalette)
    }
    val selectedPalette = AppThemePalette.from(appState.themePalette)
    NotificationCoordinator(
        context = context,
        appState = appState,
        isInitialized = isInitialized,
        expenseToOpenId = expenseToOpenId,
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = model::clearError,
        onStartupRemindersChanged = { showStartupReminders = it }
    )
    AppUpdateCoordinator(
        enabled = isInitialized && appState.onboardingCompleted &&
            appState.autoUpdateChecksEnabled,
        readyForReleaseNotes = isInitialized && appState.onboardingCompleted,
        snackbarHostState = snackbarHostState
    )
    val balanceController = BalanceController(model)
    val movementController = MovementController(model)
    val plannedExpenseController = PlannedExpenseController(model)
    val budgetController = BudgetController(model)
    val statisticsController = remember { StatisticsController() }
    val budgetSummary = budgetController.calculate(appState)
    val statisticsSummary = statisticsController.calculate(appState)
    val navController: NavHostController = rememberNavController()
    LaunchedEffect(expenseToOpenId) {
        if (expenseToOpenId != null) {
            navController.navigate(Screen.BUDGETS.route) { launchSingleTop = true }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.HOME.route

    MyApplicationTheme(palette = selectedPalette) {
        when {
            !isInitialized || !splashMinimumElapsed -> Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                StartupLoadingScreen(
                    modifier = Modifier.padding(innerPadding),
                    error = initializationError,
                    onRetry = model::retryInitialization
                )
            }
            !appState.onboardingCompleted -> MyApplicationTheme(
                palette = AppThemePalette.from(onboardingTheme)
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    FirstRunSetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        initialBalanceCents = appState.initialBalanceCents,
                        currencyCode = appState.currencyCode,
                        themePalette = onboardingTheme,
                        onChangeTheme = { onboardingTheme = it },
                        billRemindersEnabled = appState.billRemindersEnabled,
                        monthlyReminderDaysBefore = appState.monthlyReminderDaysBefore,
                        weeklyReminderDaysBefore = appState.weeklyReminderDaysBefore,
                        billReminderHour = appState.billReminderHour,
                        billReminderMinute = appState.billReminderMinute,
                        overdueReminderIntervalDays = appState.overdueReminderIntervalDays,
                        reminderSnoozeHours = appState.reminderSnoozeHours,
                        billPushNotificationsEnabled = appState.billPushNotificationsEnabled,
                        onCompleteSetup = {
                            balance, currency, theme, reminders, pushNotifications,
                            monthlyDays, weeklyDays, hour, minute, repeatDays, snoozeHours ->
                            model.completeFirstRunSetup(
                                balance,
                                currency,
                                theme,
                                reminders,
                                pushNotifications,
                                monthlyDays,
                                weeklyDays,
                                hour,
                                minute,
                                repeatDays,
                                snoozeHours
                            )
                        }
                    )
                }
            }
            else -> Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    AppBottomNavigation(
                        navController = navController,
                        currentRoute = currentRoute
                    )
                }
            ) { innerPadding ->
                CompositionLocalProvider(LocalCurrencyCode provides appState.currencyCode) {
                    AppNavigationHost(
                        navController = navController,
                        appState = appState,
                        model = model,
                        balanceController = balanceController,
                        movementController = movementController,
                        plannedExpenseController = plannedExpenseController,
                        budgetController = budgetController,
                        budgetSummary = budgetSummary,
                        statisticsSummary = statisticsSummary,
                        expenseToOpenId = expenseToOpenId,
                        selectedExpenseId = selectedExpenseId,
                        onSelectedExpenseChanged = { selectedExpenseId = it },
                        modifier = Modifier.padding(innerPadding)
                    )
                    if (showStartupReminders) {
                        BillRemindersDialog(
                            expenses = appState.plannedExpenses,
                            enabled = appState.billRemindersEnabled,
                            monthlyLeadDays = appState.monthlyReminderDaysBefore,
                            weeklyLeadDays = appState.weeklyReminderDaysBefore,
                            reminderHour = appState.billReminderHour,
                            reminderMinute = appState.billReminderMinute,
                            onDismiss = { showStartupReminders = false },
                            onOpenDetails = { expenseId ->
                                selectedExpenseId = expenseId
                                showStartupReminders = false
                                navController.navigate(Screen.BUDGETS.route) { launchSingleTop = true }
                            },
                            onSnooze = plannedExpenseController::snoozeReminder,
                            onDisable = plannedExpenseController::disableReminders
                        )
                    }
                }
            }
        }
        crashReport?.let { report ->
            CrashReportPrompt(
                report = report,
                snackbarHostState = snackbarHostState,
                onReportCleared = { crashReport = null }
            )
        }
    }
}
