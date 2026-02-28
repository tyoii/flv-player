package com.tyoii.flvplayer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyoii.flvplayer.data.db.PlayHistory
import com.tyoii.flvplayer.data.db.PlayHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: PlayHistoryDao
) : ViewModel() {

    val histories: StateFlow<List<PlayHistory>> = historyDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearAll() {
        viewModelScope.launch {
            historyDao.deleteAll()
        }
    }
}
