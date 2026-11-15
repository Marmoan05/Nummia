package com.example.nummia.views.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import com.example.nummia.model.settings.supportedCurrencies

/** Permite cambiar la moneda de presentación sin convertir los importes guardados. */
@Composable
internal fun SettingsGeneral(
    currencyCode: String,
    onChangeCurrency: (String) -> Unit
) {
    val selectedCurrency = supportedCurrencies.first { it.first == currencyCode }
    var currencyMenuExpanded by remember { mutableStateOf(false) }

    CompactPreferenceRow("Moneda") {
        Box {
            OutlinedButton(onClick = { currencyMenuExpanded = true }) {
                Text("${selectedCurrency.second} (${selectedCurrency.first})")
            }
            DropdownMenu(
                expanded = currencyMenuExpanded,
                onDismissRequest = { currencyMenuExpanded = false }
            ) {
                supportedCurrencies.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text("$name ($code)") },
                        onClick = {
                            onChangeCurrency(code)
                            currencyMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
    Text(
        "Sin conversión de divisas; las monedas sin decimales redondean los importes.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
