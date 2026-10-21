package com.rippipupil.clothify.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rippipupil.clothify.data.pool.BasketRepository
import com.rippipupil.clothify.data.pool.ProductPoolRepository
import com.rippipupil.clothify.data.pool.ProfileRepository
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.ProfileEvent
import com.rippipupil.clothify.domain.Recommender
import com.rippipupil.clothify.domain.Source
import com.rippipupil.clothify.ui.sourceWarning
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface DiscoverUiState {
    data object Loading : DiscoverUiState
    data class Cards(val stack: List<Product>, val warning: String?) : DiscoverUiState
    data class Empty(val warning: String?) : DiscoverUiState
}

class DiscoverViewModel(
    private val pool: ProductPoolRepository,
    private val profile: ProfileRepository,
    private val basket: BasketRepository,
    private val recommender: Recommender,
) : ViewModel() {
    private val _state = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    val state: StateFlow<DiscoverUiState> = _state

    private val stack = mutableListOf<Product>()
    private val shownSources = mutableListOf<Source>()
    private var warning: String? = null
    private val mutex = Mutex()
    private var backgroundRefill: Job? = null

    init {
        viewModelScope.launch {
            pool.purgeExpired()
            mutex.withLock { topUp(refillIfEmpty = true) }
        }
    }

    fun onSwipe(product: Product, liked: Boolean): Job = viewModelScope.launch {
        mutex.withLock {
            stack.removeAll { it.id == product.id }
            pool.markSeen(product, liked)
            profile.apply(if (liked) ProfileEvent.Liked(product.tagIds) else ProfileEvent.Disliked(product.tagIds))
            topUp(refillIfEmpty = true)
        }
        if (pool.needsRefill()) refillInBackground()
    }

    fun onAddToBasket(product: Product): Job = viewModelScope.launch { basket.add(product) }

    fun onRetry(): Job = viewModelScope.launch {
        _state.value = DiscoverUiState.Loading
        mutex.withLock {
            warning = sourceWarning(pool.refill().failures)
            topUp(refillIfEmpty = false)
        }
    }

    private fun refillInBackground() {
        if (backgroundRefill?.isActive == true) return
        backgroundRefill = viewModelScope.launch {
            val report = pool.refillIfNeeded()
            mutex.withLock {
                warning = sourceWarning(report.failures)
                topUp(refillIfEmpty = false)
            }
        }
    }

    /** Completa la pila hasta [STACK_SIZE] cartas; si no hay ninguna, busca más en las fuentes. */
    private suspend fun topUp(refillIfEmpty: Boolean) {
        var candidates = pool.candidates().filter { c -> stack.none { it.id == c.id } }
        if (stack.isEmpty() && candidates.isEmpty() && refillIfEmpty) {
            _state.value = DiscoverUiState.Loading
            warning = sourceWarning(pool.refillIfNeeded().failures)
            candidates = pool.candidates()
        }
        val scores = profile.snapshot()
        while (stack.size < STACK_SIZE) {
            val next = recommender.next(candidates, scores, shownSources.takeLast(Recommender.MAX_SAME_SOURCE_IN_A_ROW)) ?: break
            stack += next
            shownSources += next.source
            candidates = candidates - next
        }
        _state.value = if (stack.isEmpty()) DiscoverUiState.Empty(warning) else DiscoverUiState.Cards(stack.toList(), warning)
        if (candidates.size + stack.size < ProductPoolRepository.REFILL_THRESHOLD && stack.isNotEmpty()) refillInBackground()
    }

    private companion object {
        const val STACK_SIZE = 3
    }
}
