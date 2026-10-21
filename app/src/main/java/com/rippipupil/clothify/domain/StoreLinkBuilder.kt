package com.rippipupil.clothify.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder

enum class Store(val label: String) {
    AMAZON("Amazon"),
    ALIEXPRESS("AliExpress"),
    SHEIN("Shein"),
    VINTED("Vinted"),
    WALLAPOP("Wallapop"),
}

/** Enlaces de búsqueda "buscar parecidos" en cada tienda (España, sección hombre). */
object StoreLinkBuilder {
    private const val SECTION = "hombre"
    private const val MAX_EXTRAS = 3

    /** Orden natural en español: "chaqueta denim oversize azul claro". */
    private val groupOrder = listOf(TagGroup.STYLE, TagGroup.FIT_MATERIAL, TagGroup.COLOR)

    fun queryText(tagIds: Set<String>, withSection: Boolean = true): String {
        val tags = TagVocabulary.all.filter { it.id in tagIds }
        val category = tags.firstOrNull { it.group == TagGroup.CATEGORY }
        val extras = tags.filter { it.group != TagGroup.CATEGORY }
            .sortedBy { groupOrder.indexOf(it.group) }
            .take(MAX_EXTRAS)
        val words = listOfNotNull(category) + extras
        return (words.map { it.searchTerm } + if (withSection) listOf(SECTION) else emptyList()).joinToString(" ")
    }

    fun links(tagIds: Set<String>, location: LatLng?): List<Pair<Store, String>> {
        val text = queryText(tagIds)
        val plus = URLEncoder.encode(text, "UTF-8")
        val words = text.split(" ").map { URLEncoder.encode(it, "UTF-8") }
        val wallapopLocation = location?.let { "&latitude=${coord(it.lat)}&longitude=${coord(it.lon)}" }.orEmpty()
        return listOf(
            Store.AMAZON to "https://www.amazon.es/s?k=$plus",
            Store.ALIEXPRESS to "https://es.aliexpress.com/w/wholesale-${words.joinToString("-")}.html",
            Store.SHEIN to "https://es.shein.com/pdsearch/${words.joinToString("%20")}/",
            Store.VINTED to "https://www.vinted.es/catalog?search_text=$plus",
            Store.WALLAPOP to "https://es.wallapop.com/app/search?keywords=$plus$wallapopLocation",
        )
    }

    private fun coord(value: Double): String =
        BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
