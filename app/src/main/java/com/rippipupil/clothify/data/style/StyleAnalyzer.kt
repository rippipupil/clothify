package com.rippipupil.clothify.data.style

import android.graphics.Bitmap

/** Saca etiquetas del vocabulario a partir de una foto de ropa. */
interface StyleAnalyzer {
    suspend fun analyze(image: Bitmap): StyleResult
}

sealed interface StyleResult {
    data class Success(val tagIds: List<String>) : StyleResult
    data class Failure(val reason: FailureReason) : StyleResult
}

enum class FailureReason { NO_INTERNET, QUOTA_EXCEEDED, NO_TAGS, MISSING_KEY, UNKNOWN }
