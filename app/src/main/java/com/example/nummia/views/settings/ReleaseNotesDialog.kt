package com.example.nummia.views.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nummia.updates.AppRelease
import com.example.nummia.updates.ReleaseNotesBlock
import com.example.nummia.updates.ReleaseNotesFormatter

@Composable
internal fun ReleaseNotesDialog(
    release: AppRelease,
    title: String = release.title,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Versión ${release.versionName}")
                ReleaseNotesText(release.releaseNotes)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
internal fun ReleaseNotesText(notes: String) {
    val blocks = remember(notes) { ReleaseNotesFormatter.parse(notes) }
    if (blocks.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is ReleaseNotesBlock.Heading -> Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    is ReleaseNotesBlock.Paragraph -> Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    is ReleaseNotesBlock.Bullet -> Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("•", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = block.text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    } else {
        Text("Esta versión no incluye notas de publicación.")
    }
}
