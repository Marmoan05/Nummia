package com.example.nummia.calculations

import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.TransactionType
import java.util.Calendar

/** Calcula los totales mensuales mostrados por la pantalla principal de estadísticas. */
internal object StatisticsSummaryCalculator {
    fun calculate(state: AppState, now: Long): StatisticsSummary {
        val movementsToDate = state.movements.filter { it.timestamp <= now }
        val thisMonth = movementsToDate.filter { it.timestamp.isInCurrentMonth(now) }
        val previousMonthExpenses = movementsToDate.filter {
            it.type == TransactionType.EXPENSE && it.timestamp.isInPreviousMonth(now)
        }
        val thisMonthExpenses = thisMonth.filter { it.type == TransactionType.EXPENSE }
        return StatisticsSummary(
            incomeThisMonthCents = thisMonth
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amountCents },
            expensesThisMonthCents = thisMonthExpenses.sumOf { it.amountCents },
            expensesPreviousMonthCents = previousMonthExpenses.sumOf { it.amountCents },
            expensesByCategoryCents = thisMonthExpenses
                .groupBy { it.category }
                .mapValues { (_, movements) -> movements.sumOf { it.amountCents } }
        )
    }

    private fun Long.isInCurrentMonth(now: Long): Boolean {
        val movement = Calendar.getInstance().apply { timeInMillis = this@isInCurrentMonth }
        val current = Calendar.getInstance().apply { timeInMillis = now }
        return movement.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            movement.get(Calendar.MONTH) == current.get(Calendar.MONTH)
    }

    private fun Long.isInPreviousMonth(now: Long): Boolean {
        val movement = Calendar.getInstance().apply { timeInMillis = this@isInPreviousMonth }
        val previous = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -1)
        }
        return movement.get(Calendar.YEAR) == previous.get(Calendar.YEAR) &&
            movement.get(Calendar.MONTH) == previous.get(Calendar.MONTH)
    }
}
