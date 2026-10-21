package com.rippipupil.clothify.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagVocabularyTest {
    @Test
    fun idsAreUnique() {
        val ids = TagVocabulary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun hasEnoughTagsInEveryGroup() {
        assertTrue(TagVocabulary.all.size >= 70)
        TagGroup.entries.forEach { group ->
            assertTrue("$group", TagVocabulary.all.count { it.group == group } >= 8)
        }
    }

    @Test
    fun onlyCategoriesHaveSizeKinds() {
        TagVocabulary.all.filter { it.sizeKind != null }.forEach {
            assertEquals(TagGroup.CATEGORY, it.group)
        }
    }

    @Test
    fun byIdFindsKnownTagsOnly() {
        assertNotNull(TagVocabulary.byId("sudadera"))
        assertNull(TagVocabulary.byId("inventada"))
        assertTrue(TagVocabulary.isValid("negro"))
    }

    @Test
    fun normalizeStripsAccentsCaseAndPunctuation() {
        assertEquals("sudadera oversize marron", TagVocabulary.normalize("Sudadera ÓVERSIZE, Marrón!"))
    }
}
