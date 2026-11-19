package com.example.nummia.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.nummia.database.FinanceDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Ejecuta las acciones rápidas de los recordatorios desde el panel de notificaciones. */
class BillReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in supportedActions) {
            Log.w(TAG, "Ignoring unsupported reminder action: $action")
            return
        }

        val expenseId = intent.getLongExtra(BillReminderScheduler.EXTRA_EXPENSE_ID, -1L)
        if (expenseId < 0L) {
            Log.w(TAG, "Ignoring reminder action with invalid expense ID: $expenseId")
            return
        }

        if (action == BillReminderScheduler.ACTION_SHOW) {
            try {
                context.startActivity(
                    Intent(context, com.example.nummia.MainActivity::class.java)
                        .putExtra(BillReminderScheduler.EXTRA_EXPENSE_ID, expenseId)
                        .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                )
                BillReminderScheduler.cancelNotification(context, expenseId)
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to open planned expense $expenseId from its reminder.", exception)
            }
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
                    val updatedExpense = when (action) {
                        BillReminderScheduler.ACTION_SNOOZE -> expense.copy(
                            reminderSnoozedUntil = System.currentTimeMillis() +
                                (settings?.reminderSnoozeHours ?: 24) * 60 * 60 * 1000L
                            )
                        BillReminderScheduler.ACTION_DISMISS ->
                            expense.copy(remindersDisabled = true)
                        else -> return@launch
                    }
                    dao.updatePlannedExpense(updatedExpense)
                    BillReminderScheduler.cancel(context, expenseId)
                    if (action == BillReminderScheduler.ACTION_SNOOZE) {
                        BillReminderScheduler.schedule(
                            context,
                            updatedExpense,
                            enabled = remindersEnabled,
                            monthlyLeadDays = settings?.monthlyReminderDaysBefore ?: 7,
                            weeklyLeadDays = settings?.weeklyReminderDaysBefore ?: 2,
                            reminderHour = settings?.billReminderHour ?: 9,
                            reminderMinute = settings?.billReminderMinute ?: 0,
                            repeatIntervalDays = settings?.overdueReminderIntervalDays ?: 1
                        )
                    }
                } else {
                    Log.w(TAG, "Planned expense $expenseId no longer exists.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to process reminder action $action for expense $expenseId.", exception)
                try {
                    reconcileReminder(context, expenseId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (recoveryException: Exception) {
                    Log.e(TAG, "Failed to recover reminder state for expense $expenseId.", recoveryException)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun reconcileReminder(context: Context, expenseId: Long) {
        val dao = FinanceDatabase.open(context).financeDao()
        val settings = dao.getSettings()
        val expense = dao.getPlannedExpenses().firstOrNull { it.id == expenseId }
        if (expense == null) {
            BillReminderScheduler.cancel(context, expenseId)
            return
        }

        BillReminderScheduler.schedule(
            context,
            expense,
            enabled = settings?.let {
                it.billRemindersEnabled && it.billPushNotificationsEnabled
            } == true,
            monthlyLeadDays = settings?.monthlyReminderDaysBefore ?: 7,
            weeklyLeadDays = settings?.weeklyReminderDaysBefore ?: 2,
            reminderHour = settings?.billReminderHour ?: 9,
            reminderMinute = settings?.billReminderMinute ?: 0,
            repeatIntervalDays = settings?.overdueReminderIntervalDays ?: 1
        )
    }

    private companion object {
        const val TAG = "BillReminderAction"
        val supportedActions = setOf(
            BillReminderScheduler.ACTION_SHOW,
            BillReminderScheduler.ACTION_SNOOZE,
            BillReminderScheduler.ACTION_DISMISS
        )
    }
}
