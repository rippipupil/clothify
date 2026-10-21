package com.rippipupil.clothify.domain

object Similarity {
    fun jaccard(a: Set<String>, b: Set<String>): Double {
        val union = (a union b).size
        return if (union == 0) 0.0 else (a intersect b).size.toDouble() / union
    }

    fun rank(query: Set<String>, products: List<Product>, limit: Int = 60): List<Product> =
        products
            .map { it to jaccard(query, it.tagIds) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
}
