package com.tyoii.flvplayer.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PlayHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playHistoryDao(): PlayHistoryDao
}
