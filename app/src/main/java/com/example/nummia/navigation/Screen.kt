package com.example.nummia.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

/** Define rutas, etiquetas e iconos que se muestran en la navegación inferior. */
enum class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Inicio", Icons.Filled.Home),
    HISTORY("history", "Movimientos", Icons.AutoMirrored.Filled.ReceiptLong),
    BUDGETS("budgets", "Presupuesto", Icons.Filled.CalendarMonth),
    STATISTICS("statistics", "Estadísticas", Icons.Filled.Assessment)
}
