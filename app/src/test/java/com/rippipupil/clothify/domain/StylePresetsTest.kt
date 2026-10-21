package com.rippipupil.clothify.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class StylePresetsTest {
    @Test
    fun atLeastEightPresets() = assertTrue(StylePresets.all.size >= 8)

    @Test
    fun allPresetTagsAreValid() {
        StylePresets.all.flatMap { it.tagIds }.forEach { id -> assertTrue(id, TagVocabulary.isValid(id)) }
    }

    @Test
    fun everyPresetIncludesACategory() {
        StylePresets.all.forEach { preset ->
            assertTrue(preset.label, preset.tagIds.any { TagVocabulary.byId(it)?.group == TagGroup.CATEGORY })
        }
    }
}
