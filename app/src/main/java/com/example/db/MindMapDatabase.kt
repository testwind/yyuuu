package com.example.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MindMapEntity::class,
        SyncConfigEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MindMapDatabase : RoomDatabase() {
    abstract fun mindMapDao(): MindMapDao

    companion object {
        @Volatile
        private var INSTANCE: MindMapDatabase? = null

        fun getDatabase(context: Context): MindMapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindMapDatabase::class.java,
                    "mind_map_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
