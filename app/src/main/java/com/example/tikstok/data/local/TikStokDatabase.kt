package com.example.tikstok.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
