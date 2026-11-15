package com.example.nummia.views.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.nummia.model.transactions.FinanceCategories
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import java.util.Calendar

/** Recoge los datos y la fecha de un ingreso o gasto nuevo o editado. */
@Composable
internal fun MovementDialog(
    initialMovement: MoneyMovement? = null,
    onDismiss: () -> Unit,
    onSave: (String, String?, Long, String, TransactionType, Long) -> Unit
) {
    val context = LocalContext.current
    val currencyCode = LocalCurrencyCode.current
    var type by remember(initialMovement?.id) {
        mutableStateOf(initialMovement?.type ?: TransactionType.EXPENSE)
    }
    var title by remember(initialMovement?.id) { mutableStateOf(initialMovement?.title.orEmpty()) }
    var description by remember(initialMovement?.id) {
        mutableStateOf(initialMovement?.description.orEmpty())
    }
    var amount by remember(initialMovement?.id) {
        mutableStateOf(initialMovement?.let { formatInputAmount(it.amountCents, currencyCode) }.orEmpty())
    }
    var category by remember(initialMovement?.id) {
        mutableStateOf(initialMovement?.category ?: FinanceCategories.all.first())
    }
    var useCustomDateTime by remember(initialMovement?.id) { mutableStateOf(initialMovement != null) }
    var selectedTimestamp by remember(initialMovement?.id) {
        mutableLongStateOf(initialMovement?.timestamp ?: System.currentTimeMillis())
    }
    val cents = parseCents(amount, currencyCode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMovement == null) "Añadir movimiento" else "Editar movimiento") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TransactionType.EXPENSE,
                        onClick = { type = TransactionType.EXPENSE },
                        label = { Text("Gasto") }
                    )
                    FilterChip(
                        selected = type == TransactionType.INCOME,
                        onClick = { type = TransactionType.INCOME },
                        label = { Text("Ingreso") }
                    )
                }
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true)
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Descripción (opcional)") },
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    amount,
                    { amount = it },
                    label = { Text("Importe ($currencyCode)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                CategoryPicker(category, { category = it })
                Text("Fecha y hora")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useCustomDateTime,
                        onClick = { useCustomDateTime = false },
                        label = { Text("Ahora") }
                    )
                    FilterChip(
                        selected = useCustomDateTime,
                        onClick = { useCustomDateTime = true },
                        label = { Text("Elegir") }
                    )
                }
                if (useCustomDateTime) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val selected = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val chosenDate = Calendar.getInstance().apply {
                                        timeInMillis = selectedTimestamp
                                        set(Calendar.DAY_OF_MONTH, 1)
                                        set(year, month, day)
                                    }
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            chosenDate.set(Calendar.HOUR_OF_DAY, hour)
                                            chosenDate.set(Calendar.MINUTE, minute)
                                            chosenDate.set(Calendar.SECOND, 0)
                                            chosenDate.set(Calendar.MILLISECOND, 0)
                                            selectedTimestamp = chosenDate.timeInMillis
                                        },
                                        selected.get(Calendar.HOUR_OF_DAY),
                                        selected.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                selected.get(Calendar.YEAR),
                                selected.get(Calendar.MONTH),
                                selected.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Text(formatDateTime(selectedTimestamp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && cents != null && cents > 0,
                onClick = {
                    cents?.let {
                        onSave(
                            title.trim(),
                            description.trim().takeIf { it.isNotEmpty() },
                            it,
                            category,
                            type,
                            if (useCustomDateTime) selectedTimestamp else System.currentTimeMillis()
                        )
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
