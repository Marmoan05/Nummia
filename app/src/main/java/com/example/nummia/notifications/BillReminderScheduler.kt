package com.example.nummia.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.nummia.R
import com.example.nummia.MainActivity
import com.example.nummia.model.plannedexpenses.PlannedExpense

/** Programa recordatorios diarios desde el periodo previo hasta que se pague la factura. */
object BillReminderScheduler {
    const val CHANNEL_ID = "bill_reminders"
    const val EXTRA_EXPENSE_ID = "planned_expense_id"
    const val ACTION_SNOOZE = "com.example.nummia.SNOOZE_BILL"
    const val ACTION_DISMISS = "com.example.nummia.DISMISS_BILL_REMINDERS"
    const val ACTION_SHOW = "com.example.nummia.SHOW_BILL"

    /** Crea el canal de notificaciones requerido por Android 8 o posterior. */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de facturas",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos de facturas próximas y vencidas."
            }
            context.getSystemService(android.app.NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /** Sincroniza las alarmas con las facturas actuales y cancela las que ya se eliminaron. */
    fun scheduleAll(
        context: Context,
        expenses: List<PlannedExpense>,
        enabled: Boolean,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        repeatIntervalDays: Int = 1
    ) {
        createChannel(context)
        val preferences = context.getSharedPreferences("bill_reminders", Context.MODE_PRIVATE)
        val previousIds = preferences.getStringSet("scheduled_ids", emptySet()).orEmpty().toSet()
        val currentIds = expenses.map { it.id.toString() }.toSet()
        (previousIds - currentIds).forEach {
            it.toLongOrNull()?.let { expenseId -> cancel(context, expenseId) }
        }
        preferences.edit().putStringSet("scheduled_ids", currentIds).apply()
        expenses.forEach { expense ->
            schedule(
                context, expense, enabled, monthlyLeadDays, weeklyLeadDays,
                reminderHour, reminderMinute, repeatIntervalDays
            )
        }
    }

    /** Sustituye la alarma de una factura o la cancela si no debe generar avisos. */
    fun schedule(
        context: Context,
        expense: PlannedExpense,
        enabled: Boolean,
        monthlyLeadDays: Int = 7,
        weeklyLeadDays: Int = 2,
        reminderHour: Int = 9,
        reminderMinute: Int = 0,
        repeatIntervalDays: Int = 1
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = alarmPendingIntent(context, expense.id)
        alarmManager.cancel(pendingIntent)
        if (!enabled || expense.remindersDisabled || expense.nextDueAt <= 0L) {
            NotificationManagerCompat.from(context).cancel(notificationId(expense.id))
            return
        }

        val reminderAt = BillReminderCalculator.nextReminderAt(
            expense, System.currentTimeMillis(), monthlyLeadDays, weeklyLeadDays,
            reminderHour, reminderMinute, repeatIntervalDays
        ) ?: return
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderAt,
            pendingIntent
        )
    }

    /** Cancela tanto la alarma pendiente como la notificación visible de la factura. */
    fun cancel(context: Context, expenseId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(alarmPendingIntent(context, expenseId))
        cancelNotification(context, expenseId)
    }

    fun cancelNotification(context: Context, expenseId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(expenseId))
    }

    /** Publica el aviso con accesos a detalles, posposición y desactivación de recordatorios. */
    fun showNotification(context: Context, expense: PlannedExpense) {
        createChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_EXPENSE_ID, expense.id)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode(expense.id, 3),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = actionIntent(context, expense.id, ACTION_SNOOZE, 1)
        val dismissIntent = actionIntent(context, expense.id, ACTION_DISMISS, 2)
        val detailsIntent = actionIntent(context, expense.id, ACTION_SHOW, 5)
        val overdue = BillReminderCalculator.isOverdue(expense, System.currentTimeMillis())
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Factura pendiente: ${expense.title}")
            .setContentText(
                if (overdue) {
                    "Está vencida. Pulsa para ver sus detalles."
                } else {
                    "Se acerca la fecha de pago."
                }
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (overdue) {
                        "${expense.title} está vencida. Pulsa para abrir los detalles de la factura."
                    } else {
                        "${expense.title} vence pronto. Pulsa para abrir los detalles de la factura."
                    }
                )
            )
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Ver detalles", detailsIntent)
            .addAction(0, "Más tarde", snoozeIntent)
            .addAction(0, "No recordar", dismissIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(expense.id), notification)
        } catch (_: SecurityException) {
            Log.w("BillReminder", "Notification permission is unavailable for bill ${expense.id}")
        }
    }

    private fun alarmPendingIntent(context: Context, expenseId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(expenseId, 0),
            Intent(context, BillReminderReceiver::class.java)
                .setAction("com.example.nummia.REMIND_BILL")
                .setData(android.net.Uri.parse("bill-reminder://$expenseId"))
                .putExtra(EXTRA_EXPENSE_ID, expenseId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun actionIntent(
        context: Context,
        expenseId: Long,
        action: String,
        salt: Int
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(expenseId, salt),
            Intent(context, BillReminderActionReceiver::class.java)
                .setAction(action)
                .setData(android.net.Uri.parse("bill-action://$action/$expenseId"))
                .putExtra(EXTRA_EXPENSE_ID, expenseId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun notificationId(expenseId: Long): Int = requestCode(expenseId, 4)

    private fun requestCode(expenseId: Long, salt: Int): Int =
        ((expenseId xor (expenseId ushr 32)).toInt() * 31) + salt

}
