package com.rippipupil.clothify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        BasketItemEntity::class,
        TagScoreEntity::class,
        SeenProductEntity::class,
        QueryHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ClothifyDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun seenDao(): SeenDao
    abstract fun tagScoreDao(): TagScoreDao
    abstract fun basketDao(): BasketDao
    abstract fun queryHistoryDao(): QueryHistoryDao

    companion object {
        fun create(context: Context): ClothifyDatabase =
            Room.databaseBuilder(context, ClothifyDatabase::class.java, "clothify.db").build()

        fun inMemory(context: Context): ClothifyDatabase =
            Room.inMemoryDatabaseBuilder(context, ClothifyDatabase::class.java).allowMainThreadQueries().build()
    }
}
