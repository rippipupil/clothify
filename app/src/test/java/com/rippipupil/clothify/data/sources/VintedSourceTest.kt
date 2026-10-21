package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.UserSizes
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VintedSourceTest {
    private val server = MockWebServer()
    private var clock = 1_000_000L

    @After
    fun tearDown() = server.shutdown()

    private class FakeCookies : CookieProvider {
        val calls = mutableListOf<Boolean>()
        override suspend fun cookies(forceRefresh: Boolean): String? {
            calls += forceRefresh
            return if (forceRefresh) "access_token_web=new" else "access_token_web=old"
        }
    }

    private fun source(cookies: CookieProvider = FakeCookies()) =
        VintedSource(OkHttpClient(), cookies, Throttle(0), server.url("/").toString()) { clock }

    @Test
    fun parsesObjectAndStringPricesSkippingItemsWithoutPhoto() {
        val products = VintedParser.parse(fixture("vinted_search.json"), query(), 3)!!
        assertEquals(listOf("vt:5012345678", "vt:5012345679"), products.map { it.id })
        val first = products.first()
        assertEquals(Source.VINTED, first.source)
        assertEquals(25.0, first.priceEur, 1e-9)
        assertEquals(12.0, products[1].priceEur, 1e-9)
        assertEquals("L", first.size)
        assertEquals("https://www.vinted.es/items/5012345678-sudadera-carhartt-oversize", first.productUrl)
        assertEquals("https://images1.vinted.net/t/01_abc/f800/5012345678.jpeg", first.imageUrl)
        assertTrue(first.tagIds.containsAll(listOf("sudadera", "oversize", "workwear")))
        assertNull(first.distanceKm)
    }

    @Test
    fun filtersBySize() {
        val products = VintedParser.parse(fixture("vinted_search.json"), query(sizes = UserSizes(top = "M")), 0)!!
        assertEquals(listOf("vt:5012345679"), products.map { it.id })
    }

    @Test
    fun invalidJsonGivesNull() = assertNull(VintedParser.parse("not json", query(), 0))

    @Test
    fun sendsCookieAndFilters() = runTest {
        server.enqueue(MockResponse().setBody(fixture("vinted_search.json")))
        val result = source().search(query(maxPrice = 40.0))
        assertEquals(2, (result as SourceResult.Ok).products.size)
        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("/api/v2/catalog/items", url.encodedPath)
        assertEquals("sudadera oversize", url.queryParameter("search_text"))
        assertEquals("5", url.queryParameter("catalog_ids"))
        assertEquals("40", url.queryParameter("price_to"))
        assertEquals("access_token_web=old", request.getHeader("Cookie"))
    }

    @Test
    fun refreshesCookieOnceAfterForbidden() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setBody(fixture("vinted_search.json")))
        val cookies = FakeCookies()
        val result = source(cookies).search(query())
        assertTrue(result is SourceResult.Ok)
        assertEquals(listOf(false, true), cookies.calls)
        server.takeRequest()
        assertEquals("access_token_web=new", server.takeRequest().getHeader("Cookie"))
    }

    @Test
    fun blockedTwiceDisablesForAnHour() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setResponseCode(403))
        val vinted = source()
        assertEquals(SourceResult.Failed(SourceFailure.BLOCKED), vinted.search(query()))
        clock += 30 * 60_000L
        assertEquals(SourceResult.Failed(SourceFailure.BLOCKED), vinted.search(query()))
        assertEquals(2, server.requestCount)
        clock += 31 * 60_000L
        server.enqueue(MockResponse().setBody(fixture("vinted_search.json")))
        assertTrue(vinted.search(query()) is SourceResult.Ok)
    }

    @Test
    fun missingCookieIsBlocked() = runTest {
        val noCookies = object : CookieProvider {
            override suspend fun cookies(forceRefresh: Boolean): String? = null
        }
        assertEquals(SourceResult.Failed(SourceFailure.BLOCKED), source(noCookies).search(query()))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun missingCookieDisablesForAnHourWithoutRetryingTheWebView() = runTest {
        var attempts = 0
        val noCookies = object : CookieProvider {
            override suspend fun cookies(forceRefresh: Boolean): String? = null.also { attempts++ }
        }
        val vinted = source(noCookies)
        vinted.search(query())
        val afterFirst = attempts
        vinted.search(query())
        assertEquals(afterFirst, attempts)
    }
}
