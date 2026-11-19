package com.example.nummia.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.nummia.R

/** Muestra la marca y la versión mientras Room prepara el estado inicial. */
@Composable
internal fun StartupLoadingScreen(
    modifier: Modifier = Modifier,
    error: String? = null,
    onRetry: () -> Unit = {}
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (error == null) {
            CircularProgressIndicator(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        } else {
            Text(
                "No se pudieron cargar los datos guardados.\n$error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Reintentar")
            }
        }
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Icono de Nummia",
            modifier = Modifier.size(88.dp).clip(CircleShape)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Versión $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
