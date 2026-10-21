package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.TitleTagger
import java.net.URLEncoder

object GoogleShoppingParser {
    /** null si la respuesta no es JSON; las prendas incompletas o caras se descartan. */
    fun parse(json: String, query: SearchQuery, nowMillis: Long): List<Product>? {
        val root = parseJsonOrNull(json) ?: return null
        val results = root.arr("shopping_results") ?: return emptyList()
        return results.mapNotNull { item ->
            val title = item.str("title") ?: return@mapNotNull null
            val price = item.num("extracted_price") ?: return@mapNotNull null
            val image = item.str("thumbnail") ?: return@mapNotNull null
            val seller = item.str("source")
            val url = item.str("link") ?: item.str("product_link") ?: shoppingSearchUrl(title, seller)
            if (price > query.maxPriceEur) return@mapNotNull null
            Product(
                id = "gs:" + (item.str("product_id") ?: url.hashCode().toString()),
                source = Source.GOOGLE_SHOPPING,
                title = title,
                imageUrl = image,
                priceEur = price,
                sellerName = seller ?: Source.GOOGLE_SHOPPING.label,
                productUrl = url,
                size = null,
                distanceKm = null,
                tagIds = query.tagIds + TitleTagger.tag(title),
                fetchedAtMillis = nowMillis,
            )
        }
    }

    /** Si la respuesta no trae enlace, busca esa prenda en Google Shopping. */
    private fun shoppingSearchUrl(title: String, seller: String?): String =
        "https://www.google.com/search?tbm=shop&q=" + URLEncoder.encode(listOfNotNull(title, seller).joinToString(" "), "UTF-8")
}
