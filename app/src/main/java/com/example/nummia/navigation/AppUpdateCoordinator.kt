package com.example.nummia.navigation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nummia.updates.AppRelease
import com.example.nummia.updates.AppUpdate
import com.example.nummia.updates.AppUpdateCheckResult
import com.example.nummia.updates.AppUpdateRepository
import com.example.nummia.updates.ReleaseNotesStore
import com.example.nummia.views.settings.ReleaseNotesDialog
import com.example.nummia.views.settings.ReleaseNotesText
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import org.json.JSONException

private const val UPDATE_PREFERENCES = "app_updates"
private const val LAST_UPDATE_CHECK = "last_check_millis"
private const val UPDATE_CHECK_INTERVAL_MILLIS = 24 * 60 * 60 * 1000L

@Composable
internal fun AppUpdateCoordinator(
    enabled: Boolean,
    readyForReleaseNotes: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    var pendingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var whatsNew by remember { mutableStateOf<AppRelease?>(null) }

    LaunchedEffect(context, currentVersion, readyForReleaseNotes) {
        if (!readyForReleaseNotes) return@LaunchedEffect

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val preferences = context.getSharedPreferences(UPDATE_PREFERENCES, 0)
        val pendingRelease =
            ReleaseNotesStore.pendingForInstalledVersion(preferences, currentVersion)
        if (pendingRelease != null) {
            try {
                whatsNew = AppUpdateRepository.changelogForVersion(currentVersion)
                    ?: pendingRelease
            } catch (error: IOException) {
                whatsNew = pendingRelease
                snackbarHostState.showSnackbar(
                    "Se mostrarán las notas guardadas de Nummia $currentVersion: ${error.message}"
                )
            }
            return@LaunchedEffect
        }
        if (ReleaseNotesStore.hasPending(preferences) ||
            !ReleaseNotesStore.shouldShowAfterUpdate(
                preferences,
                currentVersion,
                packageInfo.firstInstallTime,
                packageInfo.lastUpdateTime
            )
        ) return@LaunchedEffect

        try {
            whatsNew = AppUpdateRepository.releaseNotesForVersion(currentVersion)
        } catch (error: IOException) {
            snackbarHostState.showSnackbar(
                "No se pudieron cargar las novedades de Nummia ${currentVersion}: ${error.message}"
            )
        }
    }

    LaunchedEffect(context, currentVersion, enabled) {
        if (!enabled) {
            pendingUpdate = null
            downloadedApk = null
            return@LaunchedEffect
        }

        val preferences = context.getSharedPreferences(UPDATE_PREFERENCES, 0)
        val now = System.currentTimeMillis()
        val lastCheck = preferences.getLong(LAST_UPDATE_CHECK, 0L)
        if (now - lastCheck < UPDATE_CHECK_INTERVAL_MILLIS) return@LaunchedEffect

        try {
            when (val result = AppUpdateRepository.checkForUpdate(currentVersion)) {
                AppUpdateCheckResult.UpToDate ->
                    preferences.edit().putLong(LAST_UPDATE_CHECK, now).apply()
                is AppUpdateCheckResult.Available -> {
                    pendingUpdate = AppUpdateRepository.withChangelogNotes(
                        result.update
                    )
                    preferences.edit().putLong(LAST_UPDATE_CHECK, now).apply()
                }
            }
        } catch (error: IOException) {
            snackbarHostState.showSnackbar(
                "No se pudo comprobar la actualización: ${error.message}"
            )
        } catch (error: JSONException) {
            snackbarHostState.showSnackbar(
                "GitHub devolvió una respuesta de actualización no válida: ${error.message}"
            )
        } catch (error: IllegalArgumentException) {
            snackbarHostState.showSnackbar(
                "La versión publicada no es válida: ${error.message}"
            )
        }
    }

    val update = pendingUpdate
    val apkFile = downloadedApk
    if (update != null && apkFile == null) {
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) pendingUpdate = null
            },
            title = { Text("Actualización disponible") },
            text = {
                if (isDownloading) {
                    Text("Descargando Nummia ${update.versionName}…")
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            "Hay una nueva versión de Nummia (${update.versionName}). " +
                                "¿Quieres descargarla ahora?"
                        )
                        ReleaseNotesText(update.releaseNotes)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = {
                        scope.launch {
                            isDownloading = true
                            try {
                                downloadedApk =
                                    AppUpdateRepository.downloadUpdate(context, update)
                            } catch (error: IOException) {
                                snackbarHostState.showSnackbar(
                                    "No se pudo descargar la actualización: ${error.message}"
                                )
                            } catch (error: JSONException) {
                                snackbarHostState.showSnackbar(
                                    "La respuesta de GitHub no es válida: ${error.message}"
                                )
                            } catch (error: IllegalArgumentException) {
                                snackbarHostState.showSnackbar(
                                    "La versión publicada no es válida: ${error.message}"
                                )
                            } finally {
                                isDownloading = false
                            }
                        }
                    }
                ) {
                    Text(if (isDownloading) "Descargando…" else "Descargar")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isDownloading,
                    onClick = { pendingUpdate = null }
                ) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (update != null && apkFile != null) {
        AlertDialog(
            onDismissRequest = {
                pendingUpdate = null
                downloadedApk = null
            },
            title = { Text("Actualización disponible") },
            text = {
                Text(
                    "Nummia ${update.versionName} se ha descargado y verificado. " +
                        "¿Quieres iniciar la instalación? Android te pedirá confirmarla."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                )
                            } catch (error: ActivityNotFoundException) {
                                pendingUpdate = null
                                downloadedApk = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudieron abrir los ajustes de instalación: ${error.message}"
                                    )
                                }
                            }
                        } else {
                            try {
                                ReleaseNotesStore.savePending(
                                    context.getSharedPreferences(UPDATE_PREFERENCES, 0),
                                    update
                                )
                                context.startActivity(
                                    AppUpdateRepository.createInstallIntent(context, apkFile)
                                )
                                pendingUpdate = null
                                downloadedApk = null
                            } catch (error: ActivityNotFoundException) {
                                ReleaseNotesStore.clearPending(
                                    context.getSharedPreferences(UPDATE_PREFERENCES, 0),
                                    update.versionName
                                )
                                pendingUpdate = null
                                downloadedApk = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudo abrir el instalador de Android: ${error.message}"
                                    )
                                }
                            } catch (error: IllegalArgumentException) {
                                ReleaseNotesStore.clearPending(
                                    context.getSharedPreferences(UPDATE_PREFERENCES, 0),
                                    update.versionName
                                )
                                pendingUpdate = null
                                downloadedApk = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudo preparar el APK para instalarlo: ${error.message}"
                                    )
                                }
                            } catch (error: IOException) {
                                ReleaseNotesStore.clearPending(
                                    context.getSharedPreferences(UPDATE_PREFERENCES, 0),
                                    update.versionName
                                )
                                pendingUpdate = null
                                downloadedApk = null
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudieron guardar las novedades antes de instalar: ${error.message}"
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Instalar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingUpdate = null
                        downloadedApk = null
                    }
                ) {
                    Text("Ahora no")
                }
            }
        )
    }

    whatsNew?.let { release ->
        ReleaseNotesDialog(
            release = release,
            title = "Novedades de Nummia",
            onDismiss = {
                context.getSharedPreferences(UPDATE_PREFERENCES, 0)
                    .let { ReleaseNotesStore.markSeen(it, currentVersion) }
                whatsNew = null
            }
        )
    }
}
