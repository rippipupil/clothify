package com.rippipupil.clothify.data.pool

import com.rippipupil.clothify.data.local.BasketItemEntity
import com.rippipupil.clothify.data.local.ClothifyDatabase
import com.rippipupil.clothify.data.local.toColumns
import com.rippipupil.clothify.data.local.toDomain
import com.rippipupil.clothify.domain.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BasketItem(val product: Product, val addedAtMillis: Long)

class BasketRepository(db: ClothifyDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.basketDao()

    val items: Flow<List<BasketItem>> = dao.observeAll().map { list ->
        list.map { BasketItem(it.columns.toDomain(it.productId), it.addedAtMillis) }
    }

    suspend fun add(product: Product) = dao.upsert(BasketItemEntity(product.id, product.toColumns(), now()))

    suspend fun remove(productId: String) = dao.delete(productId)

    suspend fun clear() = dao.deleteAll()
}
