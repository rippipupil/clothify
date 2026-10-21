package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommenderTest {
    private val scores = mapOf("a" to 4.0, "b" to 0.0, "c" to -2.0)

    @Test
    fun emptyCandidatesGiveNull() =
        assertNull(Recommender(FixedRandom()).next(emptyList(), scores, emptyList()))

    @Test
    fun scoreIsSumOverSqrtOfTagCount() {
        val p = product("p", setOf("a", "b", "x", "y"))
        assertEquals(2.0, Recommender(FixedRandom()).score(p, scores), 1e-9)
    }

    @Test
    fun productWithoutTagsScoresZero() =
        assertEquals(0.0, Recommender(FixedRandom()).score(product("p"), scores), 1e-9)

    @Test
    fun exploitationPicksHighestScore() {
        val candidates = listOf(product("low", setOf("c")), product("high", setOf("a")), product("mid", setOf("b")))
        assertEquals("high", Recommender(FixedRandom(double = 0.5)).next(candidates, scores, emptyList())?.id)
    }

    @Test
    fun explorationPicksRandomIndex() {
        val candidates = listOf(product("low", setOf("c")), product("high", setOf("a")), product("mid", setOf("b")))
        assertEquals("mid", Recommender(FixedRandom(double = 0.1, int = 2)).next(candidates, scores, emptyList())?.id)
    }

    @Test
    fun avoidsFourthCardInARowFromSameSource() {
        val candidates = listOf(
            product("wp", setOf("a"), Source.WALLAPOP),
            product("vt", setOf("c"), Source.VINTED),
        )
        val recent = List(3) { Source.WALLAPOP }
        val next = Recommender(FixedRandom(double = 0.5)).next(candidates, scores, recent)
        assertNotEquals(Source.WALLAPOP, next?.source)
    }

    @Test
    fun allowsSameSourceWhenNoAlternative() {
        val candidates = listOf(product("wp", setOf("a"), Source.WALLAPOP))
        val next = Recommender(FixedRandom()).next(candidates, scores, List(3) { Source.WALLAPOP })
        assertEquals("wp", next?.id)
    }
}
