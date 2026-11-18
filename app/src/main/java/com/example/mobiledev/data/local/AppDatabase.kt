package com.example.mobiledev.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * De lokale Room-database van de app (één singleton).
 * Week 6: FavoriteStore en CachedReservation komen hier later bij
 * (entities-lijst uitbreiden en version verhogen).
 */
@Database(entities = [CachedAddress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cachedAddressDao(): CachedAddressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mobiledev.db"
                )
                    // tijdens het project: bij een nieuwe versie gewoon opnieuw beginnen
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
