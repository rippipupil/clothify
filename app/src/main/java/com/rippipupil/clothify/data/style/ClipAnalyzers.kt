package com.rippipupil.clothify.data.style

import android.graphics.Bitmap

/** Convierte una imagen en su vector CLIP. */
interface ImageEmbedder {
    /** false si el modelo no viene en el APK. */
    val available: Boolean
    suspend fun embed(bitmap: Bitmap): FloatArray?
}

/** Análisis de fotos en el móvil, sin internet ni cuotas. */
class ClipStyleAnalyzer(
    private val embedder: ImageEmbedder,
    private val tags: () -> TagEmbeddings?,
) : StyleAnalyzer {
    override suspend fun analyze(image: Bitmap): StyleResult {
        val embeddings = tags()
        if (!embedder.available || embeddings == null) return StyleResult.Failure(FailureReason.MODEL_UNAVAILABLE)
        val vector = embedder.embed(image) ?: return StyleResult.Failure(FailureReason.UNKNOWN)
        return StyleResult.Success(ClipZeroShot.tags(vector, embeddings), onDevice = true)
    }
}

/** Usa [primary] (Gemini) y, si falla por lo que sea, [fallback] (CLIP en el móvil). */
class FallbackStyleAnalyzer(
    private val primary: StyleAnalyzer,
    private val fallback: StyleAnalyzer,
) : StyleAnalyzer {
    override suspend fun analyze(image: Bitmap): StyleResult {
        val first = primary.analyze(image)
        if (first is StyleResult.Success) return first
        val second = fallback.analyze(image)
        return if (second is StyleResult.Success) second else first
    }
}
