package com.example.nummia.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nummia.calculations.BudgetSummary
import com.example.nummia.calculations.CategoryChartPeriod
import com.example.nummia.calculations.CategoryStatisticsCalculator
import com.example.nummia.calculations.StatisticsSummary
import com.example.nummia.controller.BalanceController
import com.example.nummia.controller.BudgetController
import com.example.nummia.controller.MovementController
import com.example.nummia.controller.PlannedExpenseController
import com.example.nummia.model.FinanceModel
import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.budgets.BudgetsScreen
import com.example.nummia.views.detail.TransactionDetailScreen
import com.example.nummia.views.history.HistoryScreen
import com.example.nummia.views.home.HomeScreen
import com.example.nummia.views.settings.SettingsScreen
import com.example.nummia.views.settings.ReleaseNotesHistoryScreen
import com.example.nummia.views.statistics.CategoryStatisticsScreen
import com.example.nummia.views.statistics.StatisticsScreen
import com.example.nummia.views.statistics.TransactionStatisticsScreen

private const val TRANSACTION_DETAIL_ROUTE = "transaction/{movementId}"
private const val CATEGORY_STATISTICS_ROUTE = "category-statistics/{category}"
private const val TRANSACTION_STATISTICS_ROUTE = "transaction-statistics/{type}/{comparison}"
private const val OVERVIEW_STATISTICS_ROUTE = "overview-statistics"
private const val SETTINGS_ROUTE = "settings"
private const val RELEASE_NOTES_ROUTE = "release-notes"

/** Conecta el estado financiero con todas las rutas de la aplicación. */
@Composable
internal fun AppNavigationHost(
    navController: NavHostController,
    appState: AppState,
    model: FinanceModel,
    balanceController: BalanceController,
    movementController: MovementController,
    plannedExpenseController: PlannedExpenseController,
    budgetController: BudgetController,
    budgetSummary: BudgetSummary,
    statisticsSummary: StatisticsSummary,
    expenseToOpenId: Long?,
    selectedExpenseId: Long?,
    onSelectedExpenseChanged: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = if (expenseToOpenId == null) {
            Screen.HOME.route
        } else {
            Screen.BUDGETS.route
        },
        modifier = modifier
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
                    onSelectedExpenseChanged(expenseId)
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
                    if (selectedExpenseId == expenseId) onSelectedExpenseChanged(null)
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
                        "transaction-statistics/${TransactionType.EXPENSE.name}/true"
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
            val type = TransactionType.valueOf(
                backStackEntry.arguments?.getString("type")
                    ?: TransactionType.EXPENSE.name
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
                type = TransactionType.EXPENSE,
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
                selectedTheme = com.example.nummia.model.settings.AppThemePalette.from(appState.themePalette),
                billRemindersEnabled = appState.billRemindersEnabled,
                billPushNotificationsEnabled = appState.billPushNotificationsEnabled,
                autoUpdateChecksEnabled = appState.autoUpdateChecksEnabled,
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
                onChangeAutoUpdateChecks = model::changeAutoUpdateChecks,
                onChangeReminderPreferences = model::changeBillReminderPreferences,
                onOpenReleaseNotes = {
                    navController.navigate(RELEASE_NOTES_ROUTE)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(RELEASE_NOTES_ROUTE) {
            ReleaseNotesHistoryScreen(onBack = { navController.popBackStack() })
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
}
