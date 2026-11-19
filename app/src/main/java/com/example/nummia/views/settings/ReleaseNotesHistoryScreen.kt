package com.example.nummia.views.settings

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.nummia.updates.AppRelease
import com.example.nummia.updates.AppUpdateRepository
import com.example.nummia.views.components.ScreenColumn
import com.example.nummia.views.components.ScreenHeading
import java.io.IOException

@Composable
internal fun ReleaseNotesHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val currentVersion = remember(context) { installedVersion(context) }
    var releases by remember { mutableStateOf<List<AppRelease>>(emptyList()) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context, currentVersion) {
        try {
            releases = AppUpdateRepository.changelogHistory()
            selectedIndex = releases.indexOfFirst {
                it.versionName == currentVersion
            }.coerceAtLeast(0)
        } catch (exception: IOException) {
            error = "No se pudo leer el historial de cambios: ${exception.message}"
        } finally {
            isLoading = false
        }
    }

    ScreenColumn {
        TextButton(onClick = onBack) { Text("‹  Volver a Ajustes") }
        ScreenHeading("Historial de cambios", "Novedades de cada versión")

        when {
            isLoading -> CircularProgressIndicator()
            error != null -> Text(error.orEmpty())
            releases.isEmpty() -> Text("No hay versiones documentadas.")
            else -> {
                val release = releases[selectedIndex]
                ScreenHeading(release.title, "Versión ${release.versionName}")
                ReleaseNotesText(release.releaseNotes)
                if (selectedIndex + 1 < releases.size) {
                    Button(onClick = { selectedIndex++ }) {
                        Text("Versión anterior")
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun installedVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
