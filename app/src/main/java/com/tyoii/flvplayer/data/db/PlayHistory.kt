package com.tyoii.flvplayer.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "play_history")
data class PlayHistory(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val filePath: String,
    val position: Long = 0,       // milliseconds
    val duration: Long = 0,       // milliseconds
    val lastPlayed: Long = System.currentTimeMillis(),
    val hostName: String? = null,
    val title: String? = null
) {
    val progressPercent: Int
        get() = if (duration > 0) ((position * 100) / duration).toInt() else 0

    val isFinished: Boolean
        get() = progressPercent >= 95
}
