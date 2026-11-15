package com.example.nummia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nummia.database.FinanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Ejecuta las acciones rápidas de los recordatorios desde el panel de notificaciones. */
class BillReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val expenseId = intent.getLongExtra(BillReminderScheduler.EXTRA_EXPENSE_ID, -1L)
        if (expenseId < 0L) return
        val action = intent.action
        if (action == BillReminderScheduler.ACTION_SHOW) {
            BillReminderScheduler.cancelNotification(context, expenseId)
            context.startActivity(
                Intent(context, com.example.nummia.MainActivity::class.java)
                    .putExtra(BillReminderScheduler.EXTRA_EXPENSE_ID, expenseId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = FinanceDatabase.open(context)
                val dao = database.financeDao()
                val settings = dao.getSettings()
                val remindersEnabled = settings?.let {
                    it.billRemindersEnabled && it.billPushNotificationsEnabled
                } == true
                val expense = dao.getPlannedExpenses().firstOrNull { it.id == expenseId }
                if (expense != null) {
                    when (action) {
                        BillReminderScheduler.ACTION_SNOOZE ->
                            dao.updatePlannedExpense(
                                expense.copy(
                                    reminderSnoozedUntil = System.currentTimeMillis() +
                                        (settings?.reminderSnoozeHours ?: 24) * 60 * 60 * 1000L
                                )
                            )
                        BillReminderScheduler.ACTION_DISMISS ->
                            dao.updatePlannedExpense(expense.copy(remindersDisabled = true))
                    }
                    BillReminderScheduler.cancel(context, expenseId)
                    if (action == BillReminderScheduler.ACTION_SNOOZE) {
                        BillReminderScheduler.schedule(
                            context,
                            expense.copy(
                                reminderSnoozedUntil = System.currentTimeMillis() +
                                    (settings?.reminderSnoozeHours ?: 24) * 60 * 60 * 1000L
                            ),
                            enabled = remindersEnabled,
                            monthlyLeadDays = settings?.monthlyReminderDaysBefore ?: 7,
                            weeklyLeadDays = settings?.weeklyReminderDaysBefore ?: 2,
                            reminderHour = settings?.billReminderHour ?: 9,
                            reminderMinute = settings?.billReminderMinute ?: 0,
                            repeatIntervalDays = settings?.overdueReminderIntervalDays ?: 1
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
