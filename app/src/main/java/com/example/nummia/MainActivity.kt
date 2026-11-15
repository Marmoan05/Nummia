package com.example.nummia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.nummia.navigation.AppNavigation

/** Activity de entrada que aplica el tema de la app y presenta la navegación principal. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val expenseId = intent.getLongExtra(
            com.example.nummia.notifications.BillReminderScheduler.EXTRA_EXPENSE_ID,
            -1L
        ).takeIf { it >= 0L }
        intent.removeExtra(com.example.nummia.notifications.BillReminderScheduler.EXTRA_EXPENSE_ID)
        showApplication(expenseId)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val expenseId = intent.getLongExtra(
            com.example.nummia.notifications.BillReminderScheduler.EXTRA_EXPENSE_ID,
            -1L
        ).takeIf { it >= 0L }
        intent.removeExtra(com.example.nummia.notifications.BillReminderScheduler.EXTRA_EXPENSE_ID)
        showApplication(expenseId)
    }

    private fun showApplication(expenseId: Long?) {
        setContent {
            AppNavigation(expenseToOpenId = expenseId)
        }
    }
}
