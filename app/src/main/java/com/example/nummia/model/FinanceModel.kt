package com.example.nummia.model

import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.managers.MovementManager
import com.example.nummia.model.managers.PlannedExpenseManager
import com.example.nummia.model.managers.SettingsManager
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.state.AppState
import com.example.nummia.model.state.createEmptyFinanceState
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias FinanceStateUpdater = (suspend (AppState) -> AppState) -> Unit

/** Coordina el estado en memoria con la carga y persistencia de datos en Room. */
class FinanceModel(private val repository: FinanceRepository) {
    private val _state = MutableStateFlow(createEmptyFinanceState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _error.value = throwable.localizedMessage ?: "No se pudieron guardar los cambios."
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val mutex = Mutex()
    private val initialized = scope.launch {
        _state.value = repository.loadOrCreateState()
        _isInitialized.value = true
    }
    private val settingsManager = SettingsManager(repository, ::updateState)
    private val movementManager = MovementManager(repository, ::updateState)
    private val plannedExpenseManager = PlannedExpenseManager(repository, ::updateState)

    /** Actualiza el saldo inicial. */
    fun setInitialBalance(amountCents: Long) =
        settingsManager.setInitialBalance(amountCents)

    /** Crea y guarda un ingreso o gasto. */
    fun addMovement(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        type: TransactionType,
        timestamp: Long
    ) = movementManager.addMovement(title, description, amountCents, category, type, timestamp)

    /** Actualiza un movimiento existente. */
    fun updateMovement(movement: MoneyMovement) = movementManager.updateMovement(movement)

    /** Elimina un movimiento. */
    fun deleteMovement(movementId: Long) = movementManager.deleteMovement(movementId)

    /** Crea una factura pendiente, opcionalmente recurrente. */
    fun addPlannedExpense(
        title: String,
        description: String?,
        amountCents: Long,
        category: String,
        dueDate: String,
        dueAt: Long,
        frequency: BillingFrequency,
        billingDay: Int,
        repeatUntilAt: Long?,
        includeUntilPeriod: Boolean
    ) = plannedExpenseManager.addPlannedExpense(
        title,
        description,
        amountCents,
        category,
        dueDate,
        dueAt,
        frequency,
        billingDay,
        repeatUntilAt,
        includeUntilPeriod
    )

    /** Actualiza una factura pendiente. */
    fun updatePlannedExpense(expense: PlannedExpense) =
        plannedExpenseManager.updatePlannedExpense(expense)

    /** Elimina una factura pendiente. */
    fun deletePlannedExpense(expenseId: Long) =
        plannedExpenseManager.deletePlannedExpense(expenseId)

    /** Pospone los avisos de una factura. */
    fun snoozePlannedExpenseReminder(expenseId: Long) =
        plannedExpenseManager.snoozePlannedExpenseReminder(expenseId)

    /** Desactiva los recordatorios de una factura sin eliminarla. */
    fun disablePlannedExpenseReminders(expenseId: Long) =
        plannedExpenseManager.disablePlannedExpenseReminders(expenseId)

    /** Cambia la preferencia global de recordatorios. */
    fun changeBillReminders(enabled: Boolean) =
        settingsManager.changeBillReminders(enabled)

    /** Guarda las opciones de recordatorio y posposición. */
    fun changeBillReminderPreferences(
        monthlyDaysBefore: Int,
        weeklyDaysBefore: Int,
        hour: Int,
        minute: Int,
        repeatIntervalDays: Int,
        snoozeHours: Int
    ) = settingsManager.changeBillReminderPreferences(
        monthlyDaysBefore,
        weeklyDaysBefore,
        hour,
        minute,
        repeatIntervalDays,
        snoozeHours
    )

    /** Activa o desactiva únicamente las notificaciones del sistema. */
    fun changeBillPushNotifications(enabled: Boolean) =
        settingsManager.changeBillPushNotifications(enabled)

    /** Registra el pago como gasto y completa o programa la siguiente factura recurrente. */
    fun markPlannedExpensePaid(expenseId: Long, extraCents: Long) =
        plannedExpenseManager.markPlannedExpensePaid(expenseId, extraCents)

    /** Guarda los límites de gasto semanal y mensual. */
    fun setBudgets(weeklyCents: Long, monthlyCents: Long) =
        settingsManager.setBudgets(weeklyCents, monthlyCents)

    /** Cambia la moneda sin conversión y ajusta la precisión de los importes. */
    fun changeCurrency(currencyCode: String) = settingsManager.changeCurrency(currencyCode)

    /** Cambia la paleta visual seleccionada. */
    fun changeThemePalette(themePalette: String) =
        settingsManager.changeThemePalette(themePalette)

    /** Limpia el error ya mostrado por la interfaz. */
    fun clearError() {
        _error.value = null
    }

    /** Serializa cambios y persistencia para que dos acciones no sobrescriban el mismo estado. */
    private fun updateState(transform: suspend (AppState) -> AppState) {
        scope.launch {
            initialized.join()
            mutex.withLock {
                _state.value = transform(_state.value)
            }
        }
    }
}
