package com.rippipupil.clothify.data.style

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Codificador de imágenes CLIP (ViT-B/32 cuantizado) con ONNX Runtime.
 * El modelo lo añade GitHub Actions a assets/clip/ al compilar; si no está, [available] es false.
 */
class OnnxClipEmbedder(private val context: Context) : ImageEmbedder {
    private val mutex = Mutex()
    private var session: OrtSession? = null

    override val available: Boolean by lazy {
        runCatching { context.assets.list(ASSET_DIR)?.contains(MODEL_FILE) == true }.getOrDefault(false)
    }

    override suspend fun embed(bitmap: Bitmap): FloatArray? = withContext(Dispatchers.Default) {
        if (!available) return@withContext null
        runCatching {
            mutex.withLock {
                val s = session ?: openSession().also { session = it }
                val input = ClipPreprocessor.normalize(pixels224(bitmap))
                val env = OrtEnvironment.getEnvironment()
                OnnxTensor.createTensor(env, FloatBuffer.wrap(input), longArrayOf(1, 3, 224, 224)).use { tensor ->
                    s.run(mapOf(s.inputNames.first() to tensor)).use { result ->
                        @Suppress("UNCHECKED_CAST")
                        val output = (result[0].value as Array<FloatArray>)[0]
                        val norm = sqrt(output.sumOf { (it * it).toDouble() }).toFloat()
                        FloatArray(output.size) { output[it] / norm }
                    }
                }
            }
        }.getOrNull()
    }

    /** Copia el modelo a almacenamiento interno una vez (ONNX Runtime lo abre desde un fichero). */
    private fun openSession(): OrtSession {
        val file = File(context.filesDir, MODEL_FILE)
        if (!file.exists()) {
            context.assets.open("$ASSET_DIR/$MODEL_FILE").use { input ->
                val tmp = File(context.filesDir, "$MODEL_FILE.tmp")
                tmp.outputStream().use { input.copyTo(it) }
                tmp.renameTo(file)
            }
        }
        return OrtEnvironment.getEnvironment().createSession(file.absolutePath, OrtSession.SessionOptions())
    }

    /** Lado corto a 224 y recorte central 224×224. */
    private fun pixels224(bitmap: Bitmap): IntArray {
        val size = ClipPreprocessor.SIZE
        val scale = size.toFloat() / minOf(bitmap.width, bitmap.height)
        val w = max(size, (bitmap.width * scale).roundToInt())
        val h = max(size, (bitmap.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false), w, h, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, (w - size) / 2, (h - size) / 2, size, size)
        return pixels
    }

    companion object {
        const val ASSET_DIR = "clip"
        const val MODEL_FILE = "vision_model_quantized.onnx"
        const val TAGS_FILE = "tag_embeddings.json"

        fun loadTagEmbeddings(context: Context): TagEmbeddings? = runCatching {
            context.assets.open("$ASSET_DIR/$TAGS_FILE").bufferedReader().use { TagEmbeddings.parse(it.readText()) }
        }.getOrNull()
    }
}
