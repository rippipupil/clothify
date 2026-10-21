package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Segunda mano de Wallapop mediante su API interna de búsqueda (no oficial). */
class WallapopSource(
    private val client: OkHttpClient,
    private val throttle: Throttle,
    private val baseUrl: String = "https://api.wallapop.com/",
    private val now: () -> Long = System::currentTimeMillis,
) : ProductSource {
    override val source = Source.WALLAPOP

    override suspend fun search(query: SearchQuery): SourceResult {
        val center = query.location ?: MADRID
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/v3/search")
            .addQueryParameter("keywords", query.text)
            .addQueryParameter("latitude", center.lat.toString())
            .addQueryParameter("longitude", center.lon.toString())
            .addQueryParameter("source", "search_box")
            .addQueryParameter("category_id", FASHION_CATEGORY_ID)
            .addQueryParameter("subcategory_ids", WallapopParser.MEN_TAXONOMY_ID.toString())
            .addQueryParameter("max_sale_price", query.maxPriceEur.asQueryNumber())
            .addQueryParameter("order_by", if (query.location != null) "closest" else "most_relevance")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("X-DeviceOS", "0")
            .header("Accept", "application/json")
            .header("Accept-Language", "es-ES,es;q=0.9")
            .build()
        return safeFetch {
            throttle.await()
            val response = client.fetch(request)
            when (response.code) {
                200 -> WallapopParser.parse(response.body, query, now())
                    ?.let { SourceResult.Ok(it) }
                    ?: SourceResult.Failed(SourceFailure.BAD_RESPONSE)
                401, 403, 429 -> SourceResult.Failed(SourceFailure.BLOCKED)
                else -> SourceResult.Failed(SourceFailure.BAD_RESPONSE)
            }
        }
    }

    private companion object {
        const val FASHION_CATEGORY_ID = "12465"
        val MADRID = LatLng(40.4168, -3.7038)
    }
}
