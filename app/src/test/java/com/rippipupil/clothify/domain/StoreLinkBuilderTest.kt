package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreLinkBuilderTest {
    private val tags = setOf("azul_claro", "oversize", "chaqueta", "denim")
    private val q = "chaqueta+oversize+denim+azul+claro+hombre"

    private fun link(store: Store, location: LatLng? = null) =
        StoreLinkBuilder.links(tags, location).first { it.first == store }.second

    @Test
    fun queryTextPutsCategoryFirstAndAddsSection() =
        assertEquals("chaqueta oversize denim azul claro hombre", StoreLinkBuilder.queryText(tags))

    @Test
    fun queryTextWithoutSection() =
        assertEquals("chaqueta oversize denim azul claro", StoreLinkBuilder.queryText(tags, withSection = false))

    @Test
    fun queryTextKeepsAtMostThreeExtras() {
        val text = StoreLinkBuilder.queryText(setOf("sudadera", "streetwear", "negro", "oversize", "algodon"), withSection = false)
        assertEquals(4, text.split(" ").size)
        assertEquals("sudadera", text.split(" ").first())
    }

    @Test
    fun amazon() = assertEquals("https://www.amazon.es/s?k=$q", link(Store.AMAZON))

    @Test
    fun aliexpress() = assertEquals(
        "https://es.aliexpress.com/w/wholesale-chaqueta-oversize-denim-azul-claro-hombre.html",
        link(Store.ALIEXPRESS),
    )

    @Test
    fun shein() = assertEquals(
        "https://es.shein.com/pdsearch/chaqueta%20oversize%20denim%20azul%20claro%20hombre/",
        link(Store.SHEIN),
    )

    @Test
    fun vinted() = assertEquals("https://www.vinted.es/catalog?search_text=$q", link(Store.VINTED))

    @Test
    fun wallapopWithoutLocation() =
        assertEquals("https://es.wallapop.com/app/search?keywords=$q", link(Store.WALLAPOP))

    @Test
    fun wallapopWithLocation() = assertEquals(
        "https://es.wallapop.com/app/search?keywords=$q&latitude=40.42&longitude=-3.7",
        link(Store.WALLAPOP, LatLng(40.4168, -3.7038)),
    )

    @Test
    fun encodesNonAsciiCharacters() {
        val links = StoreLinkBuilder.links(setOf("banador"), null).toMap()
        assertEquals("https://www.amazon.es/s?k=ba%C3%B1ador+hombre", links[Store.AMAZON])
        assertEquals("https://es.aliexpress.com/w/wholesale-ba%C3%B1ador-hombre.html", links[Store.ALIEXPRESS])
    }

    @Test
    fun allFiveStores() = assertEquals(Store.entries.toList(), StoreLinkBuilder.links(tags, null).map { it.first })
}
