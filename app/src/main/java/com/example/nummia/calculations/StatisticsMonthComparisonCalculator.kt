package com.example.nummia.calculations

import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import java.util.Calendar

/** Compara series mensuales y obtiene los meses disponibles para elegir. */
internal object StatisticsMonthComparisonCalculator {
    fun transactionMonthComparison(
        movements: List<MoneyMovement>,
        type: TransactionType,
        comparisonMonthStart: Long,
        now: Long
    ): CategoryMonthComparison = monthComparison(
        movements = movements,
        matches = { it.type == type },
        comparisonMonthStart = comparisonMonthStart,
        now = now
    )

    private fun monthComparison(
        movements: List<MoneyMovement>,
        matches: (MoneyMovement) -> Boolean,
        comparisonMonthStart: Long,
        now: Long
    ): CategoryMonthComparison {
        val current = Calendar.getInstance().apply { timeInMillis = now }.startOfMonth()
        val comparison = Calendar.getInstance().apply {
            timeInMillis = comparisonMonthStart
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentToday =
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
        val currentDays = current.getActualMaximum(Calendar.DAY_OF_MONTH)
        val comparisonDays = comparison.getActualMaximum(Calendar.DAY_OF_MONTH)
        val selectedMovements = movements.filter { matches(it) && it.timestamp <= now }

        fun cumulativePoints(month: Calendar, dayCount: Int): List<SpendingPoint> {
            var runningTotal = 0L
            return (1..dayCount).map { day ->
                val dayStart = (month.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, day)
                }
                val nextDay = (dayStart.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                runningTotal += selectedMovements.asSequence()
                    .filter {
                        it.timestamp >= dayStart.timeInMillis &&
                            it.timestamp < nextDay.timeInMillis
                    }
                    .sumOf { it.amountCents }
                SpendingPoint(dayStart.timeInMillis, day.toString(), runningTotal)
            }
        }

        val currentPoints = cumulativePoints(current, currentDays).take(currentToday)
        val comparisonPoints = cumulativePoints(comparison, comparisonDays)
        return CategoryMonthComparison(
            currentMonthPoints = currentPoints,
            comparisonMonthPoints = comparisonPoints,
            currentMonthTotalCents = currentPoints.lastOrNull()?.amountCents ?: 0L,
            comparisonMonthTotalCents = comparisonPoints.lastOrNull()?.amountCents ?: 0L
        )
    }

    fun transactionComparisonMonths(
        movements: List<MoneyMovement>,
        type: TransactionType,
        now: Long
    ): List<Long> = monthStarts(movements.filter { it.type == type }, now)

    private fun monthStarts(movements: List<MoneyMovement>, now: Long): List<Long> {
        val current = Calendar.getInstance().apply { timeInMillis = now }.startOfMonth()
        val earliestMonth = movements.asSequence()
            .filter { it.timestamp < current.timeInMillis }
            .map {
                Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    .startOfMonth().timeInMillis
            }
            .minOrNull()
        val previous = (current.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val cursor = earliestMonth?.let {
            Calendar.getInstance().apply { timeInMillis = it }
        } ?: (previous.clone() as Calendar)
        val months = mutableListOf<Long>()
        while (!cursor.after(previous)) {
            months += cursor.timeInMillis
            cursor.add(Calendar.MONTH, 1)
        }
        return months.asReversed()
    }

    private fun Calendar.startOfDay(): Calendar = (clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.startOfMonth(): Calendar = startOfDay().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
}
