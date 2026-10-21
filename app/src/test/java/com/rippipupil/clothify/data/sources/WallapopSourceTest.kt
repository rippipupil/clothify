package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.LatLng
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.UserSizes
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

class WallapopSourceTest {
    private val server = MockWebServer()
    private val madrid = LatLng(40.4168, -3.7038)

    @After
    fun tearDown() = server.shutdown()

    private fun parse(q: com.rippipupil.clothify.domain.SearchQuery = query()) =
        WallapopParser.parse(fixture("wallapop_search.json"), q, nowMillis = 7)!!

    @Test
    fun parsesMenItemsSkippingReservedWomenAndImageless() {
        val products = parse()
        assertEquals(3, products.size)
        assertTrue(products.none { it.id in setOf("wp:reserved1", "wp:women1", "wp:noimg1") })
        val first = products.first()
        assertTrue(first.id.startsWith("wp:"))
        assertEquals(Source.WALLAPOP, first.source)
        assertEquals("Wallapop", first.sellerName)
        assertEquals(27.0, first.priceEur, 1e-9)
        assertEquals("https://es.wallapop.com/item/sudadera-nike-sin-capucha-marron-1297609802", first.productUrl)
        assertTrue(first.imageUrl.startsWith("https://cdn.wallapop.com/"))
        assertTrue(first.tagIds.containsAll(listOf("sudadera", "oversize", "marron")))
        assertEquals(7L, first.fetchedAtMillis)
    }

    @Test
    fun extractsSizeFromTitle() =
        assertEquals("M", parse().first { it.title.contains("GAP") }.size)

    @Test
    fun dropsItemsWithAnotherSize() {
        val products = parse(query(sizes = UserSizes(top = "L")))
        assertTrue(products.none { it.title.contains("GAP") })
        assertEquals(2, products.size)
    }

    @Test
    fun keepsItemsWithMatchingSize() =
        assertTrue(parse(query(sizes = UserSizes(top = "M"))).any { it.title.contains("GAP") })

    @Test
    fun distanceOnlyWithUserLocation() {
        assertNull(parse().first().distanceKm)
        assertNotNull(parse(query(location = madrid)).first().distanceKm)
        assertTrue(parse(query(location = madrid)).first().distanceKm!! < 20)
    }

    @Test
    fun dropsItemsAboveMaxPrice() =
        assertEquals(listOf(5.0), parse(query(maxPrice = 10.0)).map { it.priceEur })

    @Test
    fun invalidJsonGivesNull() = assertNull(WallapopParser.parse("{\"meta\":{}}", query(), 0))

    @Test
    fun buildsRequestWithFiltersAndHeaders() = runTest {
        server.enqueue(MockResponse().setBody(fixture("wallapop_search.json")))
        val source = WallapopSource(OkHttpClient(), Throttle(0), server.url("/").toString())
        val result = source.search(query(location = LatLng(41.39, 2.17), maxPrice = 45.0))
        assertEquals(3, (result as SourceResult.Ok).products.size)
        val request = server.takeRequest()
        val url = request.requestUrl!!
        assertEquals("/api/v3/search", url.encodedPath)
        assertEquals("sudadera oversize", url.queryParameter("keywords"))
        assertEquals("41.39", url.queryParameter("latitude"))
        assertEquals("2.17", url.queryParameter("longitude"))
        assertEquals("12465", url.queryParameter("category_id"))
        assertEquals("11003", url.queryParameter("subcategory_ids"))
        assertEquals("45", url.queryParameter("max_sale_price"))
        assertEquals("closest", url.queryParameter("order_by"))
        assertEquals("0", request.getHeader("X-DeviceOS"))
    }

    @Test
    fun withoutLocationUsesMadridAndRelevance() = runTest {
        server.enqueue(MockResponse().setBody(fixture("wallapop_search.json")))
        WallapopSource(OkHttpClient(), Throttle(0), server.url("/").toString()).search(query())
        val url = server.takeRequest().requestUrl!!
        assertEquals("40.4168", url.queryParameter("latitude"))
        assertEquals("most_relevance", url.queryParameter("order_by"))
    }

    @Test
    fun forbiddenIsBlocked() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))
        val result = WallapopSource(OkHttpClient(), Throttle(0), server.url("/").toString()).search(query())
        assertEquals(SourceResult.Failed(SourceFailure.BLOCKED), result)
    }
}
