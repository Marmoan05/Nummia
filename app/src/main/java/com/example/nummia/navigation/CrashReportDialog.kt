package com.example.nummia.navigation

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nummia.crash.CrashReportManager
import kotlinx.coroutines.launch

/** Permite revisar, copiar o compartir el informe del último cierre inesperado. */
@Composable
internal fun CrashReportPrompt(
    report: String,
    snackbarHostState: SnackbarHostState,
    onReportCleared: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val coroutineScope = rememberCoroutineScope()
    val onDismiss: () -> Unit = {
        if (CrashReportManager.clear(context)) {
            onReportCleared()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No se pudo eliminar el informe guardado.")
            }
        }
        Unit
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("La app se cerró inesperadamente") },
        text = {
            Column {
                Text(
                    "Puedes copiar el informe o compartirlo con otra app. El informe solo se envía si eliges compartir.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    report,
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Informe de error de Nummia")
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    try {
                        context.startActivity(
                            Intent.createChooser(sendIntent, "Compartir informe")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        onDismiss()
                    } catch (_: ActivityNotFoundException) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                "No hay ninguna app disponible para compartir."
                            )
                        }
                    }
                }
            ) {
                Text("Compartir")
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        if (clipboard == null) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "No se pudo acceder al portapapeles."
                                )
                            }
                        } else {
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Informe de error de Nummia", report)
                            )
                            if (CrashReportManager.clear(context)) {
                                onReportCleared()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Informe copiado al portapapeles."
                                    )
                                }
                            } else {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        "No se pudo eliminar el informe guardado."
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Copiar informe")
                }
            }
        }
    )
}
