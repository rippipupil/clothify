package com.rippipupil.clothify.domain

import kotlin.random.Random

fun product(
    id: String,
    tags: Set<String> = emptySet(),
    source: Source = Source.WALLAPOP,
    price: Double = 20.0,
    fetchedAt: Long = 0L,
) = Product(
    id = id,
    source = source,
    title = id,
    imageUrl = "https://img/$id.jpg",
    priceEur = price,
    sellerName = source.label,
    productUrl = "https://shop/$id",
    size = null,
    distanceKm = null,
    tagIds = tags,
    fetchedAtMillis = fetchedAt,
)

/** Random controlado: nextDouble devuelve [double]; nextInt devuelve [int] acotado. */
class FixedRandom(private val double: Double = 0.5, private val int: Int = 0) : Random() {
    override fun nextBits(bitCount: Int): Int = 0
    override fun nextDouble(): Double = double
    override fun nextInt(until: Int): Int = int.coerceAtMost(until - 1)
    override fun nextInt(from: Int, until: Int): Int = (from + int).coerceAtMost(until - 1)
}
