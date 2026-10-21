package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Da las cookies de una sesión anónima de vinted.es (null si no se consiguen). */
interface CookieProvider {
    suspend fun cookies(forceRefresh: Boolean): String?
}

/** Segunda mano de Vinted mediante su API interna del catálogo (no oficial). */
class VintedSource(
    private val client: OkHttpClient,
    private val cookies: CookieProvider,
    private val throttle: Throttle,
    private val baseUrl: String = "https://www.vinted.es/",
    private val now: () -> Long = System::currentTimeMillis,
) : ProductSource {
    override val source = Source.VINTED
    private var blockedUntil = 0L

    override suspend fun search(query: SearchQuery): SourceResult {
        if (now() < blockedUntil) return SourceResult.Failed(SourceFailure.BLOCKED)
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/v2/catalog/items")
            .addQueryParameter("search_text", query.text)
            .addQueryParameter("catalog_ids", MEN_CATALOG_ID)
            .addQueryParameter("price_to", query.maxPriceEur.asQueryNumber())
            .addQueryParameter("currency", "EUR")
            .addQueryParameter("order", "relevance")
            .addQueryParameter("per_page", "40")
            .build()
        return safeFetch {
            var response = request(url.toString(), cookies.cookies(forceRefresh = false))
                ?: return@safeFetch blocked()
            if (response.code == 401 || response.code == 403) {
                response = request(url.toString(), cookies.cookies(forceRefresh = true))
                    ?: return@safeFetch blocked()
            }
            when (response.code) {
                200 -> VintedParser.parse(response.body, query, now())
                    ?.let { SourceResult.Ok(it) }
                    ?: SourceResult.Failed(SourceFailure.BAD_RESPONSE)
                401, 403, 429 -> blocked()
                else -> SourceResult.Failed(SourceFailure.BAD_RESPONSE)
            }
        }
    }

    /** Deja Vinted en pausa una hora para no repetir peticiones (ni el WebView) que van a fallar. */
    private fun blocked(): SourceResult {
        blockedUntil = now() + BLOCK_MILLIS
        return SourceResult.Failed(SourceFailure.BLOCKED)
    }

    private suspend fun request(url: String, cookie: String?): HttpResponse? {
        if (cookie.isNullOrBlank()) return null
        throttle.await()
        return client.fetch(
            Request.Builder()
                .url(url)
                .header("Cookie", cookie)
                .header("User-Agent", MOBILE_USER_AGENT)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .build(),
        )
    }

    private companion object {
        const val MEN_CATALOG_ID = "5"
        const val BLOCK_MILLIS = 60 * 60 * 1000L
    }
}
