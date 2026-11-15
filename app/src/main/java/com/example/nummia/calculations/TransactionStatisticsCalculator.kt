package com.example.nummia.calculations

import com.example.nummia.model.state.AppState
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Intervalos que pueden seleccionarse en las gráficas de movimientos y categorías. */
enum class CategoryChartPeriod(val label: String) {
    LAST_7_DAYS("Últimos 7 días"),
    THIS_MONTH("Este mes"),
    LAST_3_MONTHS("Últimos 3 meses"),
    LAST_6_MONTHS("Últimos 6 meses"),
    LAST_YEAR("Último año"),
    ALL_TIME("Total")
}

/** Importe en céntimos asociado a un intervalo y a su etiqueta visible. */
data class SpendingPoint(val timestamp: Long, val label: String, val amountCents: Long)

/** Series diarias del mes actual y del mes comparado, con sus totales acumulados. */
data class CategoryMonthComparison(
    val currentMonthPoints: List<SpendingPoint>,
    val comparisonMonthPoints: List<SpendingPoint>,
    val currentMonthTotalCents: Long,
    val comparisonMonthTotalCents: Long
)

/** Total de una categoría; percentage es una fracción entre 0 y 1. */
data class StatisticsCategoryTotal(
    val category: String,
    val amountCents: Long,
    val percentage: Float
)

/** Serie total y desglose por categoría alineados por índice de punto. */
data class TransactionPeriodData(
    val points: List<SpendingPoint>,
    val categoryAmountsByPoint: List<Map<String, Long>>
)

/** Resultado agregado usado por la pantalla de estadísticas. */
data class StatisticsSummary(
    val incomeThisMonthCents: Long,
    val expensesThisMonthCents: Long,
    val expensesPreviousMonthCents: Long,
    val expensesByCategoryCents: Map<String, Long>
) {
    /** Diferencia entre gastos del mes actual y los del mes anterior. */
    val expenseDifferenceCents: Long
        get() = expensesThisMonthCents - expensesPreviousMonthCents
}

/** Calcula resúmenes y series temporales para las estadísticas de movimientos. */
object TransactionStatisticsCalculator {
    /** Convierte los importes por periodo en un total progresivo, si se solicita. */
    fun cumulativeSeries(
        points: List<SpendingPoint>,
        cumulative: Boolean
    ): List<SpendingPoint> {
        if (!cumulative) return points
        var total = 0L
        return points.map { point ->
            total += point.amountCents
            point.copy(amountCents = total)
        }
    }

    /** Resume movimientos del mes actual y gastos del mes inmediatamente anterior. */
    fun calculate(state: AppState): StatisticsSummary {
        val thisMonth = state.movements.filter { it.timestamp.isInCurrentMonth() }
        val previousMonthExpenses = state.movements.filter {
            it.type == TransactionType.EXPENSE && it.timestamp.isInPreviousMonth()
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

    /** Crea la serie temporal de ingresos o gastos para el periodo elegido. */
    fun transactionSeries(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): List<SpendingPoint> = transactionPeriodData(
        movements,
        type,
        period,
        now,
        rangeStartOverride
    ).points

    /** Agrega en una pasada movimientos, categorías y totales en los intervalos visibles. */
    fun transactionPeriodData(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): TransactionPeriodData {
        val selected = movements.filter { it.type == type && it.timestamp <= now }
        val start = rangeStart(
            rangeStartOverride ?: selected.minOfOrNull { it.timestamp },
            period,
            now
        )
        val current = Calendar.getInstance().apply { timeInMillis = now }
        val end = current.startOfDay()
        val daily = period == CategoryChartPeriod.LAST_7_DAYS ||
            period == CategoryChartPeriod.THIS_MONTH
        val bucket = start.clone() as Calendar
        val points = mutableListOf<SpendingPoint>()
        val categoryAmounts = mutableListOf<MutableMap<String, Long>>()
        val bucketIndexes = mutableMapOf<Long, Int>()
        while (if (daily) !bucket.after(end) else !bucket.after(end.startOfMonth())) {
            val bucketStart = bucket.timeInMillis
            bucketIndexes[bucketStart] = points.size
            points += SpendingPoint(
                timestamp = bucketStart,
                label = chartLabel(bucket, start, current, daily),
                amountCents = 0L
            )
            categoryAmounts += mutableMapOf()
            bucket.add(if (daily) Calendar.DAY_OF_YEAR else Calendar.MONTH, 1)
        }
        selected.forEach { movement ->
            val movementCalendar = Calendar.getInstance().apply { timeInMillis = movement.timestamp }
            val bucketStart = if (daily) {
                movementCalendar.startOfDay().timeInMillis
            } else {
                movementCalendar.startOfMonth().timeInMillis
            }
            val bucketIndex = bucketIndexes[bucketStart] ?: return@forEach
            val categories = categoryAmounts[bucketIndex]
            categories[movement.category] = (categories[movement.category] ?: 0L) + movement.amountCents
        }
        val aggregatedPoints = points.mapIndexed { index, point ->
            point.copy(amountCents = categoryAmounts[index].values.sum())
        }
        return TransactionPeriodData(aggregatedPoints, categoryAmounts)
    }

    /** Desglosa categorías para el periodo completo o hasta el punto seleccionado. */
    fun categoryBreakdown(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        selectedPointIndex: Int?,
        cumulative: Boolean,
        now: Long = System.currentTimeMillis(),
        rangeStartOverride: Long? = null
    ): List<StatisticsCategoryTotal> {
        val periodData = transactionPeriodData(movements, type, period, now, rangeStartOverride)
        return categoryBreakdown(periodData, selectedPointIndex, cumulative)
    }

    /** Calcula el desglose usando solo los totales previamente agrupados de los intervalos. */
    fun categoryBreakdown(
        periodData: TransactionPeriodData,
        selectedPointIndex: Int?,
        cumulative: Boolean
    ): List<StatisticsCategoryTotal> {
        if (periodData.points.isEmpty()) return emptyList()
        val endIndex = selectedPointIndex?.coerceIn(periodData.points.indices)
            ?: periodData.points.lastIndex
        val startIndex = if (cumulative) 0 else selectedPointIndex?.coerceIn(periodData.points.indices) ?: 0
        val totals = mutableMapOf<String, Long>()
        for (index in startIndex..endIndex) {
            periodData.categoryAmountsByPoint[index].forEach { (category, amount) ->
                totals[category] = (totals[category] ?: 0L) + amount
            }
        }
        val nonZeroTotals = totals.filterValues { it > 0L }
        val total = nonZeroTotals.values.sum()
        return nonZeroTotals.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                StatisticsCategoryTotal(
                    category = category,
                    amountCents = amount,
                    percentage = if (total > 0L) amount.toFloat() / total else 0f
                )
            }
    }

    /** Compara ingresos o gastos acumulados del mes actual con un mes elegido. */
    fun transactionMonthComparison(
        movements: List<MoneyMovement>,
        type: TransactionType,
        comparisonMonthStart: Long,
        now: Long = System.currentTimeMillis()
    ): CategoryMonthComparison = monthComparison(
        movements = movements,
        matches = { it.type == type },
        comparisonMonthStart = comparisonMonthStart,
        now = now
    )

    /** Alinea ambos meses por día y compara los acumulados hasta hoy en el mes actual. */
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
        val currentToday = Calendar.getInstance().apply { timeInMillis = now }
            .get(Calendar.DAY_OF_MONTH)
        val currentDays = current.getActualMaximum(Calendar.DAY_OF_MONTH)
        val comparisonDays = comparison.getActualMaximum(Calendar.DAY_OF_MONTH)
        val selectedMovements = movements.filter { matches(it) && it.timestamp <= now }

        /** Rellena todos los días del mes para que la comparación tenga una escala diaria común. */
        fun cumulativePoints(month: Calendar, dayCount: Int): List<SpendingPoint> {
            var runningTotal = 0L
            return (1..dayCount).map { day ->
                val dayStart = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
                val nextDay = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                runningTotal += selectedMovements.asSequence()
                    .filter { it.timestamp >= dayStart.timeInMillis && it.timestamp < nextDay.timeInMillis }
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

    /** Meses disponibles para comparar ingresos o gastos. */
    fun transactionComparisonMonths(
        movements: List<MoneyMovement>,
        type: TransactionType,
        now: Long = System.currentTimeMillis()
    ): List<Long> = monthStarts(movements.filter { it.type == type }, now)

    private fun monthStarts(movements: List<MoneyMovement>, now: Long): List<Long> {
        val current = Calendar.getInstance().apply { timeInMillis = now }.startOfMonth()
        val earliestMonth = movements.asSequence()
            .filter { it.timestamp < current.timeInMillis }
            .map { Calendar.getInstance().apply { timeInMillis = it.timestamp }.startOfMonth().timeInMillis }
            .minOrNull()
        val previous = (current.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val cursor = earliestMonth?.let { Calendar.getInstance().apply { timeInMillis = it } }
            ?: (previous.clone() as Calendar)
        val months = mutableListOf<Long>()
        while (!cursor.after(previous)) {
            months += cursor.timeInMillis
            cursor.add(Calendar.MONTH, 1)
        }
        return months.asReversed()
    }

    /** Calcula el primer intervalo visible según el periodo y la fecha más antigua disponible. */
    private fun rangeStart(earliestMovement: Long?, period: CategoryChartPeriod, now: Long): Calendar {
        val current = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            CategoryChartPeriod.LAST_7_DAYS -> current.startOfDay().apply { add(Calendar.DAY_OF_YEAR, -6) }
            CategoryChartPeriod.THIS_MONTH -> current.startOfMonth()
            CategoryChartPeriod.LAST_3_MONTHS -> current.startOfMonth().apply { add(Calendar.MONTH, -2) }
            CategoryChartPeriod.LAST_6_MONTHS -> current.startOfMonth().apply { add(Calendar.MONTH, -5) }
            CategoryChartPeriod.LAST_YEAR -> current.startOfMonth().apply { add(Calendar.MONTH, -11) }
            CategoryChartPeriod.ALL_TIME -> earliestMovement
                ?.let { Calendar.getInstance().apply { timeInMillis = it }.startOfMonth() }
                ?: current.startOfMonth()
        }
    }

    /** Usa etiquetas cortas para puntos diarios y etiquetas de mes para series más largas. */
    private fun chartLabel(bucket: Calendar, start: Calendar, current: Calendar, daily: Boolean): String =
        when {
            daily -> SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES")).format(Date(bucket.timeInMillis))
            start.get(Calendar.YEAR) != current.get(Calendar.YEAR) ->
                SimpleDateFormat("MMM yy", Locale.forLanguageTag("es-ES")).format(Date(bucket.timeInMillis))
            else -> SimpleDateFormat("MMM", Locale.forLanguageTag("es-ES")).format(Date(bucket.timeInMillis))
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

    /** Comprueba si una marca temporal corresponde al mes actual. */
    private fun Long.isInCurrentMonth(): Boolean {
        val movement = Calendar.getInstance().apply { timeInMillis = this@isInCurrentMonth }
        val current = Calendar.getInstance()
        return movement.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
            movement.get(Calendar.MONTH) == current.get(Calendar.MONTH)
    }

    /** Comprueba si una marca temporal corresponde al mes anterior, incluso entre años. */
    private fun Long.isInPreviousMonth(): Boolean {
        val movement = Calendar.getInstance().apply { timeInMillis = this@isInPreviousMonth }
        val previous = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        return movement.get(Calendar.YEAR) == previous.get(Calendar.YEAR) &&
            movement.get(Calendar.MONTH) == previous.get(Calendar.MONTH)
    }
}
