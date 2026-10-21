package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source

/** Productos nuevos de AliExpress mediante la API oficial de afiliados. */
class AliExpressSource(
    private val api: AliExpressClient,
    private val throttle: Throttle,
    private val now: () -> Long = System::currentTimeMillis,
) : ProductSource {
    override val source = Source.ALIEXPRESS

    override suspend fun search(query: SearchQuery): SourceResult {
        if (!api.configured) return SourceResult.Failed(SourceFailure.DISABLED)
        return safeFetch {
            throttle.await()
            val response = api.call(
                "aliexpress.affiliate.product.query",
                mapOf(
                    "keywords" to "${query.text} hombre",
                    "target_currency" to "EUR",
                    "target_language" to "ES",
                    "ship_to_country" to "ES",
                    "page_size" to "40",
                    "max_sale_price" to (query.maxPriceEur * 100).toLong().toString(),
                ),
            ) ?: return@safeFetch SourceResult.Failed(SourceFailure.DISABLED)
            val error = AliExpressParser.errorCode(response.body)
            when {
                error in AliExpressClient.CREDENTIAL_ERRORS -> SourceResult.Failed(SourceFailure.DISABLED)
                error != null || response.code != 200 -> SourceResult.Failed(SourceFailure.BAD_RESPONSE)
                else -> AliExpressParser.parseQuery(response.body, query, now())
                    ?.let { SourceResult.Ok(it) }
                    ?: SourceResult.Failed(SourceFailure.BAD_RESPONSE)
            }
        }
    }
}
