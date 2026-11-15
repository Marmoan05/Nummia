package com.example.nummia.views.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.components.MovementDialog
import com.example.nummia.views.components.LocalCurrencyCode
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.formatDate
import com.example.nummia.views.components.formatMoney

/** Presenta los datos de un movimiento seleccionado en el historial. */
@Composable
fun TransactionDetailScreen(
    movement: MoneyMovement?,
    onBack: () -> Unit,
    onEdit: (MoneyMovement) -> Unit,
    onDelete: (Long) -> Unit
) {
    val currencyCode = LocalCurrencyCode.current
    var showEditDialog by remember(movement?.id) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(movement?.id) { mutableStateOf(false) }
    ScreenColumn {
        TextButton(onClick = onBack) {
            Text("Volver")
        }
        if (movement == null) {
            ScreenHeading("Movimiento no encontrado", "Este movimiento ya no está disponible.")
        } else {
            ScreenHeading("Detalle del movimiento", movement.title)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    DetailValue(
                        label = "Importe",
                        value = (if (movement.type == TransactionType.INCOME) "+" else "−") +
                            formatMoney(movement.amountCents, currencyCode),
                        emphasized = true
                    )
                    if (!movement.description.isNullOrBlank()) {
                        DetailValue(label = "Descripción", value = movement.description)
                    }
                    DetailValue(
                        label = "Tipo",
                        value = if (movement.type == TransactionType.INCOME) "Ingreso" else "Gasto"
                    )
                    DetailValue(label = "Categoría", value = movement.category)
                    DetailValue(label = "Fecha", value = formatDate(movement.timestamp))
                    Button(onClick = { showEditDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar movimiento")
                    }
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar movimiento", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showEditDialog && movement != null) {
        MovementDialog(
            initialMovement = movement,
            onDismiss = { showEditDialog = false },
            onSave = { title, description, amount, category, type, timestamp ->
                onEdit(movement.copy(
                    title = title,
                    description = description,
                    amountCents = amount,
                    category = category,
                    type = type,
                    timestamp = timestamp
                ))
                showEditDialog = false
            }
        )
    }

    if (showDeleteConfirmation && movement != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar movimiento") },
            text = { Text("¿Seguro que quieres eliminar «${movement.title}»? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { onDelete(movement.id) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetailValue(label: String, value: String, emphasized: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium
        )
    }
}