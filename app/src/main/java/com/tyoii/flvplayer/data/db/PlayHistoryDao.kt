package com.tyoii.flvplayer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY lastPlayed DESC")
    fun getAll(): Flow<List<PlayHistory>>

    @Query("SELECT * FROM play_history WHERE fileId = :fileId")
    suspend fun getByFileId(fileId: String): PlayHistory?

    @Upsert
    suspend fun upsert(history: PlayHistory)

    @Delete
    suspend fun delete(history: PlayHistory)

    @Query("DELETE FROM play_history")
    suspend fun deleteAll()
}
