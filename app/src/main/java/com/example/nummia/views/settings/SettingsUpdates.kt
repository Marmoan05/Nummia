package com.example.nummia.views.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nummia.updates.AppUpdate
import com.example.nummia.updates.AppUpdateCheckResult
import com.example.nummia.updates.AppUpdateRepository
import com.example.nummia.updates.ReleaseNotesStore
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import org.json.JSONException

@Composable
internal fun SettingsUpdates(
    autoUpdateChecksEnabled: Boolean,
    onChangeAutoUpdateChecks: (Boolean) -> Unit,
    onOpenReleaseNotes: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    var update by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    SettingsCard("Actualizaciones") {
        Text("Versión instalada: $currentVersion", style = MaterialTheme.typography.bodyMedium)
        SettingSwitchRow(
            title = "Detectar actualizaciones automáticamente",
            subtitle = "Buscar al iniciar, como máximo una vez cada 24 horas",
            checked = autoUpdateChecksEnabled,
            onCheckedChange = onChangeAutoUpdateChecks
        )
        TextButton(
            onClick = onOpenReleaseNotes
        ) {
            Text("Historial de cambios")
        }
        TextButton(
            enabled = !isBusy,
            onClick = {
                scope.launch {
                    isBusy = true
                    status = "Buscando actualizaciones…"
                    isError = false
                    update = null
                    downloadedApk = null
                    try {
                        when (val result = AppUpdateRepository.checkForUpdate(currentVersion)) {
                            AppUpdateCheckResult.UpToDate -> status = "Ya tienes la última versión."
                            is AppUpdateCheckResult.Available -> {
                                val availableUpdate = AppUpdateRepository.withChangelogNotes(
                                    result.update
                                )
                                update = availableUpdate
                                status = "Está disponible Nummia ${availableUpdate.versionName}."
                            }
                        }
                    } catch (error: IOException) {
                        status = "No se pudo buscar la actualización: ${error.message}"
                        isError = true
                    } catch (error: JSONException) {
                        status = "La respuesta del servidor no es válida: ${error.message}"
                        isError = true
                    } catch (error: IllegalArgumentException) {
                        status = "La versión publicada no es válida: ${error.message}"
                        isError = true
                    } finally {
                        isBusy = false
                    }
                }
            }
        ) {
            Text(if (isBusy) "Procesando…" else "Buscar actualizaciones")
        }

        update?.let { availableUpdate ->
            ReleaseNotesText(availableUpdate.releaseNotes)
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                onClick = {
                    scope.launch {
                        isBusy = true
                        status = "Descargando Nummia ${availableUpdate.versionName}…"
                        isError = false
                        try {
                            downloadedApk = AppUpdateRepository.downloadUpdate(context, availableUpdate)
                            status = "Descarga verificada. Ya puedes instalar la actualización."
                        } catch (error: IOException) {
                            status = "No se pudo descargar la actualización: ${error.message}"
                            isError = true
                        } finally {
                            isBusy = false
                        }
                    }
                }
            ) {
                Text("Descargar actualización")
            }
        }

        downloadedApk?.let { apkFile ->
            update?.let { availableUpdate ->
                Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        try {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                            status = "Permite instalar aplicaciones desde Nummia y vuelve para pulsar Instalar."
                            isError = false
                        } catch (error: ActivityNotFoundException) {
                            status = "No se pudieron abrir los ajustes de instalación: ${error.message}"
                            isError = true
                        }
                    } else {
                        try {
                            val preferences = context.getSharedPreferences("app_updates", 0)
                            ReleaseNotesStore.savePending(preferences, availableUpdate)
                            context.startActivity(
                                AppUpdateRepository.createInstallIntent(context, apkFile)
                            )
                            status = "Confirma la instalación en el instalador de Android."
                            isError = false
                        } catch (error: ActivityNotFoundException) {
                            context.getSharedPreferences("app_updates", 0).let {
                                ReleaseNotesStore.clearPending(it, availableUpdate.versionName)
                            }
                            status = "No se pudo abrir el instalador de Android: ${error.message}"
                            isError = true
                        } catch (error: IllegalArgumentException) {
                            context.getSharedPreferences("app_updates", 0).let {
                                ReleaseNotesStore.clearPending(it, availableUpdate.versionName)
                            }
                            status = "No se pudo preparar el APK para instalarlo: ${error.message}"
                            isError = true
                        } catch (error: IOException) {
                            context.getSharedPreferences("app_updates", 0).let {
                                ReleaseNotesStore.clearPending(it, availableUpdate.versionName)
                            }
                            status = "No se pudieron guardar las novedades antes de instalar: ${error.message}"
                            isError = true
                        }
                    }
                }
                ) {
                    Text("Instalar actualización")
                }
            }
        }

        status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "La instalación requiere tu confirmación y puede pedir permiso para instalar desde esta fuente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

}
