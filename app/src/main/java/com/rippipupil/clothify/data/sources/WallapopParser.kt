package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Geo
import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.SizeExtractor
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.TitleTagger
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

object WallapopParser {
    /** Id de la categoría "Hombre" dentro de Moda y accesorios. */
    const val MEN_TAXONOMY_ID = 11003

    fun parse(json: String, query: SearchQuery, nowMillis: Long): List<Product>? {
        val root = parseJsonOrNull(json) ?: return null
        val items = root.obj("data").obj("section").obj("payload").arr("items") ?: return null
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            if (item.obj("reserved").bool("flag") == true) return@mapNotNull null
            if (!isForMen(item)) return@mapNotNull null
            val id = item.str("id") ?: return@mapNotNull null
            val title = item.str("title") ?: return@mapNotNull null
            val slug = item.str("web_slug") ?: return@mapNotNull null
            val price = item.obj("price").num("amount") ?: return@mapNotNull null
            val image = item.arr("images")?.firstOrNull().obj("urls").let { it.str("medium") ?: it.str("small") }
                ?: return@mapNotNull null
            if (price > query.maxPriceEur) return@mapNotNull null
            val size = SizeExtractor.fromText("$title ${item.str("description").orEmpty()}")
            if (!query.acceptsSize(size)) return@mapNotNull null
            val location = item.obj("location").let { loc ->
                val lat = loc.num("latitude")
                val lon = loc.num("longitude")
                if (lat != null && lon != null) LatLng(lat, lon) else null
            }
            Product(
                id = "wp:$id",
                source = Source.WALLAPOP,
                title = title,
                imageUrl = image,
                priceEur = price,
                sellerName = Source.WALLAPOP.label,
                productUrl = "https://es.wallapop.com/item/$slug",
                size = size,
                distanceKm = if (query.location != null && location != null) Geo.distanceKm(query.location, location) else null,
                tagIds = query.tagIds + TitleTagger.tag(title),
                fetchedAtMillis = nowMillis,
            )
        }
    }

    /** Si la respuesta trae taxonomía, exigimos que incluya "Hombre". */
    private fun isForMen(item: JsonObject): Boolean {
        val taxonomy = item.arr("taxonomy") ?: return true
        return taxonomy.any { runCatching { it.jsonObject.num("id")?.toInt() == MEN_TAXONOMY_ID }.getOrDefault(false) }
    }
}
