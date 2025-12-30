package com.cogniter.watchaccuracychecker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cogniter.watchaccuracychecker.database.entity.*

@Database(
    entities = [WatchEntity::class, SubItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchDao(): WatchDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watch_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
