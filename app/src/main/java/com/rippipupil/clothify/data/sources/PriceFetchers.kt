package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.data.pool.PriceFetcher
import com.rippipupil.clothify.data.pool.PriceLookup
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.Source
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object WallapopItemParser {
    /** Precio de la ficha de un artículo (`price.cash.amount`). */
    fun price(json: String): Double? = parseJsonOrNull(json).obj("price").let { it.obj("cash").num("amount") ?: it.num("amount") }
}

/** Precio actual de un artículo de Wallapop; 404 = vendido o retirado. */
class WallapopPriceFetcher(
    private val client: OkHttpClient,
    private val throttle: Throttle,
    private val baseUrl: String = "https://api.wallapop.com/",
) : PriceFetcher {
    override fun supports(source: Source) = source == Source.WALLAPOP

    override suspend fun lookup(product: Product): PriceLookup {
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/v3/items")
            .addPathSegment(product.id.removePrefix("wp:"))
            .build()
        val request = Request.Builder().url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("X-DeviceOS", "0")
            .build()
        return try {
            throttle.await()
            val response = client.fetch(request)
            when (response.code) {
                200 -> WallapopItemParser.price(response.body)?.let { PriceLookup.Price(it) } ?: PriceLookup.Unknown
                404, 410 -> PriceLookup.Gone
                else -> PriceLookup.Unknown
            }
        } catch (e: IOException) {
            PriceLookup.Unknown
        }
    }
}

/** Precio actual de un producto de AliExpress con la API de afiliados. */
class AliExpressPriceFetcher(private val api: AliExpressClient) : PriceFetcher {
    override fun supports(source: Source) = source == Source.ALIEXPRESS && api.configured

    override suspend fun lookup(product: Product): PriceLookup {
        val id = product.id.removePrefix("ae:")
        return try {
            val response = api.call(
                "aliexpress.affiliate.productdetail.get",
                mapOf("product_ids" to id, "target_currency" to "EUR", "target_language" to "ES", "country" to "ES"),
            ) ?: return PriceLookup.Unknown
            if (AliExpressParser.errorCode(response.body) != null) return PriceLookup.Unknown
            val prices = AliExpressParser.parseDetailPrices(response.body) ?: return PriceLookup.Unknown
            prices[id]?.let { PriceLookup.Price(it) } ?: PriceLookup.Gone
        } catch (e: IOException) {
            PriceLookup.Unknown
        }
    }
}
