package com.rippipupil.clothify.data.style

import android.graphics.Bitmap
import com.rippipupil.clothify.domain.product
import com.rippipupil.clothify.ui.bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VisualRankerTest {
    private val photo = bitmap()
    private val images = mapOf("https://img/near.jpg" to bitmap(), "https://img/far.jpg" to bitmap(), "https://img/mid.jpg" to bitmap())
    private val vectors = mapOf(
        photo to floatArrayOf(1f, 0f),
        images.getValue("https://img/near.jpg") to floatArrayOf(0.9f, 0.1f),
        images.getValue("https://img/mid.jpg") to floatArrayOf(0.5f, 0.5f),
        images.getValue("https://img/far.jpg") to floatArrayOf(0f, 1f),
    )

    private inner class Fake(override val available: Boolean = true) : ImageEmbedder {
        override suspend fun embed(bitmap: Bitmap): FloatArray? = vectors[bitmap]
    }

    private fun ranker(available: Boolean = true) = VisualRanker(Fake(available)) { images[it] }

    @Test
    fun ordersByVisualSimilarity() = runTest {
        val products = listOf(product("far"), product("broken"), product("mid"), product("near"))
        val ranked = ranker().rank(photo, products)!!
        assertEquals(listOf("near", "mid", "far", "broken"), ranked.map { it.id })
    }

    @Test
    fun keepsProductsBeyondTheLimitAtTheEnd() = runTest {
        val products = listOf(product("far"), product("near"), product("mid"))
        assertEquals(listOf("near", "far", "mid"), ranker().rank(photo, products, limit = 2)!!.map { it.id })
    }

    @Test
    fun noModelGivesNull() = runTest {
        assertNull(ranker(available = false).rank(photo, listOf(product("near"))))
    }
}
