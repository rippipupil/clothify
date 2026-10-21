package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Source
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleShoppingSourceTest {
    private val server = MockWebServer()
    private val client = OkHttpClient()

    @After
    fun tearDown() = server.shutdown()

    private fun gate(allow: Boolean) = object : BudgetGate {
        var consumed = 0
        override suspend fun tryConsume(): Boolean = allow.also { if (it) consumed++ }
    }

    @Test
    fun parsesValidResultsAndDropsInvalidOnes() {
        val products = GoogleShoppingParser.parse(fixture("serpapi_shopping.json"), query(), nowMillis = 5)!!
        assertEquals(listOf("gs:111", "gs:222"), products.map { it.id })
        val first = products.first()
        assertEquals(Source.GOOGLE_SHOPPING, first.source)
        assertEquals("Zalando", first.sellerName)
        assertEquals(29.99, first.priceEur, 1e-9)
        assertEquals("https://www.google.com/shopping/product/111", first.productUrl)
        assertEquals(5L, first.fetchedAtMillis)
        assertTrue(first.tagIds.containsAll(listOf("sudadera", "oversize", "negro", "algodon")))
    }

    @Test
    fun prefersDirectLinkOverGoogleProductPage() {
        val products = GoogleShoppingParser.parse(fixture("serpapi_shopping.json"), query(), 0)!!
        assertEquals("https://www.amazon.es/dp/B0TEST", products.first { it.id == "gs:222" }.productUrl)
    }

    @Test
    fun missingLinkFallsBackToGoogleShoppingSearch() {
        val json = """{"shopping_results":[{"title":"Polo azul","extracted_price":15.0,"source":"Zara","thumbnail":"https://t/1"}]}"""
        val product = GoogleShoppingParser.parse(json, query(), 0)!!.single()
        assertEquals("https://www.google.com/search?tbm=shop&q=Polo+azul+Zara", product.productUrl)
    }

    @Test
    fun garbageJsonGivesNull() = assertNull(GoogleShoppingParser.parse("<html>", query(), 0))

    @Test
    fun missingResultsGiveEmptyList() = assertEquals(emptyList<Any>(), GoogleShoppingParser.parse("{}", query(), 0))

    @Test
    fun emptyKeyIsDisabledWithoutRequest() = runTest {
        val result = GoogleShoppingSource(client, apiKey = "", budget = gate(true), baseUrl = server.url("/").toString()).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.DISABLED), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun exhaustedBudgetSkipsRequest() = runTest {
        val result = GoogleShoppingSource(client, "key", gate(false), server.url("/").toString()).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.BUDGET_EXHAUSTED), result)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun successfulSearchBuildsTheRightRequest() = runTest {
        server.enqueue(MockResponse().setBody(fixture("serpapi_shopping.json")))
        val budget = gate(true)
        val result = GoogleShoppingSource(client, "key", budget, server.url("/").toString()).search(query())
        assertEquals(2, (result as SourceResult.Ok).products.size)
        assertEquals(1, budget.consumed)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/search.json", url.encodedPath)
        assertEquals("google_shopping", url.queryParameter("engine"))
        assertEquals("sudadera oversize hombre", url.queryParameter("q"))
        assertEquals("es", url.queryParameter("gl"))
        assertEquals("es", url.queryParameter("hl"))
        assertEquals("key", url.queryParameter("api_key"))
    }

    @Test
    fun outOfSearchesMapsToBudgetExhausted() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("""{"error":"Your account has run out of searches."}"""))
        val result = GoogleShoppingSource(client, "key", gate(true), server.url("/").toString()).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.BUDGET_EXHAUSTED), result)
    }

    @Test
    fun serverErrorIsBadResponse() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("oops"))
        val result = GoogleShoppingSource(client, "key", gate(true), server.url("/").toString()).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.BAD_RESPONSE), result)
    }

    @Test
    fun unreachableServerIsNoInternet() = runTest {
        val url = server.url("/").toString()
        server.shutdown()
        val result = GoogleShoppingSource(client, "key", gate(true), url).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.NO_INTERNET), result)
    }
}
