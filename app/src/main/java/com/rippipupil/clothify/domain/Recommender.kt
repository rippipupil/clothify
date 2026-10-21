package com.rippipupil.clothify.domain

import kotlin.math.sqrt
import kotlin.random.Random

/** Elige la siguiente tarjeta: 80 % la de mejor nota, 20 % una al azar. */
class Recommender(private val random: Random) {

    fun score(product: Product, scores: Map<String, Double>): Double {
        if (product.tagIds.isEmpty()) return 0.0
        return product.tagIds.sumOf { scores[it] ?: 0.0 } / sqrt(product.tagIds.size.toDouble())
    }

    fun next(candidates: List<Product>, scores: Map<String, Double>, recentSources: List<Source>): Product? {
        val pool = withSourceVariety(candidates, recentSources)
        if (pool.isEmpty()) return null
        if (random.nextDouble() < EXPLORATION_RATE) return pool[random.nextInt(pool.size)]
        val best = pool.maxOf { score(it, scores) }
        val ties = pool.filter { score(it, scores) == best }
        return ties[random.nextInt(ties.size)]
    }

    private fun withSourceVariety(candidates: List<Product>, recentSources: List<Source>): List<Product> {
        val last = recentSources.takeLast(MAX_SAME_SOURCE_IN_A_ROW)
        if (last.size < MAX_SAME_SOURCE_IN_A_ROW || last.distinct().size != 1) return candidates
        val others = candidates.filter { it.source != last.first() }
        return others.ifEmpty { candidates }
    }

    companion object {
        const val EXPLORATION_RATE = 0.2
        const val MAX_SAME_SOURCE_IN_A_ROW = 3
    }
}
