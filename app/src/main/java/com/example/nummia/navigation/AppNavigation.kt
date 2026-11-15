package com.example.nummia.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.nummia.controller.BalanceController
import com.example.nummia.controller.BudgetController
import com.example.nummia.controller.MovementController
import com.example.nummia.controller.PlannedExpenseController
import com.example.nummia.controller.StatisticsController
import com.example.nummia.calculations.CategoryChartPeriod
import com.example.nummia.calculations.CategoryStatisticsCalculator
import com.example.nummia.calculations.TransactionStatisticsCalculator
import com.example.nummia.database.FinanceDatabase
import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.model.FinanceModel
import com.example.nummia.ui.theme.MyApplicationTheme
import com.example.nummia.views.budgets.BudgetsScreen
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.plannedexpenses.BillRemindersDialog
import com.example.nummia.views.history.HistoryScreen
import com.example.nummia.views.detail.TransactionDetailScreen
import com.example.nummia.views.home.HomeScreen
import com.example.nummia.views.settings.SettingsScreen
import com.example.nummia.views.statistics.StatisticsScreen
import com.example.nummia.views.statistics.CategoryStatisticsScreen
import com.example.nummia.views.statistics.TransactionStatisticsScreen

private const val TRANSACTION_DETAIL_ROUTE = "transaction/{movementId}"
private const val CATEGORY_STATISTICS_ROUTE = "category-statistics/{category}"
private const val TRANSACTION_STATISTICS_ROUTE = "transaction-statistics/{type}/{comparison}"
private const val OVERVIEW_STATISTICS_ROUTE = "overview-statistics"
private const val SETTINGS_ROUTE = "settings"

/** Construye la base Room, conecta el modelo y presenta las pantallas navegables. */
@Composable
fun AppNavigation(expenseToOpenId: Long? = null) {
    val context = LocalContext.current.applicationContext
    val database = remember(context) { FinanceDatabase.open(context) }
    val model = remember {
        FinanceModel(FinanceRepository(database))
    }
    val appState by model.state.collectAsStateWithLifecycle()
    val isInitialized by model.isInitialized.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedExpenseId by remember(expenseToOpenId) { mutableStateOf(expenseToOpenId) }
    var showStartupReminders by remember { mutableStateOf(false) }
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
    val bottomBarRoutes = setOf(
        Screen.HOME.route,
        Screen.HISTORY.route,
        Screen.BUDGETS.route,
        Screen.STATISTICS.route
    )

    MyApplicationTheme(palette = selectedPalette) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentRoute in bottomBarRoutes) {
                    NavigationBar {
                        Screen.entries.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.label) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }

                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            CompositionLocalProvider(LocalCurrencyCode provides appState.currencyCode) {
                NavHost(
                    navController = navController,
                    startDestination = if (expenseToOpenId == null) {
                        Screen.HOME.route
                    } else {
                        Screen.BUDGETS.route
                    },
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.HOME.route) {
                        HomeScreen(
                            state = appState,
                            budgetSummary = budgetSummary,
                            onAddMovement = movementController::addMovement,
                            onMovementClick = { movementId ->
                                navController.navigate("transaction/$movementId")
                            },
                            onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                            onSetInitialBalance = balanceController::setInitialBalance,
                            onAddPlannedExpense = plannedExpenseController::addPlannedExpense,
                            onMarkPlannedExpensePaid = plannedExpenseController::markPlannedExpensePaid,
                            onUpdatePlannedExpense = plannedExpenseController::updatePlannedExpense,
                            onDeletePlannedExpense = plannedExpenseController::deletePlannedExpense,
                            onSnoozeReminder = plannedExpenseController::snoozeReminder,
                            onDisableReminders = plannedExpenseController::disableReminders,
                            onOpenPlannedExpenseDetails = { expenseId ->
                                selectedExpenseId = expenseId
                                navController.navigate(Screen.BUDGETS.route) { launchSingleTop = true }
                            },
                            onOpenOverviewStatistics = {
                                navController.navigate(OVERVIEW_STATISTICS_ROUTE)
                            },
                            onOpenStatistics = { type ->
                                navController.navigate("transaction-statistics/${type.name}/false")
                            }
                        )
                    }
                    composable(Screen.HISTORY.route) {
                        HistoryScreen(
                            movements = appState.movements,
                            onAddMovement = movementController::addMovement,
                            onMovementClick = { movementId ->
                                navController.navigate("transaction/$movementId")
                            },
                            onOpenSettings = { navController.navigate(SETTINGS_ROUTE) }
                        )
                    }
                    composable(Screen.BUDGETS.route) {
                        BudgetsScreen(
                            state = appState,
                            budgetSummary = budgetSummary,
                            onSetBudgets = budgetController::setBudgets,
                            onAddPlannedExpense = plannedExpenseController::addPlannedExpense,
                            onMarkPlannedExpensePaid = plannedExpenseController::markPlannedExpensePaid,
                            onUpdatePlannedExpense = plannedExpenseController::updatePlannedExpense,
                            onDeletePlannedExpense = plannedExpenseController::deletePlannedExpense,
                            onSnoozeReminder = plannedExpenseController::snoozeReminder,
                            onDisableReminders = plannedExpenseController::disableReminders,
                            expenseToOpenId = selectedExpenseId,
                            onDetailsOpened = { expenseId ->
                                if (selectedExpenseId == expenseId) selectedExpenseId = null
                            },
                            onOpenSettings = { navController.navigate(SETTINGS_ROUTE) }
                        )
                    }
                    composable(Screen.STATISTICS.route) {
                        StatisticsScreen(
                            summary = statisticsSummary,
                            onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                            onCategoryClick = { category ->
                                navController.navigate("category-statistics/${Uri.encode(category)}")
                            },
                            onOpenStatistics = { type ->
                                navController.navigate("transaction-statistics/${type.name}/false")
                            },
                            onOpenComparison = {
                                navController.navigate(
                                    "transaction-statistics/${com.example.nummia.model.transactions.TransactionType.EXPENSE.name}/true"
                                )
                            }
                        )
                    }
                    composable(
                        route = TRANSACTION_STATISTICS_ROUTE,
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("comparison") { type = NavType.BoolType }
                        )
                    ) { backStackEntry ->
                        val type = com.example.nummia.model.transactions.TransactionType.valueOf(
                            backStackEntry.arguments?.getString("type")
                                ?: com.example.nummia.model.transactions.TransactionType.EXPENSE.name
                        )
                        TransactionStatisticsScreen(
                            movements = appState.movements,
                            type = type,
                            openComparisonOnStart = backStackEntry.arguments?.getBoolean("comparison") == true,
                            onCategoryClick = { category ->
                                navController.navigate("category-statistics/${Uri.encode(category)}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(OVERVIEW_STATISTICS_ROUTE) {
                        TransactionStatisticsScreen(
                            movements = appState.movements,
                            type = com.example.nummia.model.transactions.TransactionType.EXPENSE,
                            openComparisonOnStart = false,
                            combined = true,
                            onCategoryClick = { category ->
                                navController.navigate("category-statistics/${Uri.encode(category)}")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = CATEGORY_STATISTICS_ROUTE,
                        arguments = listOf(navArgument("category") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val category = backStackEntry.arguments?.getString("category").orEmpty()
                        val comparisonMonths = CategoryStatisticsCalculator.comparisonMonths(
                            appState.movements,
                            category
                        )
                        val selectedPeriod = remember(category) {
                            mutableStateOf(CategoryChartPeriod.THIS_MONTH)
                        }
                        val selectedComparisonMonth = remember(category, comparisonMonths) {
                            mutableLongStateOf(comparisonMonths.first())
                        }
                        val series = remember(appState.movements, category, selectedPeriod.value) {
                            CategoryStatisticsCalculator.spendingSeries(
                                appState.movements,
                                category,
                                selectedPeriod.value
                            )
                        }
                        val comparison = remember(
                            appState.movements,
                            category,
                            selectedComparisonMonth.longValue
                        ) {
                            CategoryStatisticsCalculator.monthComparison(
                                appState.movements,
                                category,
                                selectedComparisonMonth.longValue
                            )
                        }
                        CategoryStatisticsScreen(
                            category = category,
                            period = selectedPeriod.value,
                            series = series,
                            comparison = comparison,
                            comparisonMonth = selectedComparisonMonth.longValue,
                            comparisonMonths = comparisonMonths,
                            onPeriodChange = { selectedPeriod.value = it },
                            onComparisonMonthChange = { selectedComparisonMonth.longValue = it },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(SETTINGS_ROUTE) {
                        SettingsScreen(
                            currencyCode = appState.currencyCode,
                            selectedTheme = selectedPalette,
                            billRemindersEnabled = appState.billRemindersEnabled,
                            billPushNotificationsEnabled = appState.billPushNotificationsEnabled,
                            monthlyReminderDaysBefore = appState.monthlyReminderDaysBefore,
                            weeklyReminderDaysBefore = appState.weeklyReminderDaysBefore,
                            billReminderHour = appState.billReminderHour,
                            billReminderMinute = appState.billReminderMinute,
                            overdueReminderIntervalDays = appState.overdueReminderIntervalDays,
                            reminderSnoozeHours = appState.reminderSnoozeHours,
                            onChangeCurrency = model::changeCurrency,
                            onChangeTheme = model::changeThemePalette,
                            onChangeBillReminders = model::changeBillReminders,
                            onChangeBillPushNotifications = model::changeBillPushNotifications,
                            onChangeReminderPreferences = model::changeBillReminderPreferences,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = TRANSACTION_DETAIL_ROUTE,
                        arguments = listOf(navArgument("movementId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val movementId = backStackEntry.arguments?.getLong("movementId")
                        val movement = appState.movements.firstOrNull { it.id == movementId }
                        TransactionDetailScreen(
                            movement = movement,
                            onBack = { navController.popBackStack() },
                            onEdit = { updatedMovement ->
                                movementController.updateMovement(updatedMovement)
                                navController.popBackStack()
                            },
                            onDelete = { id ->
                                movementController.deleteMovement(id)
                                navController.popBackStack()
                            }
                        )
                    }
                }
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
}
