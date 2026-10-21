package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Source
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AliExpressSourceTest {
    private val server = MockWebServer()

    @After
    fun tearDown() = server.shutdown()

    private fun api(key: String = "12345", secret: String = "secret") =
        AliExpressClient(OkHttpClient(), key, secret, "clothify", server.url("/").toString()) { 1_700_000_000_000L }

    @Test
    fun signsSortedParamsWithHmacSha256() = assertEquals(
        "6F8118494058AE0E760EB2EF2ADC8AF060FAE0EC88BFE7F6500BFB7870882929",
        AliExpressSigner.sign(mapOf("timestamp" to "1", "method" to "m", "sign_method" to "sha256", "app_key" to "12345"), "secret"),
    )

    @Test
    fun parsesProductsDroppingExpensiveAndImageless() {
        val products = AliExpressParser.parseQuery(fixture("aliexpress_query.json"), query(), 9)!!
        assertEquals(listOf("ae:1005006000000001"), products.map { it.id })
        val p = products.single()
        assertEquals(Source.ALIEXPRESS, p.source)
        assertEquals(18.45, p.priceEur, 1e-9)
        assertEquals("https://s.click.aliexpress.com/e/_abc", p.productUrl)
        assertEquals("AliExpress", p.sellerName)
        assertTrue(p.tagIds.containsAll(listOf("sudadera", "oversize", "negro")))
    }

    @Test
    fun fallsBackToDetailUrlWithoutPromotionLink() {
        val products = AliExpressParser.parseQuery(fixture("aliexpress_query.json"), query(maxPrice = 100.0), 0)!!
        assertEquals("https://www.aliexpress.com/item/1005006000000002.html", products.first { it.id == "ae:1005006000000002" }.productUrl)
    }

    @Test
    fun emptyResultIsEmptyList() = assertEquals(
        emptyList<Any>(),
        AliExpressParser.parseQuery("""{"aliexpress_affiliate_product_query_response":{"resp_result":{"resp_code":405,"resp_msg":"No results"}}}""", query(), 0),
    )

    @Test
    fun garbageIsNull() = assertNull(AliExpressParser.parseQuery("<html>", query(), 0))

    @Test
    fun parsesDetailPrices() =
        assertEquals(mapOf("1005006000000001" to 15.10), AliExpressParser.parseDetailPrices(fixture("aliexpress_detail.json")))

    @Test
    fun missingCredentialsAreDisabledWithoutRequest() = runTest {
        val result = AliExpressSource(api(key = ""), Throttle(0)).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.DISABLED), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun buildsSignedRequest() = runTest {
        server.enqueue(MockResponse().setBody(fixture("aliexpress_query.json")))
        val result = AliExpressSource(api(), Throttle(0)).search(query())
        assertEquals(1, (result as SourceResult.Ok).products.size)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/sync", url.encodedPath)
        assertEquals("aliexpress.affiliate.product.query", url.queryParameter("method"))
        assertEquals("sudadera oversize hombre", url.queryParameter("keywords"))
        assertEquals("6000", url.queryParameter("max_sale_price"))
        assertEquals("EUR", url.queryParameter("target_currency"))
        assertEquals("ES", url.queryParameter("ship_to_country"))
        assertEquals("clothify", url.queryParameter("tracking_id"))
        assertEquals("1700000000000", url.queryParameter("timestamp"))
        val params = url.queryParameterNames.filter { it != "sign" }.associateWith { url.queryParameter(it)!! }
        assertEquals(AliExpressSigner.sign(params, "secret"), url.queryParameter("sign"))
    }

    @Test
    fun invalidKeyIsDisabled() = runTest {
        server.enqueue(MockResponse().setBody("""{"error_response":{"code":"InvalidApiKey","msg":"Invalid app Key"}}"""))
        assertEquals(SourceResult.Failed(SourceFailure.DISABLED), AliExpressSource(api(), Throttle(0)).search(query()))
    }

    @Test
    fun otherErrorsAreBadResponse() = runTest {
        server.enqueue(MockResponse().setBody("""{"error_response":{"code":"ApiCallLimit","msg":"limit"}}"""))
        assertEquals(SourceResult.Failed(SourceFailure.BAD_RESPONSE), AliExpressSource(api(), Throttle(0)).search(query()))
    }

    @Test
    fun clientReturnsNullWithoutCredentials() = runTest {
        assertNull(api(secret = "").call("m", emptyMap()))
        assertNotNull(api())
    }
}
