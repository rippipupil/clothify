package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SimilarityTest {
    @Test
    fun jaccard() = assertEquals(1.0 / 3.0, Similarity.jaccard(setOf("a", "b"), setOf("b", "c")), 1e-9)

    @Test
    fun jaccardOfEmptySetsIsZero() = assertEquals(0.0, Similarity.jaccard(emptySet(), emptySet()), 1e-9)

    @Test
    fun rankSortsDescendingAndDropsZero() {
        val products = listOf(
            product("none", setOf("z")),
            product("half", setOf("a", "x")),
            product("full", setOf("a", "b")),
        )
        assertEquals(listOf("full", "half"), Similarity.rank(setOf("a", "b"), products).map { it.id })
    }

    @Test
    fun rankRespectsLimit() {
        val products = List(100) { product("p$it", setOf("a")) }
        assertEquals(60, Similarity.rank(setOf("a"), products).size)
    }
}
