package com.rippipupil.clothify.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleTaggerTest {
    @Test
    fun tagsSpanishTitleIgnoringCase() {
        val tags = TitleTagger.tag("SUDADERA Oversize negra Nike")
        assertTrue(tags.containsAll(listOf("sudadera", "oversize", "negro")))
    }

    @Test
    fun tagsEnglishSynonyms() {
        val tags = TitleTagger.tag("Hoodie black")
        assertTrue(tags.containsAll(listOf("sudadera", "negro")))
    }

    @Test
    fun jeansImplyDenim() {
        val tags = TitleTagger.tag("Pantalón vaquero recto")
        assertTrue(tags.containsAll(listOf("vaqueros", "recto", "denim")))
    }

    @Test
    fun matchesWholeWordsOnly() {
        assertFalse(TitleTagger.tag("cartera").contains("camiseta"))
        assertFalse(TitleTagger.tag("Sudadera con capucha").contains("gorra"))
    }

    @Test
    fun handlesPunctuationBetweenWords() {
        assertTrue(TitleTagger.tag("chaqueta,vaquera-oversize").containsAll(listOf("chaqueta", "denim", "oversize")))
    }
}
