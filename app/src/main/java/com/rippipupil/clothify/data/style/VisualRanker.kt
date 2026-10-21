package com.rippipupil.clothify.data.style

import android.graphics.Bitmap
import com.rippipupil.clothify.domain.Product

/** Reordena resultados por parecido visual con la foto del usuario (CLIP en el móvil). */
class VisualRanker(
    private val embedder: ImageEmbedder,
    private val loadImage: suspend (String) -> Bitmap?,
) {
    /** Los [limit] primeros, ordenados por parecido; luego los que no se pudieron comparar y el resto. null sin modelo. */
    suspend fun rank(photo: Bitmap, products: List<Product>, limit: Int = 24): List<Product>? {
        if (!embedder.available) return null
        val target = embedder.embed(photo) ?: return null
        val head = products.take(limit)
        val scored = head.map { product ->
            val vector = loadImage(product.imageUrl)?.let { embedder.embed(it) }
            product to vector?.let { ClipZeroShot.cosine(target, it) }
        }
        val compared = scored.filter { it.second != null }.sortedByDescending { it.second }.map { it.first }
        val uncompared = scored.filter { it.second == null }.map { it.first }
        return compared + uncompared + products.drop(limit)
    }
}
