package com.example.nummia.views.history

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.FinanceCategories
import com.example.nummia.model.transactions.MovementSearch
import com.example.nummia.model.transactions.TransactionType
import com.example.nummia.views.components.EmptyMessage
import com.example.nummia.views.components.MovementDialog
import com.example.nummia.views.components.MovementRow
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import com.example.nummia.views.components.formatMonthHeader
import com.example.nummia.views.components.formatDateOnly
import com.example.nummia.views.components.formatWeekHeader
import com.example.nummia.views.components.isWithinDateRange
import com.example.nummia.views.components.monthStart
import com.example.nummia.views.components.weekStart
import java.util.Calendar

/** Muestra el historial completo y permite registrar un nuevo ingreso o gasto. */
@Composable
fun HistoryScreen(
    movements: List<MoneyMovement>,
    onAddMovement: (String, String?, Long, String, TransactionType, Long) -> Unit,
    onMovementClick: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var visibleMonthCount by remember { mutableStateOf(1) }
    val currentMonthStart = remember { monthStart(System.currentTimeMillis()) }
    val searchActive = searchQuery.isNotBlank() || selectedCategory != null ||
        startDate != null || endDate != null
    val validDateRange = startDate == null || endDate == null || startDate!! <= endDate!!
    val matchingMovements = remember(movements, searchQuery, selectedCategory, startDate, endDate) {
        movements.filter { movement ->
            MovementSearch.matches(movement, searchQuery) &&
            (selectedCategory == null || movement.category == selectedCategory) &&
            movement.timestamp.isWithinDateRange(startDate, endDate)
        }
    }
    val visibleStart = Calendar.getInstance().apply {
        timeInMillis = currentMonthStart
        add(Calendar.MONTH, -(visibleMonthCount - 1))
    }.timeInMillis
    val nextMonthStart = Calendar.getInstance().apply {
        timeInMillis = currentMonthStart
        add(Calendar.MONTH, 1)
    }.timeInMillis
    val visibleMovements = if (!validDateRange) {
        emptyList()
    } else if (searchActive) {
        matchingMovements
    } else {
        matchingMovements.filter { it.timestamp >= visibleStart && it.timestamp < nextMonthStart }
    }
    val hasOlderMovements = !searchActive && movements.any { it.timestamp < visibleStart }

    ScreenColumn {
        ScreenHeading("Movimientos", "Todos tus ingresos y gastos") {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Ajustes")
            }
        }
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Registrar movimiento")
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar movimientos") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Todas") }
            )
            FinanceCategories.all.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = if (selectedCategory == category) null else category
                    },
                    label = { Text(category) }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(context, startDate, System.currentTimeMillis()) { startDate = it }
                }
            ) {
                Text(startDate?.let(::formatDateOnly) ?: "Desde")
            }
            androidx.compose.material3.OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    showDatePicker(context, endDate, System.currentTimeMillis()) { endDate = it }
                }
            ) {
                Text(endDate?.let(::formatDateOnly) ?: "Hasta")
            }
            if (startDate != null || endDate != null) {
                TextButton(onClick = {
                    startDate = null
                    endDate = null
                }) {
                    Text("Limpiar")
                }
            }
        }
        if (!validDateRange) {
            Text(
                "La fecha «Desde» debe ser anterior o igual a «Hasta».",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (visibleMovements.isEmpty()) {
            EmptyMessage(
                if (!validDateRange) "Revisa el intervalo de fechas seleccionado."
                else if (searchActive) "No hay movimientos que coincidan con los filtros."
                else "Todavía no hay movimientos este mes."
            )
        } else {
            val groupedByMonth = visibleMovements
                .sortedByDescending { it.timestamp }
                .groupBy { monthStart(it.timestamp) }

            groupedByMonth.forEach { (monthTimestamp, monthMovements) ->
                CenteredDateDivider(formatMonthHeader(monthTimestamp))
                val groupedByWeek = monthMovements.groupBy { weekStart(it.timestamp) }
                groupedByWeek.forEach { (weekTimestamp, weekMovements) ->
                    CenteredDateDivider(formatWeekHeader(weekTimestamp), monthHeader = false)
                    weekMovements.sortedByDescending { it.timestamp }.forEach { movement ->
                        MovementRow(movement, onClick = { onMovementClick(movement.id) })
                    }
                }
            }
        }
        if (hasOlderMovements) {
            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { visibleMonthCount++ }
            ) {
                Text("Mostrar el mes anterior")
            }
        }
    }
    if (showDialog) {
        MovementDialog(
            onDismiss = { showDialog = false },
            onSave = { title, description, amount, category, type, timestamp ->
                onAddMovement(title, description, amount, category, type, timestamp)
                showDialog = false
            }
        )
    }
}

private fun showDatePicker(
    context: android.content.Context,
    current: Long?,
    fallback: Long,
    onSelected: (Long) -> Unit
) {
    val initial = Calendar.getInstance().apply { timeInMillis = current ?: fallback }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val selected = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onSelected(selected.timeInMillis)
        },
        initial.get(Calendar.YEAR),
        initial.get(Calendar.MONTH),
        initial.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
private fun CenteredDateDivider(label: String, monthHeader: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            label,
            style = if (monthHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            color = if (monthHeader) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
