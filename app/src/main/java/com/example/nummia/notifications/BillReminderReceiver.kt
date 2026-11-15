package com.example.nummia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nummia.database.FinanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Presenta el aviso programado y agenda el siguiente aviso mientras siga pendiente. */
class BillReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val expenseId = intent.getLongExtra(BillReminderScheduler.EXTRA_EXPENSE_ID, -1L)
        if (expenseId < 0L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = FinanceDatabase.open(context)
                val dao = database.financeDao()
                val settings = dao.getSettings()
                val expense = dao.getPlannedExpenses().firstOrNull { it.id == expenseId }
                if (settings?.let { it.billRemindersEnabled && it.billPushNotificationsEnabled } == true &&
                    expense != null &&
                    !expense.remindersDisabled
                ) {
                    val reminderOptions = arrayOf(
                        settings.monthlyReminderDaysBefore,
                        settings.weeklyReminderDaysBefore,
                        settings.billReminderHour,
                        settings.billReminderMinute,
                        settings.overdueReminderIntervalDays
                    )
                    if (expense.reminderSnoozedUntil <= System.currentTimeMillis()) {
                        BillReminderScheduler.showNotification(context, expense)
                        BillReminderScheduler.schedule(
                            context, expense, enabled = true,
                            monthlyLeadDays = reminderOptions[0],
                            weeklyLeadDays = reminderOptions[1],
                            reminderHour = reminderOptions[2],
                            reminderMinute = reminderOptions[3],
                            repeatIntervalDays = reminderOptions[4]
                        )
                    } else {
                        BillReminderScheduler.schedule(
                            context, expense, enabled = true,
                            monthlyLeadDays = reminderOptions[0],
                            weeklyLeadDays = reminderOptions[1],
                            reminderHour = reminderOptions[2],
                            reminderMinute = reminderOptions[3],
                            repeatIntervalDays = reminderOptions[4]
                        )
                    }
                } else {
                    BillReminderScheduler.cancel(context, expenseId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
