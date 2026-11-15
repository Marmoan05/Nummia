package com.example.nummia.notifications

import com.example.nummia.model.plannedexpenses.BillingFrequency
import com.example.nummia.model.plannedexpenses.PlannedExpense
import java.util.Calendar

/** Cálculos puros para determinar cuándo mostrar recordatorios de facturas. */
object BillReminderCalculator {
    /** Prioriza una posposición activa y, si no la hay, calcula el siguiente aviso programado. */
    fun nextReminderAt(
        expense: PlannedExpense,
        now: Long,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        repeatIntervalDays: Int = 1
    ): Long? {
        if (expense.nextDueAt <= 0L || expense.remindersDisabled) return null
        if (expense.reminderSnoozedUntil > now) return expense.reminderSnoozedUntil
        val start = reminderStartAt(expense, monthlyLeadDays, weeklyLeadDays) ?: return null
        val nowDay = startOfDay(now)
        val targetDay = maxOf(startOfDay(start), nowDay)
        val target = atReminderTime(targetDay, reminderHour, reminderMinute)
        return if (target > now) target else Calendar.getInstance().run {
            timeInMillis = target
            add(Calendar.DAY_OF_YEAR, repeatIntervalDays.coerceAtLeast(1))
            timeInMillis
        }
    }

    fun shouldShowInApp(
        expense: PlannedExpense,
        now: Long,
        enabled: Boolean = true,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ): Boolean =
        enabled && !expense.remindersDisabled &&
            expense.nextDueAt > 0L &&
            now >= (reminderStartAt(expense, monthlyLeadDays, weeklyLeadDays)
                ?.let { atReminderTime(it, reminderHour, reminderMinute) } ?: Long.MAX_VALUE) &&
            expense.reminderSnoozedUntil <= now

    fun remindersToShowInApp(
        expenses: List<PlannedExpense>,
        now: Long,
        enabled: Boolean = true,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ): List<PlannedExpense> = expenses
        .filter {
            shouldShowInApp(
                it, now, enabled, monthlyLeadDays, weeklyLeadDays, reminderHour, reminderMinute
            )
        }
        .sortedWith(compareBy<PlannedExpense> { !isOverdue(it, now) }.thenBy { it.nextDueAt })

    /** Devuelve el inicio del aviso previo; las facturas únicas se avisan el día de vencimiento. */
    fun reminderStartAt(
        expense: PlannedExpense,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2
    ): Long? {
        if (expense.nextDueAt <= 0L || expense.frequency == BillingFrequency.NONE) {
            return expense.nextDueAt.takeIf { it > 0L }
        }
        val start = Calendar.getInstance().apply {
            timeInMillis = expense.nextDueAt
            when (expense.frequency) {
                BillingFrequency.WEEKLY -> add(Calendar.DAY_OF_YEAR, -weeklyLeadDays.coerceAtLeast(0))
                BillingFrequency.MONTHLY -> add(Calendar.DAY_OF_YEAR, -monthlyLeadDays.coerceAtLeast(0))
                BillingFrequency.NONE -> Unit
            }
        }
        return startOfDay(start.timeInMillis)
    }

    /** Una factura vence al terminar su día; se marca retrasada a partir del siguiente. */
    fun isOverdue(expense: PlannedExpense, now: Long): Boolean =
        expense.nextDueAt > 0L && startOfDay(now) > startOfDay(expense.nextDueAt)

    private fun startOfDay(timestamp: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Aplica la hora elegida y limita sus valores al rango válido del reloj. */
    private fun atReminderTime(dayStart: Long, hour: Int = 9, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
