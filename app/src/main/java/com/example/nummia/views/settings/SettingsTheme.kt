package com.example.nummia.views.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.nummia.model.settings.AppThemePalette
import com.example.nummia.ui.theme.MyApplicationTheme

/** Muestra la paleta activa y permite cambiarla con una vista previa de sus colores. */
@Composable
internal fun SettingsTheme(
    selectedTheme: AppThemePalette,
    onChangeTheme: (String) -> Unit
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Text("Paleta", style = MaterialTheme.typography.labelLarge)
    PalettePreview()
    Box {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { themeMenuExpanded = true }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PaletteSwatch(selectedTheme)
                Text(selectedTheme.label, modifier = Modifier.weight(1f))
                Text("Cambiar", color = MaterialTheme.colorScheme.primary)
            }
        }
        DropdownMenu(
            expanded = themeMenuExpanded,
            onDismissRequest = { themeMenuExpanded = false }
        ) {
            AppThemePalette.entries.forEach { palette ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PaletteSwatch(palette)
                            Text(palette.label)
                        }
                    },
                    onClick = {
                        onChangeTheme(palette.id)
                        themeMenuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PalettePreview() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(9.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    "  Saldo disponible",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "1.250,00",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier.weight(0.65f).fillMaxHeight().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Box(
                    modifier = Modifier.weight(0.35f).fillMaxHeight().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(palette: AppThemePalette) {
    MyApplicationTheme(palette = palette) {
        Row(
            modifier = Modifier.size(width = 42.dp, height = 24.dp).clip(RoundedCornerShape(6.dp))
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.secondary)
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
