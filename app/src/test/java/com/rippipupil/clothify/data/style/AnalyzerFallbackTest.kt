package com.rippipupil.clothify.data.style

import android.graphics.Bitmap
import com.rippipupil.clothify.domain.TagVocabulary
import com.rippipupil.clothify.ui.bitmap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnalyzerFallbackTest {
    private class Fixed(private val result: StyleResult) : StyleAnalyzer {
        var calls = 0
        override suspend fun analyze(image: Bitmap) = result.also { calls++ }
    }

    private class FakeEmbedder(override val available: Boolean, private val vector: FloatArray?) : ImageEmbedder {
        override suspend fun embed(bitmap: Bitmap): FloatArray? = vector
    }

    private val tags = TagEmbeddings(TagVocabulary.all.associate { it.id to floatArrayOf(0f, 1f) } + ("camisa" to floatArrayOf(1f, 0f)))

    @Test
    fun geminiSuccessWins() = runTest {
        val clip = Fixed(StyleResult.Success(listOf("polo"), onDevice = true))
        val result = FallbackStyleAnalyzer(Fixed(StyleResult.Success(listOf("sudadera"))), clip).analyze(bitmap())
        assertEquals(StyleResult.Success(listOf("sudadera")), result)
        assertEquals(0, clip.calls)
    }

    @Test
    fun geminiFailureFallsBackToClip() = runTest {
        val result = FallbackStyleAnalyzer(
            Fixed(StyleResult.Failure(FailureReason.QUOTA_EXCEEDED)),
            Fixed(StyleResult.Success(listOf("polo"), onDevice = true)),
        ).analyze(bitmap())
        assertEquals(StyleResult.Success(listOf("polo"), onDevice = true), result)
    }

    @Test
    fun bothFailingKeepsGeminiError() = runTest {
        val result = FallbackStyleAnalyzer(
            Fixed(StyleResult.Failure(FailureReason.NO_INTERNET)),
            Fixed(StyleResult.Failure(FailureReason.MODEL_UNAVAILABLE)),
        ).analyze(bitmap())
        assertEquals(StyleResult.Failure(FailureReason.NO_INTERNET), result)
    }

    @Test
    fun clipWithoutModelIsUnavailable() = runTest {
        assertEquals(
            StyleResult.Failure(FailureReason.MODEL_UNAVAILABLE),
            ClipStyleAnalyzer(FakeEmbedder(false, null)) { tags }.analyze(bitmap()),
        )
    }

    @Test
    fun clipReturnsOnDeviceTags() = runTest {
        val result = ClipStyleAnalyzer(FakeEmbedder(true, floatArrayOf(1f, 0f))) { tags }.analyze(bitmap()) as StyleResult.Success
        assertEquals(true, result.onDevice)
        assertEquals("camisa", result.tagIds.first())
    }
}
