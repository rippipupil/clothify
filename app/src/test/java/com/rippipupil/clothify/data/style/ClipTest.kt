package com.rippipupil.clothify.data.style

import com.rippipupil.clothify.domain.TagVocabulary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ClipTest {
    private val side = ClipPreprocessor.SIZE * ClipPreprocessor.SIZE

    @Test
    fun normalizesWhiteAndBlackPixelsIntoNchw() {
        val pixels = IntArray(side) { 0xFF000000.toInt() }
        pixels[0] = 0xFFFFFFFF.toInt()
        val t = ClipPreprocessor.normalize(pixels)
        assertEquals(3 * side, t.size)
        assertEquals((1 - 0.48145466f) / 0.26862954f, t[0], 1e-4f)
        assertEquals((1 - 0.4578275f) / 0.26130258f, t[side], 1e-4f)
        assertEquals((1 - 0.40821073f) / 0.27577711f, t[2 * side], 1e-4f)
        assertEquals(-0.48145466f / 0.26862954f, t[1], 1e-4f)
    }

    @Test
    fun separatesChannels() {
        val pixels = IntArray(side) { 0xFFFF0000.toInt() } // rojo puro
        val t = ClipPreprocessor.normalize(pixels)
        assertTrue(t[5] > 1f)
        assertTrue(t[side + 5] < 0f)
        assertTrue(t[2 * side + 5] < 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsWrongSize() {
        ClipPreprocessor.normalize(IntArray(10))
    }

    @Test
    fun zeroShotPicksBestTagOfEachGroup() {
        fun hot(i: Int) = FloatArray(5).also { it[i] = 1f }
        val embeddings = TagEmbeddings(
            TagVocabulary.all.associate { it.id to hot(4) } + mapOf(
                "sudadera" to hot(0),
                "negro" to hot(1),
                "streetwear" to hot(2),
                "oversize" to hot(3),
            ),
        )
        val tags = ClipZeroShot.tags(floatArrayOf(1f, 1f, 1f, 1f, 0f), embeddings)
        assertEquals(listOf("sudadera", "streetwear", "negro", "oversize"), tags)
    }

    @Test
    fun cosineOfZeroVectorIsZero() = assertEquals(0.0, ClipZeroShot.cosine(FloatArray(3), floatArrayOf(1f, 0f, 0f)), 1e-9)

    @Test
    fun bundledTagEmbeddingsCoverTheVocabulary() {
        val file = listOf("src/main/assets/clip/tag_embeddings.json", "app/src/main/assets/clip/tag_embeddings.json")
            .map(::File).first { it.exists() }
        val embeddings = TagEmbeddings.parse(file.readText())
        assertEquals(512, embeddings.dimension)
        TagVocabulary.all.forEach { tag ->
            val v = embeddings.vectors[tag.id]
            assertTrue(tag.id, v != null)
            assertEquals(tag.id, 1.0, Math.sqrt(v!!.sumOf { (it * it).toDouble() }), 1e-3)
        }
    }
}
