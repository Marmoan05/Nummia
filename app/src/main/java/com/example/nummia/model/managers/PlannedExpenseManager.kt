package com.example.nummia.model.managers

import com.example.nummia.database.FinanceRepository
import com.example.nummia.model.state.AppState
import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.FinanceStateUpdater
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.plannedexpenses.PlannedExpense
import com.example.nummia.model.transactions.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Gestiona facturas previstas, recurrencias, recordatorios y registro de pagos. */
internal class PlannedExpenseManager(
    private val repository: FinanceRepository,
    private val updateState: FinanceStateUpdater
) {
    /** Crea una factura pendiente, opcionalmente recurrente, sin descontarla del saldo. */
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
    ) {
        updateState { state ->
            val nextDueAt = if (frequency == BillingFrequency.NONE) {
                require(dueAt > 0L) { "Selecciona la fecha de vencimiento." }
                dueAt
            } else {
                requireValidBillingDay(frequency, billingDay)
                nextBillingDate(frequency, billingDay, System.currentTimeMillis(), includeToday = true)
            }
            require(
                repeatUntilAt == null ||
                    (frequency != BillingFrequency.NONE &&
                        isAllowedPeriod(nextDueAt, repeatUntilAt, frequency, includeUntilPeriod))
            ) { "La fecha de fin no permite ningún cobro con la frecuencia elegida." }
            val expense = PlannedExpense(
                id = System.nanoTime(),
                title = title,
                description = description,
                amountCents = amountCents,
                dueDate = if (frequency == BillingFrequency.NONE) dueDate
                else formatBillingDate(nextDueAt),
                category = category,
                frequency = frequency,
                billingDay = billingDay,
                nextDueAt = nextDueAt,
                repeatUntilAt = repeatUntilAt,
                includeUntilPeriod = includeUntilPeriod
            )
            repository.addPlannedExpense(expense)
            state.copy(plannedExpenses = listOf(expense) + state.plannedExpenses)
        }
    }

    /** Actualiza la descripción, el importe y la programación de una factura pendiente. */
    fun updatePlannedExpense(expense: PlannedExpense) {
        updateState { state ->
            val current = state.plannedExpenses.firstOrNull { it.id == expense.id }
                ?: error("La factura pendiente ya no existe.")
            val nextDueAt = if (expense.frequency == BillingFrequency.NONE) {
                require(expense.nextDueAt > 0L) { "Selecciona la fecha de vencimiento." }
                expense.nextDueAt
            } else if (
                expense.frequency == current.frequency &&
                expense.billingDay == current.billingDay &&
                current.nextDueAt > 0L
            ) {
                // Editar el resto de datos no debe adelantar el vencimiento ya programado.
                current.nextDueAt
            } else {
                requireValidBillingDay(expense.frequency, expense.billingDay)
                nextBillingDate(
                    expense.frequency,
                    expense.billingDay,
                    System.currentTimeMillis(),
                    includeToday = true
                )
            }
            require(
                expense.repeatUntilAt == null ||
                    (expense.frequency != BillingFrequency.NONE &&
                        isAllowedPeriod(
                            nextDueAt,
                            expense.repeatUntilAt,
                            expense.frequency,
                            expense.includeUntilPeriod
                        ))
            ) { "La fecha de fin no permite ningún cobro con la frecuencia elegida." }
            val updated = current.copy(
                title = expense.title,
                description = expense.description,
                amountCents = expense.amountCents,
                dueDate = if (expense.frequency == BillingFrequency.NONE) expense.dueDate
                else formatBillingDate(nextDueAt),
                category = expense.category,
                frequency = expense.frequency,
                billingDay = expense.billingDay,
                nextDueAt = nextDueAt,
                repeatUntilAt = expense.repeatUntilAt,
                includeUntilPeriod = expense.includeUntilPeriod,
                remindersDisabled = expense.remindersDisabled,
                reminderSnoozedUntil = expense.reminderSnoozedUntil
            )
            repository.updatePlannedExpense(updated)
            state.copy(
                plannedExpenses = state.plannedExpenses.map {
                    if (it.id == expense.id) updated else it
                }
            )
        }
    }

    /** Elimina una factura pendiente de la base y del estado visible. */
    fun deletePlannedExpense(expenseId: Long) {
        updateState { state ->
            repository.deletePlannedExpense(expenseId)
            state.copy(plannedExpenses = state.plannedExpenses.filterNot { it.id == expenseId })
        }
    }

    /** Pospone los avisos de una factura durante el intervalo configurado. */
    fun snoozePlannedExpenseReminder(expenseId: Long) {
        updateReminderPreferences(expenseId) { expense, snoozeHours ->
            expense.copy(
                reminderSnoozedUntil = System.currentTimeMillis() + snoozeHours * 60 * 60 * 1000L
            )
        }
    }

    /** Desactiva los recordatorios de una factura sin eliminarla. */
    fun disablePlannedExpenseReminders(expenseId: Long) {
        updateReminderPreferences(expenseId) { expense, _ -> expense.copy(remindersDisabled = true) }
    }

    /** Persiste el cambio de recordatorio y deja el estado intacto si la factura ya no existe. */
    private fun updateReminderPreferences(
        expenseId: Long,
        transform: (PlannedExpense, Int) -> PlannedExpense
    ) {
        updateState { state ->
            val current = state.plannedExpenses.firstOrNull { it.id == expenseId }
                ?: return@updateState state
            val updated = transform(current, state.reminderSnoozeHours)
            repository.updatePlannedExpense(updated)
            state.copy(
                plannedExpenses = state.plannedExpenses.map {
                    if (it.id == expenseId) updated else it
                }
            )
        }
    }

    /** Registra el pago como gasto y elimina o programa la siguiente factura recurrente. */
    fun markPlannedExpensePaid(expenseId: Long, extraCents: Long) {
        updateState { state ->
            require(extraCents >= 0) { "El extra no puede ser negativo." }
            val current = state.plannedExpenses.firstOrNull { it.id == expenseId }
                ?: error("La factura pendiente ya no existe.")
            val totalCents = Math.addExact(current.amountCents, extraCents)
            val now = System.currentTimeMillis()
            val nextDueAt = if (current.frequency == BillingFrequency.NONE) {
                null
            } else {
                // Si la factura estaba atrasada, un pago no genera una cadena de pagos omitidos.
                nextBillingDate(
                    current.frequency,
                    current.billingDay,
                    maxOf(now, current.nextDueAt),
                    includeToday = false
                ).takeIf {
                    current.repeatUntilAt == null ||
                        isAllowedPeriod(
                            it,
                            current.repeatUntilAt,
                            current.frequency,
                            current.includeUntilPeriod
                        )
                }
            }
            val nextExpense = nextDueAt?.let {
                current.copy(
                    nextDueAt = it,
                    dueDate = formatBillingDate(it),
                    remindersDisabled = false,
                    reminderSnoozedUntil = 0L
                )
            }
            val movement = MoneyMovement(
                id = System.nanoTime(),
                title = current.title,
                description = when {
                    !current.description.isNullOrBlank() && extraCents > 0 ->
                        "${current.description} · Pago con extra por retraso."
                    !current.description.isNullOrBlank() -> current.description
                    extraCents > 0 -> "Pago previsto con extra por retraso."
                    else -> "Pago previsto."
                },
                amountCents = totalCents,
                category = current.category,
                type = TransactionType.EXPENSE,
                timestamp = now
            )
            repository.recordPlannedExpensePayment(movement, nextExpense, current.id)
            state.copy(
                movements = (listOf(movement) + state.movements)
                    .sortedByDescending { it.timestamp },
                plannedExpenses = state.plannedExpenses.mapNotNull {
                    when {
                        it.id != current.id -> it
                        nextExpense != null -> nextExpense
                        else -> null
                    }
                }
            )
        }
    }

    /** Valida el día según la frecuencia antes de calcular el siguiente vencimiento. */
    private fun requireValidBillingDay(frequency: BillingFrequency, day: Int) {
        val valid = when (frequency) {
            BillingFrequency.NONE -> true
            BillingFrequency.WEEKLY -> day in Calendar.SUNDAY..Calendar.SATURDAY
            BillingFrequency.MONTHLY -> day in 1..31
        }
        require(valid) { "El día de facturación no es válido." }
    }

    /** Devuelve el siguiente vencimiento y permite incluir hoy al crear o editar la factura. */
    private fun nextBillingDate(
        frequency: BillingFrequency,
        billingDay: Int,
        after: Long,
        includeToday: Boolean
    ): Long {
        val current = Calendar.getInstance().apply {
            timeInMillis = after
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val afterDay = current.timeInMillis
        when (frequency) {
            BillingFrequency.NONE -> return 0L
            BillingFrequency.WEEKLY -> {
                val daysUntil = (billingDay - current.get(Calendar.DAY_OF_WEEK) + 7) % 7
                current.add(Calendar.DAY_OF_YEAR, daysUntil)
                if (!includeToday && daysUntil == 0) current.add(Calendar.WEEK_OF_YEAR, 1)
            }
            BillingFrequency.MONTHLY -> {
                setMonthlyBillingDay(current, billingDay)
                if (current.timeInMillis < afterDay ||
                    (!includeToday && current.timeInMillis == afterDay)
                ) {
                    current.set(Calendar.DAY_OF_MONTH, 1)
                    current.add(Calendar.MONTH, 1)
                    setMonthlyBillingDay(current, billingDay)
                }
            }
        }
        return current.timeInMillis
    }

    /** Ajusta los días que no existen en el mes al último día disponible. */
    private fun setMonthlyBillingDay(calendar: Calendar, billingDay: Int) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(
            Calendar.DAY_OF_MONTH,
            minOf(billingDay, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        )
    }

    /** Compara semanas o meses completos; el indicador decide si se incluye el periodo final. */
    private fun isAllowedPeriod(
        dueAt: Long,
        endAt: Long,
        frequency: BillingFrequency,
        includeEndPeriod: Boolean
    ): Boolean {
        val due = Calendar.getInstance().apply { timeInMillis = dueAt }
        val end = Calendar.getInstance().apply { timeInMillis = endAt }
        val samePeriod = when (frequency) {
            BillingFrequency.NONE -> false
            BillingFrequency.MONTHLY ->
                due.get(Calendar.YEAR) == end.get(Calendar.YEAR) &&
                    due.get(Calendar.MONTH) == end.get(Calendar.MONTH)
            BillingFrequency.WEEKLY ->
                startOfWeek(due).timeInMillis == startOfWeek(end).timeInMillis
        }
        val duePeriodStart = if (frequency == BillingFrequency.WEEKLY) {
            startOfWeek(due).timeInMillis
        } else {
            due.set(Calendar.DAY_OF_MONTH, 1)
            due.timeInMillis
        }
        val endPeriodStart = if (frequency == BillingFrequency.WEEKLY) {
            startOfWeek(end).timeInMillis
        } else {
            end.set(Calendar.DAY_OF_MONTH, 1)
            end.timeInMillis
        }
        return duePeriodStart < endPeriodStart || (samePeriod && includeEndPeriod)
    }

    /** Normaliza al lunes a medianoche, que es el inicio de semana usado por la app. */
    private fun startOfWeek(calendar: Calendar): Calendar =
        (calendar.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /** Guarda una etiqueta breve de vencimiento para facturas recurrentes. */
    private fun formatBillingDate(timestamp: Long): String =
        "Próximo: " + SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES")).format(Date(timestamp))
}
