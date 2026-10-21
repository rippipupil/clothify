package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileScorerTest {
    private val tags = setOf("sudadera", "oversize")

    @Test
    fun presetAddsThree() {
        val s = ProfileScorer.apply(emptyMap(), ProfileEvent.PresetChosen(tags))
        assertEquals(3.0, s.getValue("sudadera"), 1e-9)
        assertEquals(3.0, s.getValue("oversize"), 1e-9)
    }

    @Test
    fun ownPhotoAddsTwo() =
        assertEquals(2.0, ProfileScorer.apply(emptyMap(), ProfileEvent.OwnPhoto(tags)).getValue("sudadera"), 1e-9)

    @Test
    fun likeAddsOne() =
        assertEquals(1.0, ProfileScorer.apply(emptyMap(), ProfileEvent.Liked(tags)).getValue("sudadera"), 1e-9)

    @Test
    fun dislikeSubtractsHalf() =
        assertEquals(-0.5, ProfileScorer.apply(emptyMap(), ProfileEvent.Disliked(tags)).getValue("sudadera"), 1e-9)

    @Test
    fun scoresAreCappedAtTen() {
        var s = emptyMap<String, Double>()
        repeat(5) { s = ProfileScorer.apply(s, ProfileEvent.PresetChosen(tags)) }
        assertEquals(10.0, s.getValue("sudadera"), 1e-9)
    }

    @Test
    fun scoresAreFlooredAtMinusTen() {
        var s = emptyMap<String, Double>()
        repeat(30) { s = ProfileScorer.apply(s, ProfileEvent.Disliked(tags)) }
        assertEquals(-10.0, s.getValue("sudadera"), 1e-9)
    }

    @Test
    fun otherTagsUntouched() {
        val s = ProfileScorer.apply(mapOf("negro" to 4.0), ProfileEvent.Liked(tags))
        assertEquals(4.0, s.getValue("negro"), 1e-9)
    }

    @Test
    fun unknownTagsIgnored() {
        val s = ProfileScorer.apply(emptyMap(), ProfileEvent.Liked(setOf("inventada")))
        assertTrue(s.isEmpty())
    }
}
