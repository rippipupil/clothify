package com.rippipupil.clothify.domain

enum class Source(val label: String) {
    WALLAPOP("Wallapop"),
    VINTED("Vinted"),
    GOOGLE_SHOPPING("Google Shopping"),
    ALIEXPRESS("AliExpress"),
}

data class Product(
    /** "{fuente}:{id en la fuente}", p. ej. "wp:w67vwekknw6x". */
    val id: String,
    val source: Source,
    val title: String,
    val imageUrl: String,
    val priceEur: Double,
    /** "Wallapop", "Vinted" o la tienda de Google Shopping. */
    val sellerName: String,
    /** Ficha real del producto. */
    val productUrl: String,
    val size: String?,
    val distanceKm: Double?,
    val tagIds: Set<String>,
    val fetchedAtMillis: Long,
)

data class LatLng(val lat: Double, val lon: Double)

data class UserSizes(val top: String? = null, val waist: String? = null, val shoe: String? = null) {
    fun forKind(kind: SizeKind?): String? = when (kind) {
        SizeKind.TOP -> top
        SizeKind.WAIST -> waist
        SizeKind.SHOE -> shoe
        null -> null
    }
}

data class SearchQuery(
    /** Texto en español sin la sección: "sudadera oversize negro". */
    val text: String,
    val tagIds: Set<String>,
    val categoryTag: String?,
    val maxPriceEur: Double,
    val sizes: UserSizes,
    val location: LatLng?,
) {
    /** Talla del usuario que aplica a la categoría buscada, si la hay. */
    val wantedSize: String? get() = sizes.forKind(categoryTag?.let { TagVocabulary.byId(it)?.sizeKind })
}
