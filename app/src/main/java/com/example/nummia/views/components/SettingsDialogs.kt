package com.example.nummia.views.components

import android.app.TimePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Abre el selector del sistema en formato de 24 horas y devuelve la hora elegida. */
@Composable
internal fun ReminderTimePickerButton(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, selectedHour, selectedMinute ->
                    onTimeSelected(selectedHour, selectedMinute)
                },
                hour,
                minute,
                true
            ).show()
        }
    ) {
        Text("%02d:%02d".format(hour, minute))
    }
}
