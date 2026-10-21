package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Prendas nuevas de muchas tiendas a través de SerpApi (motor google_shopping). */
class GoogleShoppingSource(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val budget: BudgetGate,
    private val baseUrl: String = "https://serpapi.com/",
    private val now: () -> Long = System::currentTimeMillis,
) : ProductSource {
    override val source = Source.GOOGLE_SHOPPING

    override suspend fun search(query: SearchQuery): SourceResult {
        if (apiKey.isBlank()) return SourceResult.Failed(SourceFailure.DISABLED)
        if (!budget.tryConsume()) return SourceResult.Failed(SourceFailure.BUDGET_EXHAUSTED)
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegment("search.json")
            .addQueryParameter("engine", "google_shopping")
            .addQueryParameter("q", "${query.text} hombre")
            .addQueryParameter("gl", "es")
            .addQueryParameter("hl", "es")
            .addQueryParameter("location", "Spain")
            .addQueryParameter("api_key", apiKey)
            .build()
        return safeFetch {
            val response = client.fetch(Request.Builder().url(url).build())
            when {
                response.code == 429 || response.body.contains("run out of searches") ->
                    SourceResult.Failed(SourceFailure.BUDGET_EXHAUSTED)
                response.code == 401 -> SourceResult.Failed(SourceFailure.DISABLED)
                response.code != 200 -> SourceResult.Failed(SourceFailure.BAD_RESPONSE)
                else -> GoogleShoppingParser.parse(response.body, query, now())
                    ?.let { SourceResult.Ok(it) }
                    ?: SourceResult.Failed(SourceFailure.BAD_RESPONSE)
            }
        }
    }
}
