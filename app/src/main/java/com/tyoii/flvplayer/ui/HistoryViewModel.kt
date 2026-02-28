package com.tyoii.flvplayer.ui

import androidx.lifecycle.ViewModel
import com.tyoii.flvplayer.data.db.PlayHistory
import com.tyoii.flvplayer.data.db.PlayHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: PlayHistoryDao
) : ViewModel() {

    val historyList: Flow<List<PlayHistory>> = historyDao.getAll()

    fun clearAll() {
        viewModelScope.launch {
            historyDao.deleteAll()
        }
    }
}
