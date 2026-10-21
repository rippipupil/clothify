package com.rippipupil.clothify.ui

import com.rippipupil.clothify.data.pool.BasketItem
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.basket.BasketViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceLabelTest {
    private val checkable = { s: Source -> s == Source.WALLAPOP }

    private fun label(item: BasketItem) = BasketViewModel.priceLabel(item, checkable)

    @Test
    fun drop() = assertEquals("Antes 30 € · ahora 24 €", label(BasketItem(product("a", price = 30.0), 0, currentPriceEur = 24.0)))

    @Test
    fun rise() = assertEquals("Ha subido a 35 €", label(BasketItem(product("a", price = 30.0), 0, currentPriceEur = 35.0)))

    @Test
    fun unchanged() = assertNull(label(BasketItem(product("a", price = 30.0), 0, currentPriceEur = 30.0)))

    @Test
    fun gone() = assertEquals("Ya no está disponible", label(BasketItem(product("a"), 0, unavailable = true)))

    @Test
    fun notCheckable() =
        assertEquals("Sin aviso de precio", label(BasketItem(product("v", source = Source.VINTED), 0)))

    @Test
    fun displayedPriceUsesLatest() =
        assertEquals(24.0, BasketViewModel.displayedPrice(BasketItem(product("a", price = 30.0), 0, currentPriceEur = 24.0)), 1e-9)
}
