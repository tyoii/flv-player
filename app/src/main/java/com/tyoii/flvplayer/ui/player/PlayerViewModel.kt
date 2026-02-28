package com.tyoii.flvplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyoii.flvplayer.data.db.PlayHistory
import com.tyoii.flvplayer.data.db.PlayHistoryDao
import com.tyoii.flvplayer.data.drive.DriveRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerState(
    val fileId: String = "",
    val fileName: String = "",
    val streamUrl: String = "",
    val accessToken: String = "",
    val isPlaying: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val playbackSpeed: Float = 1.0f,
    val savedPosition: Long = 0
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val driveRepo: DriveRepository,
    private val historyDao: PlayHistoryDao
) : ViewModel() {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var saveJob: Job? = null

    fun prepare(fileId: String, fileName: String) {
        val streamUrl = driveRepo.getStreamUrl(fileId)

        _state.value = PlayerState(
            fileId = fileId,
            fileName = fileName,
            streamUrl = streamUrl,
            isLoading = true
        )

        // Load token and saved position in background
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = driveRepo.getAccessToken() ?: ""
                val history = historyDao.getByFileId(fileId)
                
                _state.value = _state.value.copy(
                    accessToken = token,
                    savedPosition = if (history != null && !history.isFinished) history.position else 0
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to load credentials: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun onPlaying(isPlaying: Boolean) {
        _state.value = _state.value.copy(isPlaying = isPlaying, isLoading = false)
    }

    fun onProgress(position: Long, duration: Long) {
        _state.value = _state.value.copy(position = position, duration = duration)
        scheduleSave()
    }

    fun onError(error: String) {
        _state.value = _state.value.copy(error = error, isLoading = false)
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(3000) // Save every 3 seconds
            saveProgress()
        }
    }

    fun saveProgress() {
        val s = _state.value
        if (s.fileId.isEmpty() || s.duration <= 0) return

        viewModelScope.launch {
            val info = parseFileName(s.fileName)
            historyDao.upsert(
                PlayHistory(
                    fileId = s.fileId,
                    fileName = s.fileName,
                    filePath = "",
                    position = s.position,
                    duration = s.duration,
                    lastPlayed = System.currentTimeMillis(),
                    hostName = info?.first,
                    title = info?.second
                )
            )
        }
    }

    private fun parseFileName(name: String): Pair<String, String>? {
        val regex = """\[.+?\]\[(.+?)\]\[(.+?)\]""".toRegex()
        return regex.find(name)?.let {
            Pair(it.groupValues[1], it.groupValues[2])
        }
    }

    override fun onCleared() {
        saveProgress()
        super.onCleared()
    }
}
