package com.rippipupil.clothify.ui.basket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.BasketItem
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.ui.formatPrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BasketViewModel(private val basket: BasketRepository) : ViewModel() {
    val items: StateFlow<List<BasketItem>> =
        basket.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun remove(productId: String): Job = viewModelScope.launch { basket.remove(productId) }

    companion object {
        /** Guardada hace más de 3 días: puede que ya se haya vendido o cambiado de precio. */
        fun isStale(item: BasketItem, nowMillis: Long = System.currentTimeMillis()): Boolean =
            nowMillis - item.addedAtMillis > ProductPoolRepository.EXPIRY_MS

        /** Precio a mostrar: el último comprobado o, si no hay, el de cuando se guardó. */
        fun displayedPrice(item: BasketItem): Double = item.currentPriceEur ?: item.product.priceEur

        /** Línea de estado del precio; null si no hay nada que contar. */
        fun priceLabel(item: BasketItem, canCheck: (Source) -> Boolean): String? {
            val original = item.product.priceEur
            val current = item.currentPriceEur
            return when {
                item.unavailable -> "Ya no está disponible"
                !canCheck(item.product.source) -> "Sin aviso de precio"
                current == null -> null
                current < original - CENT -> "Antes ${formatPrice(original)} · ahora ${formatPrice(current)}"
                current > original + CENT -> "Ha subido a ${formatPrice(current)}"
                else -> null
            }
        }

        private const val CENT = 0.009
    }
}
