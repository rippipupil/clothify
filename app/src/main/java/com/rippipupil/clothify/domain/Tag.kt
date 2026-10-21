package com.rippipupil.clothify.domain

enum class TagGroup { CATEGORY, STYLE, COLOR, FIT_MATERIAL }

/** Qué talla del usuario aplica a una categoría de prenda. */
enum class SizeKind { TOP, WAIST, SHOE }

data class Tag(
    val id: String,
    val group: TagGroup,
    val label: String,
    /** Palabras o frases (sin tildes, minúsculas) que identifican la etiqueta en un título. */
    val synonyms: List<String>,
    val sizeKind: SizeKind? = null,
) {
    /** Palabra que se usa al construir búsquedas ("cuero / piel" → "cuero"). */
    val searchTerm: String get() = label.substringBefore(" / ")
}
