package com.example.nummia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nummia.database.FinanceDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Restaura los recordatorios que Android elimina al reiniciar el dispositivo. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = FinanceDatabase.open(context)
                val dao = database.financeDao()
                val settings = dao.getSettings()
                val expenses = dao.getAllPlannedExpenses()
                BillReminderScheduler.scheduleAll(
                    context,
                    expenses,
                    settings?.let { it.billRemindersEnabled && it.billPushNotificationsEnabled } == true,
                    monthlyLeadDays = settings?.monthlyReminderDaysBefore ?: 7,
                    weeklyLeadDays = settings?.weeklyReminderDaysBefore ?: 2,
                    reminderHour = settings?.billReminderHour ?: 9,
                    reminderMinute = settings?.billReminderMinute ?: 0,
                    repeatIntervalDays = settings?.overdueReminderIntervalDays ?: 1
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
