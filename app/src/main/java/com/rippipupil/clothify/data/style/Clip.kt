package com.rippipupil.clothify.data.style

import com.rippipupil.clothify.domain.TagGroup
import com.rippipupil.clothify.domain.TagVocabulary
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlin.math.sqrt

/** Prepara una imagen 224×224 (píxeles ARGB) como entrada del modelo CLIP: RGB normalizado, formato NCHW. */
object ClipPreprocessor {
    const val SIZE = 224
    private val MEAN = floatArrayOf(0.48145466f, 0.4578275f, 0.40821073f)
    private val STD = floatArrayOf(0.26862954f, 0.26130258f, 0.27577711f)

    fun normalize(pixels: IntArray): FloatArray {
        val plane = SIZE * SIZE
        require(pixels.size == plane) { "Se esperaban ${SIZE}x$SIZE píxeles" }
        val out = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val p = pixels[i]
            out[i] = (((p shr 16) and 0xFF) / 255f - MEAN[0]) / STD[0]
            out[plane + i] = (((p shr 8) and 0xFF) / 255f - MEAN[1]) / STD[1]
            out[2 * plane + i] = ((p and 0xFF) / 255f - MEAN[2]) / STD[2]
        }
        return out
    }
}

/** Vector de texto CLIP de cada etiqueta (generado con tools/clip/embed_tags.py). */
class TagEmbeddings(val vectors: Map<String, FloatArray>) {
    val dimension: Int get() = vectors.values.firstOrNull()?.size ?: 0

    companion object {
        fun parse(json: String): TagEmbeddings {
            val root = Json.parseToJsonElement(json) as JsonObject
            val tags = root["tags"] as JsonObject
            return TagEmbeddings(
                tags.mapValues { (_, v) -> (v as JsonArray).map { (it as JsonPrimitive).float }.toFloatArray() },
            ).also { check(it.dimension == (root["dim"] as JsonPrimitive).int) }
        }
    }
}

/** Clasificación sin entrenamiento: la etiqueta más parecida (coseno) de cada grupo. */
object ClipZeroShot {
    private val ORDER = listOf(TagGroup.CATEGORY, TagGroup.STYLE, TagGroup.COLOR, TagGroup.FIT_MATERIAL)

    fun tags(image: FloatArray, embeddings: TagEmbeddings): List<String> = ORDER.mapNotNull { group ->
        TagVocabulary.all
            .filter { it.group == group && it.id in embeddings.vectors }
            .maxByOrNull { cosine(image, embeddings.vectors.getValue(it.id)) }
            ?.id
    }

    fun cosine(a: FloatArray, b: FloatArray): Double {
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        return if (na == 0.0 || nb == 0.0) 0.0 else dot / (sqrt(na) * sqrt(nb))
    }
}
