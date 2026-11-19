package com.example.nummia.model.managers

import com.example.nummia.model.plannedexpenses.BillingFrequency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Calcula validaciones y fechas de facturas recurrentes sin acceder al estado ni a Room. */
internal object PlannedExpenseSchedule {
    fun requireValidBillingDay(frequency: BillingFrequency, day: Int) {
        val valid = when (frequency) {
            BillingFrequency.NONE -> true
            BillingFrequency.WEEKLY -> day in Calendar.SUNDAY..Calendar.SATURDAY
            BillingFrequency.MONTHLY -> day in 1..31
        }
        require(valid) { "El día de facturación no es válido." }
    }

    fun nextBillingDate(
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
                if (!includeToday && daysUntil == 0) {
                    current.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            BillingFrequency.MONTHLY -> {
                setMonthlyBillingDay(current, billingDay)
                if (
                    current.timeInMillis < afterDay ||
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

    fun isAllowedPeriod(
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

    fun formatBillingDate(timestamp: Long): String =
        "Próximo: " + SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES"))
            .format(Date(timestamp))

    private fun setMonthlyBillingDay(calendar: Calendar, billingDay: Int) {
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(
            Calendar.DAY_OF_MONTH,
            minOf(billingDay, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        )
    }

    private fun startOfWeek(calendar: Calendar): Calendar =
        (calendar.clone() as Calendar).apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
