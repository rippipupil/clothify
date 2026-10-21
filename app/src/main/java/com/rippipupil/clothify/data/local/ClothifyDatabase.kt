package com.rippipupil.clothify.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        BasketItemEntity::class,
        TagScoreEntity::class,
        SeenProductEntity::class,
        QueryHistoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ClothifyDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun seenDao(): SeenDao
    abstract fun tagScoreDao(): TagScoreDao
    abstract fun basketDao(): BasketDao
    abstract fun queryHistoryDao(): QueryHistoryDao

    companion object {
        /** v2: seguimiento del precio de las prendas de la cesta. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE basket_item ADD COLUMN currentPriceEur REAL")
                db.execSQL("ALTER TABLE basket_item ADD COLUMN unavailable INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE basket_item ADD COLUMN checkedAtMillis INTEGER")
            }
        }

        fun create(context: Context): ClothifyDatabase =
            Room.databaseBuilder(context, ClothifyDatabase::class.java, "clothify.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        fun inMemory(context: Context): ClothifyDatabase =
            Room.inMemoryDatabaseBuilder(context, ClothifyDatabase::class.java).allowMainThreadQueries().build()
    }
}
