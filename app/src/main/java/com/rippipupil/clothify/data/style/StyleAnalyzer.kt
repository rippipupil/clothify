package com.rippipupil.clothify.data.style

import android.graphics.Bitmap

/** Saca etiquetas del vocabulario a partir de una foto de ropa. */
interface StyleAnalyzer {
    suspend fun analyze(image: Bitmap): StyleResult
}

sealed interface StyleResult {
    /** [onDevice]: analizada en el móvil con CLIP (sin internet) en vez de con Gemini. */
    data class Success(val tagIds: List<String>, val onDevice: Boolean = false) : StyleResult
    data class Failure(val reason: FailureReason) : StyleResult
}

enum class FailureReason { NO_INTERNET, QUOTA_EXCEEDED, NO_TAGS, MISSING_KEY, MODEL_UNAVAILABLE, UNKNOWN }
