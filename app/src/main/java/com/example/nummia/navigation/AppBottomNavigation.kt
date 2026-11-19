package com.example.nummia.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/** Muestra la navegación inferior en las rutas principales de la aplicación. */
@Composable
internal fun AppBottomNavigation(
    navController: NavHostController,
    currentRoute: String
) {
    val bottomBarRoutes = setOf(
        Screen.HOME.route,
        Screen.HISTORY.route,
        Screen.BUDGETS.route,
        Screen.STATISTICS.route
    )

    if (currentRoute in bottomBarRoutes) {
        NavigationBar {
            Screen.entries.forEach { screen ->
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = null) },
                    label = { Text(screen.label) },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
