package com.example.nummia.model.settings
import java.util.Currency
import java.math.BigDecimal
import java.math.RoundingMode

/** Monedas disponibles; solo se incluyen monedas con cero o dos decimales. */
val supportedCurrencies = listOf(
    "EUR" to "Euro",
    "USD" to "Dólar estadounidense",
    "GBP" to "Libra esterlina",
    "JPY" to "Yen japonés",
    "KRW" to "Won surcoreano",
    "CHF" to "Franco suizo",
    "CAD" to "Dólar canadiense",
    "AUD" to "Dólar australiano",
    "INR" to "Rupia india",
    "BRL" to "Real brasileño",
    "MXN" to "Peso mexicano"
)

/** Devuelve el número de decimales monetarios admitidos por una moneda disponible. */
fun currencyMinorDigits(currencyCode: String): Int =
    Currency.getInstance(currencyCode).defaultFractionDigits

/** Redondea las centésimas internas a la precisión de la moneda sin conversión cambiaria. */
fun normalizeAmountForCurrency(amountCents: Long, currencyCode: String): Long =
    BigDecimal.valueOf(amountCents, 2)
        .setScale(currencyMinorDigits(currencyCode), RoundingMode.HALF_UP)
        .movePointRight(2)
        .longValueExact()
