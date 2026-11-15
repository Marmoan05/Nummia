package com.example.nummia.calculations

import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.TransactionType
import java.util.Calendar

/** Resultado de los cálculos de gasto durante la semana y el mes actuales. */
data class BudgetSummary(
    val weeklySpentCents: Long,
    val monthlySpentCents: Long
)

/** Calcula cuánto se ha gastado en los periodos presupuestarios actuales. */
object BudgetCalculator {
    /** Devuelve los totales de gastos de la semana actual y el mes actual. */
    fun calculate(state: AppState): BudgetSummary {
        val expenses = state.movements.filter { it.type == TransactionType.EXPENSE }
        return BudgetSummary(
            weeklySpentCents = expenses.filter { it.timestamp.isInCurrentWeek() }.sumOf { it.amountCents },
            monthlySpentCents = expenses.filter { it.timestamp.isInCurrentMonth() }.sumOf { it.amountCents }
        )
    }

    /** Comprueba si una marca temporal pertenece a la semana actual, de lunes a domingo. */
    private fun Long.isInCurrentWeek(): Boolean {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        return this >= start && this < calendar.timeInMillis
    }

    /** Comprueba si una marca temporal cae dentro del mes y año actuales. */
    private fun Long.isInCurrentMonth(): Boolean {
        val movement = Calendar.getInstance().apply { timeInMillis = this@isInCurrentMonth }
        val current = Calendar.getInstance()
        return movement.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            movement.get(Calendar.MONTH) == current.get(Calendar.MONTH)
    }
}
