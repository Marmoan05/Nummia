package com.example.nummia.calculations

import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Agrupa movimientos en los intervalos diarios o mensuales usados por las gráficas. */
internal object StatisticsPeriodCalculator {
    fun transactionSeries(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long,
        rangeStartOverride: Long?
    ): List<SpendingPoint> = transactionPeriodData(
        movements,
        type,
        period,
        now,
        rangeStartOverride
    ).points

    fun transactionPeriodData(
        movements: List<MoneyMovement>,
        type: TransactionType,
        period: CategoryChartPeriod,
        now: Long,
        rangeStartOverride: Long?
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
            categories[movement.category] =
                (categories[movement.category] ?: 0L) + movement.amountCents
        }
        val aggregatedPoints = points.mapIndexed { index, point ->
            point.copy(amountCents = categoryAmounts[index].values.sum())
        }
        return TransactionPeriodData(aggregatedPoints, categoryAmounts)
    }

    private fun rangeStart(
        earliestMovement: Long?,
        period: CategoryChartPeriod,
        now: Long
    ): Calendar {
        val current = Calendar.getInstance().apply { timeInMillis = now }
        return when (period) {
            CategoryChartPeriod.LAST_7_DAYS ->
                current.startOfDay().apply { add(Calendar.DAY_OF_YEAR, -6) }
            CategoryChartPeriod.THIS_MONTH -> current.startOfMonth()
            CategoryChartPeriod.LAST_3_MONTHS ->
                current.startOfMonth().apply { add(Calendar.MONTH, -2) }
            CategoryChartPeriod.LAST_6_MONTHS ->
                current.startOfMonth().apply { add(Calendar.MONTH, -5) }
            CategoryChartPeriod.LAST_YEAR ->
                current.startOfMonth().apply { add(Calendar.MONTH, -11) }
            CategoryChartPeriod.ALL_TIME -> earliestMovement
                ?.let { Calendar.getInstance().apply { timeInMillis = it }.startOfMonth() }
                ?: current.startOfMonth()
        }
    }

    private fun chartLabel(
        bucket: Calendar,
        start: Calendar,
        current: Calendar,
        daily: Boolean
    ): String = when {
        daily -> SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES"))
            .format(Date(bucket.timeInMillis))
        start.get(Calendar.YEAR) != current.get(Calendar.YEAR) ->
            SimpleDateFormat("MMM yy", Locale.forLanguageTag("es-ES"))
                .format(Date(bucket.timeInMillis))
        else -> SimpleDateFormat("MMM", Locale.forLanguageTag("es-ES"))
            .format(Date(bucket.timeInMillis))
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
