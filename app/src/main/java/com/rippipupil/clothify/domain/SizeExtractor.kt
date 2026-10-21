package com.rippipupil.clothify.domain

/** Extrae una talla ("M", "XL", "32", "42"…) de un título, descripción o size_title. */
object SizeExtractor {

    private const val LETTER = "XXXL|XXL|XL|XXS|XS|S|M|L|[2-3]XL"
    private val prefixed = Regex("\\b(?:TALLA|TALLE|SIZE|T\\.)\\s*:?\\s*($LETTER|\\d{2})\\b")
    private val leading = Regex("^\\s*($LETTER)\\b")
    private val number = Regex("(?<![\\d.,])(\\d{2})(?![\\d.,]|\\s*(?:€|EUR|CM|%|MM|KG))")

    fun fromText(text: String): String? {
        val upper = text.uppercase()
        prefixed.find(upper)?.let { return canonical(it.groupValues[1]) }
        leading.find(upper)?.let { return canonical(it.groupValues[1]) }
        number.findAll(upper)
            .map { it.groupValues[1] }
            .firstOrNull { it.toInt() in 28..47 }
            ?.let { return it }
        return null
    }

    private fun canonical(size: String): String = when (size) {
        "2XL" -> "XXL"
        "3XL" -> "XXXL"
        else -> size
    }
}
