package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.TitleTagger
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object AliExpressParser {
    fun parseQuery(json: String, query: SearchQuery, nowMillis: Long): List<Product>? {
        val products = products(json, "aliexpress_affiliate_product_query_response") ?: return null
        return products.mapNotNull { item ->
            val id = item.str("product_id") ?: return@mapNotNull null
            val title = item.str("product_title") ?: return@mapNotNull null
            val price = item.num("target_sale_price") ?: item.num("sale_price") ?: return@mapNotNull null
            val image = item.str("product_main_image_url") ?: return@mapNotNull null
            val url = item.str("promotion_link") ?: item.str("product_detail_url") ?: return@mapNotNull null
            if (price > query.maxPriceEur) return@mapNotNull null
            Product(
                id = "ae:$id",
                source = Source.ALIEXPRESS,
                title = title,
                imageUrl = image,
                priceEur = price,
                sellerName = Source.ALIEXPRESS.label,
                productUrl = url,
                size = null,
                distanceKm = null,
                tagIds = query.tagIds + TitleTagger.tag(title),
                fetchedAtMillis = nowMillis,
            )
        }
    }

    /** product_id → precio actual en euros. */
    fun parseDetailPrices(json: String): Map<String, Double>? =
        products(json, "aliexpress_affiliate_productdetail_get_response")?.mapNotNull { item ->
            val id = item.str("product_id") ?: return@mapNotNull null
            val price = item.num("target_sale_price") ?: item.num("sale_price") ?: return@mapNotNull null
            id to price
        }?.toMap()

    /** Código de error de la plataforma, si la respuesta es un error. */
    fun errorCode(json: String): String? = parseJsonOrNull(json).obj("error_response").str("code")

    /** Lista de productos; vacía si la búsqueda no da resultados; null si la respuesta no se entiende. */
    private fun products(json: String, responseKey: String): List<JsonObject>? {
        val root = parseJsonOrNull(json) ?: return null
        val result = root.obj(responseKey).obj("resp_result") ?: return null
        val list: List<JsonElement> = result.obj("result").obj("products").arr("product") ?: return emptyList()
        return list.filterIsInstance<JsonObject>()
    }
}
