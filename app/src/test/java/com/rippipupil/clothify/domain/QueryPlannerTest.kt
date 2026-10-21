package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueryPlannerTest {
    private val sizes = UserSizes(top = "L")
    private val scores = mapOf("sudadera" to 6.0, "camiseta" to 5.0, "streetwear" to 4.0, "negro" to 3.0, "oversize" to 2.0)

    private fun plan(seed: Int, scores: Map<String, Double> = this.scores, recent: Set<String> = emptySet()) =
        QueryPlanner(Random(seed)).plan(scores, recent, 60.0, sizes, null)

    @Test
    fun returnsThreeQueries() = assertEquals(3, plan(1).size)

    @Test
    fun eachQueryHasExactlyOneCategoryAndAtMostThreeTags() {
        (1..50).flatMap { plan(it) }.forEach { q ->
            assertEquals(q.text, 1, q.tagIds.count { TagVocabulary.byId(it)?.group == TagGroup.CATEGORY })
            assertTrue(q.text, q.tagIds.size <= 3)
            assertEquals(q.tagIds.first { TagVocabulary.byId(it)?.group == TagGroup.CATEGORY }, q.categoryTag)
        }
    }

    @Test
    fun textStartsWithTheCategory() {
        plan(3).forEach { q -> assertTrue(q.text, q.text.startsWith(TagVocabulary.byId(q.categoryTag!!)!!.searchTerm)) }
    }

    @Test
    fun emptyProfileStillPlansThree() = assertEquals(3, plan(7, scores = emptyMap()).size)

    @Test
    fun exploitationOnlyUsesPositiveTags() {
        (1..50).forEach { seed ->
            val first = plan(seed).first()
            assertTrue(first.text, first.tagIds.all { (scores[it] ?: 0.0) > 0 })
        }
    }

    @Test
    fun avoidsRecentTexts() {
        val onlyCategories = mapOf("sudadera" to 5.0, "camiseta" to 5.0)
        (1..50).forEach { seed ->
            assertNotEquals("sudadera", plan(seed, onlyCategories, recent = setOf("sudadera")).first().text)
        }
    }

    @Test
    fun passesFiltersThrough() {
        val q = QueryPlanner(Random(1)).plan(scores, emptySet(), 45.0, sizes, LatLng(40.0, -3.0)).first()
        assertEquals(45.0, q.maxPriceEur, 1e-9)
        assertEquals(sizes, q.sizes)
        assertEquals(LatLng(40.0, -3.0), q.location)
    }
}
