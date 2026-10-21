package com.rippipupil.clothify.data.pool

import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.Source

sealed interface PriceLookup {
    data class Price(val eur: Double) : PriceLookup
    /** La prenda ya no existe (vendida o retirada). */
    data object Gone : PriceLookup
    /** No se ha podido saber (sin red, error…). */
    data object Unknown : PriceLookup
}

/** Consulta el precio actual de una prenda en su tienda. */
interface PriceFetcher {
    fun supports(source: Source): Boolean
    suspend fun lookup(product: Product): PriceLookup
}

sealed interface PriceAlert {
    val product: Product

    data class Dropped(override val product: Product, val fromEur: Double, val toEur: Double) : PriceAlert
    data class Gone(override val product: Product) : PriceAlert
}

/** Revisa la cesta y devuelve los avisos: bajadas de precio y prendas que ya no están. */
class PriceChecker(
    private val basket: BasketRepository,
    private val fetchers: List<PriceFetcher>,
    private val now: () -> Long = System::currentTimeMillis,
) {
    fun canCheck(source: Source): Boolean = fetchers.any { it.supports(source) }

    suspend fun check(): List<PriceAlert> {
        val alerts = mutableListOf<PriceAlert>()
        for (item in basket.all()) {
            if (item.unavailable) continue
            val fetcher = fetchers.firstOrNull { it.supports(item.product.source) } ?: continue
            val previous = item.currentPriceEur ?: item.product.priceEur
            when (val lookup = fetcher.lookup(item.product)) {
                is PriceLookup.Price -> {
                    if (lookup.eur < previous - CENT) alerts += PriceAlert.Dropped(item.product, previous, lookup.eur)
                    basket.updatePrice(item.product.id, lookup.eur, unavailable = false, atMillis = now())
                }
                PriceLookup.Gone -> {
                    alerts += PriceAlert.Gone(item.product)
                    basket.updatePrice(item.product.id, item.currentPriceEur, unavailable = true, atMillis = now())
                }
                PriceLookup.Unknown -> Unit
            }
        }
        return alerts
    }

    private companion object {
        const val CENT = 0.009
    }
}
