package com.example.nummia.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.model.transactions.MoneyMovement
import com.example.nummia.model.transactions.TransactionType

internal val incomeColor = Color(0xFF21856B)
internal val expenseColor = Color(0xFFCB5B54)

/** Crea una columna desplazable con márgenes y separación comunes a las pantallas. */
@Composable
internal fun ScreenColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

/** Muestra el título y el subtítulo de una sección de la aplicación. */
@Composable
internal fun ScreenHeading(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}

/** Presenta un título de sección y, opcionalmente, una acción al lado derecho. */
@Composable
internal fun SectionTitle(title: String, trailing: @Composable (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Muestra una tarjeta con el total de ingresos o gastos y su icono identificativo. */
@Composable
internal fun SummaryAmountCard(
    title: String,
    cents: Long,
    isIncome: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(
                    if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = if (isIncome) incomeColor else expenseColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge)
                if (onClick != null) {
                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Ver detalle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                formatMoney(cents, LocalCurrencyCode.current),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Representa el gasto acumulado frente al límite con una barra de progreso. */
@Composable
internal fun BudgetProgressCard(title: String, spent: Long, limit: Long, detail: String) {
    val progress = if (limit > 0) (spent.toFloat() / limit).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    if (limit > 0) "${(progress * 100).toInt()}%" else "Sin límite",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (limit > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape),
                    color = if (spent > limit) expenseColor else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dibuja una fila del historial con concepto, categoría, fecha e importe. */
@Composable
internal fun MovementRow(movement: MoneyMovement, onClick: () -> Unit) {
    val isIncome = movement.type == TransactionType.INCOME
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape)
                .background(if (isIncome) incomeColor.copy(alpha = 0.12f) else expenseColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = if (isIncome) incomeColor else expenseColor
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(movement.title, fontWeight = FontWeight.Medium)
            Text("${movement.category} · ${formatDate(movement.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            (if (isIncome) "+" else "−") + formatMoney(movement.amountCents, LocalCurrencyCode.current),
            color = if (isIncome) incomeColor else expenseColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Compara visualmente un importe con el máximo compartido entre dos periodos. */
@Composable
internal fun ComparisonBar(label: String, amount: Long, maxAmount: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(92.dp), style = MaterialTheme.typography.bodySmall)
        LinearProgressIndicator(
            progress = { if (maxAmount > 0) amount.toFloat() / maxAmount else 0f },
            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

/** Muestra un mensaje neutral cuando una lista o un informe todavía no tiene datos. */
@Composable
internal fun EmptyMessage(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
