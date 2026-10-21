package com.rippipupil.clothify.ui.basket

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.BasketItem
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
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
    }
}
