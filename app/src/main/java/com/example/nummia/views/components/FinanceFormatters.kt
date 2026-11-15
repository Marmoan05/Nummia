package com.example.nummia.views.components

import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Currency
import com.example.nummia.model.settings.currencyMinorDigits
import androidx.compose.runtime.compositionLocalOf

/** Moneda que usan los componentes de presentación si no reciben otra configuración. */
internal val LocalCurrencyCode = compositionLocalOf { "EUR" }
/** Convierte un importe a unidades internas de centésima o devuelve null si no es válido. */
internal fun parseCents(value: String, currencyCode: String = "EUR"): Long? {
    val normalized = value.trim().replace(',', '.')
    val fractionDigits = currencyMinorDigits(currencyCode)
    val pattern = if (fractionDigits == 0) Regex("""\d+""")
    else Regex("""\d+(\.\d{1,$fractionDigits})?""")
    if (!normalized.matches(pattern)) return null
    return try {
        BigDecimal(normalized).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        null
    }
}

/** Formatea el importe con símbolo y precisión de la moneda elegida. */
internal fun formatMoney(cents: Long, currencyCode: String = "EUR"): String {
    val fractionDigits = currencyMinorDigits(currencyCode)
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).apply {
        currency = Currency.getInstance(currencyCode)
        minimumFractionDigits = fractionDigits
        maximumFractionDigits = fractionDigits
    }.format(cents / 100.0)
}

/** Convierte el importe guardado a texto editable con la precisión de la moneda actual. */
internal fun formatInputAmount(cents: Long, currencyCode: String): String =
    BigDecimal.valueOf(cents, 2)
        .setScale(currencyMinorDigits(currencyCode), java.math.RoundingMode.HALF_UP)
        .toPlainString()

/** Muestra hoy, ayer o anteayer con hora; para fechas anteriores muestra día y mes. */
internal fun formatDate(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val zone = ZoneId.systemDefault()
    val movementDay = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val daysAgo = ChronoUnit.DAYS.between(movementDay, today).toInt()
    val label = when (daysAgo) {
        0 -> "hoy"
        1 -> "ayer"
        2 -> "anteayer"
        else -> null
    }
    return if (label != null) {
        "$label ${SimpleDateFormat("HH:mm", Locale.forLanguageTag("es-ES")).format(Date(timestamp))}"
    } else {
        SimpleDateFormat("d MMM", Locale.forLanguageTag("es-ES")).format(Date(timestamp))
    }
}

/** Formatea una marca de tiempo completa para mostrarla en el selector de fecha y hora. */
internal fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES")).format(Date(timestamp))

/** Formatea una fecha local sin incluir la hora. */
internal fun formatDateOnly(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES")).format(Date(timestamp))

/** Comprueba si una marca temporal cae dentro de un intervalo local inclusivo. */
internal fun Long.isWithinDateRange(startDate: Long?, endDate: Long?): Boolean {
    if (startDate != null && this < startDate) return false
    if (endDate != null) {
        val endExclusive = Calendar.getInstance().apply {
            timeInMillis = endDate
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        if (this >= endExclusive) return false
    }
    return true
}

/** Indica si la marca de tiempo está en la semana actual, de lunes a domingo. */
internal fun Long.isInCurrentWeek(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    return this >= start && this < calendar.timeInMillis
}

/** Indica si la marca de tiempo está dentro del mes natural actual. */
internal fun Long.isInCurrentMonth(): Boolean {
    val movement = Calendar.getInstance().apply { timeInMillis = this@isInCurrentMonth }
    val current = Calendar.getInstance()
    return movement.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
        movement.get(Calendar.MONTH) == current.get(Calendar.MONTH)
}

/** Indica si la marca de tiempo está dentro del mes natural anterior. */
internal fun Long.isInPreviousMonth(): Boolean {
    val movement = Calendar.getInstance().apply { timeInMillis = this@isInPreviousMonth }
    val previous = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    return movement.get(Calendar.YEAR) == previous.get(Calendar.YEAR) &&
        movement.get(Calendar.MONTH) == previous.get(Calendar.MONTH)
}

/** Indica si el vencimiento fue antes del día de hoy en la zona horaria local. */
internal fun Long.isOverdue(now: Long = System.currentTimeMillis()): Boolean {
    val dueDay = Calendar.getInstance().apply {
        timeInMillis = this@isOverdue
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val today = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return dueDay.timeInMillis < today.timeInMillis
}

/** Devuelve el inicio local del mes natural de una marca temporal. */
internal fun monthStart(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Devuelve el lunes a medianoche de la semana de una marca temporal. */
internal fun weekStart(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Formatea el separador mensual, por ejemplo «septiembre de 2026». */
internal fun formatMonthHeader(timestamp: Long): String =
    SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))
        .format(Date(timestamp))
        .replaceFirstChar { it.lowercase(Locale.forLanguageTag("es-ES")) }

/** Formatea el separador semanal usando el lunes de esa semana. */
internal fun formatWeekHeader(timestamp: Long): String =
    "Semana del " + SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        .format(Date(timestamp))
