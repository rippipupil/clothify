package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.SizeExtractor
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.TitleTagger
import kotlinx.serialization.json.JsonObject

object VintedParser {
    fun parse(json: String, query: SearchQuery, nowMillis: Long): List<Product>? {
        val root = parseJsonOrNull(json) ?: return null
        val items = root.arr("items") ?: return null
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.str("id") ?: return@mapNotNull null
            val title = item.str("title") ?: return@mapNotNull null
            val url = item.str("url") ?: return@mapNotNull null
            val image = item.obj("photo").str("url") ?: return@mapNotNull null
            // El precio llega como objeto {amount, currency_code} o, en versiones antiguas, como texto.
            val price = item.obj("price").num("amount") ?: item.num("price") ?: return@mapNotNull null
            if (price > query.maxPriceEur) return@mapNotNull null
            val size = item.str("size_title")?.let(SizeExtractor::fromText)
            if (!query.acceptsSize(size)) return@mapNotNull null
            val brand = item.str("brand_title").orEmpty()
            Product(
                id = "vt:$id",
                source = Source.VINTED,
                title = title,
                imageUrl = image,
                priceEur = price,
                sellerName = Source.VINTED.label,
                productUrl = url,
                size = size,
                distanceKm = null,
                tagIds = query.tagIds + TitleTagger.tag("$title $brand"),
                fetchedAtMillis = nowMillis,
            )
        }
    }
}
