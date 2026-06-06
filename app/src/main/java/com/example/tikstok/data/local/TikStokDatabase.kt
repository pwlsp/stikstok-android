package com.example.tikstok.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local Room database. Per the storage split, this caches only Yahoo **market data** (candles) —
 * the portfolio lives in Firestore. Schema export is off; this is a throwaway cache, so on any
 * schema change we just drop and rebuild it.
 */
@Database(
    entities = [CandleEntity::class, SeriesMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TikStokDatabase : RoomDatabase() {

    abstract fun marketCacheDao(): MarketCacheDao

    companion object {
        @Volatile
        private var instance: TikStokDatabase? = null

        fun get(context: Context): TikStokDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TikStokDatabase::class.java,
                    "tikstok.db",
                )
                    // The cache can always be re-fetched from Yahoo, so a destructive migration is fine.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
