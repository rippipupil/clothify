package com.rippipupil.clothify.data.pool

import com.rippipupil.clothify.data.local.BasketItemEntity
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.toColumns
import com.rippipupil.clothify.data.local.toDomain
import com.rippipupil.clothify.domain.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BasketItem(
    val product: Product,
    val addedAtMillis: Long,
    /** Último precio comprobado; null si nunca se ha comprobado. */
    val currentPriceEur: Double? = null,
    val unavailable: Boolean = false,
)

class BasketRepository(db: ClothifyDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.basketDao()

    val items: Flow<List<BasketItem>> = dao.observeAll().map { list ->
        list.map { it.toItem() }
    }

    suspend fun all(): List<BasketItem> = dao.all().map { it.toItem() }

    suspend fun updatePrice(productId: String, priceEur: Double?, unavailable: Boolean, atMillis: Long) =
        dao.updatePrice(productId, priceEur, unavailable, atMillis)

    suspend fun add(product: Product) = dao.upsert(BasketItemEntity(product.id, product.toColumns(), now()))

    suspend fun remove(productId: String) = dao.delete(productId)

    suspend fun clear() = dao.deleteAll()

    private fun BasketItemEntity.toItem() =
        BasketItem(columns.toDomain(productId), addedAtMillis, currentPriceEur, unavailable)
}
