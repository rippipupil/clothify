package com.rippipupil.clothify.data.sources

import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.SearchQuery
import com.rippipupil.clothify.domain.Source

interface ProductSource {
    val source: Source
    suspend fun search(query: SearchQuery): SourceResult
}

sealed interface SourceResult {
    data class Ok(val products: List<Product>) : SourceResult
    data class Failed(val reason: SourceFailure) : SourceResult
}

enum class SourceFailure { NO_INTERNET, BLOCKED, BUDGET_EXHAUSTED, DISABLED, BAD_RESPONSE }

/** Reserva una búsqueda del presupuesto de SerpApi; false si no quedan. */
interface BudgetGate {
    suspend fun tryConsume(): Boolean
}
