package com.example.nummia.model.transactions
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Busca movimientos por texto, importe, tipo y fecha, tolerando variaciones comunes. */
object MovementSearch {
    /** Todos los términos deben coincidir; admite prefijos y un error de escritura en palabras largas. */
    fun matches(movement: MoneyMovement, query: String): Boolean {
        val terms = normalize(query).split(whitespace).filter(String::isNotBlank)
        if (terms.isEmpty()) return true

        val searchableText = normalize(
            listOfNotNull(
                movement.title,
                movement.description,
                movement.category,
                if (movement.type == TransactionType.INCOME) "ingreso ingresos entrada" else "gasto gastos salida",
                formatAmount(movement.amountCents),
                SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("es-ES"))
                    .format(Date(movement.timestamp))
            )
                .joinToString(" ")
        )
        val words = searchableText.split(whitespace).filter(String::isNotBlank)
        return terms.all { term ->
            searchableText.contains(term) ||
                words.any { word ->
                    word.startsWith(term) ||
                        (term.length >= MIN_FUZZY_LENGTH && editDistanceAtMostOne(term, word))
                }
        }
    }

    private fun formatAmount(amountCents: Long): String {
        val whole = amountCents / 100
        val cents = (amountCents % 100).toString().padStart(2, '0')
        return "$whole $cents $amountCents"
    }

    private fun editDistanceAtMostOne(left: String, right: String): Boolean {
        if (kotlin.math.abs(left.length - right.length) > 1) return false
        var leftIndex = 0
        var rightIndex = 0
        var differences = 0
        while (leftIndex < left.length && rightIndex < right.length) {
            if (left[leftIndex] == right[rightIndex]) {
                leftIndex++
                rightIndex++
            } else {
                differences++
                if (differences > 1) return false
                when {
                    left.length > right.length -> leftIndex++
                    right.length > left.length -> rightIndex++
                    else -> {
                        leftIndex++
                        rightIndex++
                    }
                }
            }
        }
        if (leftIndex < left.length || rightIndex < right.length) differences++
        return differences <= 1
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(diacritics, "")
            .replace(nonWordCharacters, " ")
            .trim()

    private val whitespace = Regex("\\s+")
    private val diacritics = Regex("\\p{Mn}+")
    private val nonWordCharacters = Regex("[^\\p{L}\\p{N}]+")
    private const val MIN_FUZZY_LENGTH = 4
}
