package com.rippipupil.clothify.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rippipupil.clothify.domain.Product
import com.rippipupil.clothify.domain.Source

/** Columnas de un producto, compartidas por la reserva y la cesta. */
data class ProductColumns(
    val source: String,
    val title: String,
    val imageUrl: String,
    val priceEur: Double,
    val sellerName: String,
    val productUrl: String,
    val size: String?,
    val distanceKm: Double?,
    /** Ids separados por comas. */
    val tagIds: String,
    val fetchedAtMillis: Long,
)

@Entity(tableName = "product")
data class ProductEntity(@PrimaryKey val id: String, @Embedded val columns: ProductColumns)

@Entity(tableName = "basket_item")
data class BasketItemEntity(
    @PrimaryKey val productId: String,
    @Embedded val columns: ProductColumns,
    val addedAtMillis: Long,
)

@Entity(tableName = "tag_score")
data class TagScoreEntity(@PrimaryKey val tagId: String, val score: Double)

@Entity(tableName = "seen_product")
data class SeenProductEntity(@PrimaryKey val productId: String, val liked: Boolean, val seenAtMillis: Long)

@Entity(tableName = "query_history")
data class QueryHistoryEntity(@PrimaryKey val text: String, val atMillis: Long)

fun Product.toColumns() = ProductColumns(
    source = source.name,
    title = title,
    imageUrl = imageUrl,
    priceEur = priceEur,
    sellerName = sellerName,
    productUrl = productUrl,
    size = size,
    distanceKm = distanceKm,
    tagIds = tagIds.joinToString(","),
    fetchedAtMillis = fetchedAtMillis,
)

fun Product.toEntity() = ProductEntity(id, toColumns())

fun ProductColumns.toDomain(id: String) = Product(
    id = id,
    source = Source.valueOf(source),
    title = title,
    imageUrl = imageUrl,
    priceEur = priceEur,
    sellerName = sellerName,
    productUrl = productUrl,
    size = size,
    distanceKm = distanceKm,
    tagIds = tagIds.split(",").filter { it.isNotBlank() }.toSet(),
    fetchedAtMillis = fetchedAtMillis,
)

fun ProductEntity.toDomain() = columns.toDomain(id)
