package com.rippipupil.clothify.ui

import com.rippipupil.clothify.data.sources.SourceFailure
import com.rippipupil.clothify.data.style.FailureReason
import com.rippipupil.clothify.domain.Source

fun FailureReason.message(): String = when (this) {
    FailureReason.NO_INTERNET -> "No he podido analizar la foto: revisa tu conexión."
    FailureReason.QUOTA_EXCEEDED -> "Has agotado el análisis gratuito de hoy. Vuelve mañana o elige las etiquetas a mano."
    FailureReason.NO_TAGS -> "No he reconocido ropa en la foto. Elige las etiquetas a mano."
    FailureReason.MISSING_KEY -> "Falta la clave de Gemini: elige las etiquetas a mano (mira el README)."
    FailureReason.MODEL_UNAVAILABLE -> "El análisis en el móvil no está disponible en esta versión. Elige las etiquetas a mano."
    FailureReason.UNKNOWN -> "Algo ha fallado al analizar la foto. Reintenta o elige las etiquetas a mano."
}

/** Aviso corto para fuentes que no han respondido; null si no hay nada que avisar. */
fun sourceWarning(failures: Map<Source, SourceFailure>): String? {
    val relevant = failures.filterValues { it != SourceFailure.DISABLED }
    if (relevant.isEmpty()) return null
    val offline = if (relevant.values.any { it == SourceFailure.NO_INTERNET }) listOf("Sin conexión") else emptyList()
    val others = relevant.filterValues { it != SourceFailure.NO_INTERNET }.map { (source, reason) ->
        if (reason == SourceFailure.BUDGET_EXHAUSTED) "${source.label}: búsquedas de hoy agotadas" else "${source.label} no responde ahora"
    }
    return (offline + others).joinToString(" · ")
}

fun formatPrice(eur: Double): String =
    if (eur % 1.0 == 0.0) "${eur.toLong()} €" else String.format(java.util.Locale("es", "ES"), "%.2f €", eur)
