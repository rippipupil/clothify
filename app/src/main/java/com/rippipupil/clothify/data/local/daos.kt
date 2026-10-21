package com.rippipupil.clothify.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Upsert
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM product")
    suspend fun all(): List<ProductEntity>

    @Query("SELECT * FROM product WHERE fetchedAtMillis >= :minFetchedAt AND id NOT IN (SELECT productId FROM seen_product)")
    suspend fun unseenFresh(minFetchedAt: Long): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM product WHERE fetchedAtMillis >= :minFetchedAt AND id NOT IN (SELECT productId FROM seen_product)")
    suspend fun countUnseenFresh(minFetchedAt: Long): Int

    @Query("DELETE FROM product WHERE fetchedAtMillis < :millis")
    suspend fun deleteOlderThan(millis: Long)

    @Query("DELETE FROM product")
    suspend fun deleteAll()
}

@Dao
interface SeenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(seen: SeenProductEntity)

    @Query("SELECT productId FROM seen_product")
    suspend fun allIds(): List<String>

    @Query("DELETE FROM seen_product")
    suspend fun deleteAll()
}

@Dao
interface TagScoreDao {
    @Query("SELECT * FROM tag_score")
    fun observeAll(): Flow<List<TagScoreEntity>>

    @Query("SELECT * FROM tag_score")
    suspend fun all(): List<TagScoreEntity>

    @Upsert
    suspend fun upsertAll(scores: List<TagScoreEntity>)

    @Query("DELETE FROM tag_score")
    suspend fun deleteAll()
}

@Dao
interface BasketDao {
    @Query("SELECT * FROM basket_item ORDER BY addedAtMillis DESC")
    fun observeAll(): Flow<List<BasketItemEntity>>

    @Upsert
    suspend fun upsert(item: BasketItemEntity)

    @Query("DELETE FROM basket_item WHERE productId = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM basket_item")
    suspend fun deleteAll()
}

@Dao
interface QueryHistoryDao {
    @Upsert
    suspend fun upsertAll(entries: List<QueryHistoryEntity>)

    @Query("SELECT text FROM query_history WHERE atMillis >= :minMillis")
    suspend fun textsSince(minMillis: Long): List<String>

    @Query("DELETE FROM query_history")
    suspend fun deleteAll()
}
