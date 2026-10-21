package com.rippipupil.clothify.data.style

import android.graphics.Bitmap
import com.rippipupil.clothify.data.sources.fetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import kotlin.math.max

/** Analiza fotos con la Gemini API (capa gratuita, clave de Google AI Studio). */
class GeminiStyleAnalyzer(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/",
) : StyleAnalyzer {

    override suspend fun analyze(image: Bitmap): StyleResult = analyzeJpeg(toJpeg(image))

    suspend fun analyzeJpeg(jpeg: ByteArray): StyleResult {
        if (apiKey.isBlank()) return StyleResult.Failure(FailureReason.MISSING_KEY)
        val url = baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("v1beta/models/$model:generateContent")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(GeminiRequest.body(Base64.getEncoder().encodeToString(jpeg)).toRequestBody(JSON))
            .build()
        val response = try {
            client.fetch(request)
        } catch (e: IOException) {
            return StyleResult.Failure(FailureReason.NO_INTERNET)
        }
        return when (response.code) {
            200 -> GeminiResponseParser.parse(response.body)
                .takeIf { it.isNotEmpty() }
                ?.let { StyleResult.Success(it) }
                ?: StyleResult.Failure(FailureReason.NO_TAGS)
            429 -> StyleResult.Failure(FailureReason.QUOTA_EXCEEDED)
            else -> StyleResult.Failure(FailureReason.UNKNOWN)
        }
    }

    private suspend fun toJpeg(image: Bitmap): ByteArray = withContext(Dispatchers.Default) {
        val scale = MAX_SIDE.toFloat() / max(image.width, image.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(image, (image.width * scale).toInt(), (image.height * scale).toInt(), true)
        } else {
            image
        }
        ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
    }

    private companion object {
        const val MAX_SIDE = 1024
        const val JPEG_QUALITY = 85
        val JSON = "application/json".toMediaType()
    }
}
