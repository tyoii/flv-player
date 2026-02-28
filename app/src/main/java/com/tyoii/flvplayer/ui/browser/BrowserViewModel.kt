package com.tyoii.flvplayer.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tyoii.flvplayer.data.drive.DriveRepository
import com.tyoii.flvplayer.data.model.DriveFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserState(
    val files: List<DriveFile> = emptyList(),
    val currentPath: List<PathItem> = listOf(PathItem("root", "My Drive")),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false
)

data class PathItem(val id: String, val name: String)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val driveRepo: DriveRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    fun loadFolder(folderId: String? = null, folderName: String = "My Drive") {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, isSearching = false, searchQuery = "")
            try {
                val files = driveRepo.listFiles(folderId)
                val sortedFiles = files.sortedWith(
                    compareByDescending<DriveFile> { it.isFolder }
                        .thenByDescending { it.modifiedTime }
                )

                val newPath = if (folderId == null) {
                    listOf(PathItem("root", "My Drive"))
                } else {
                    _state.value.currentPath + PathItem(folderId, folderName)
                }

                _state.value = _state.value.copy(
                    files = sortedFiles,
                    currentPath = newPath,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load files"
                )
            }
        }
    }

    fun navigateToPath(index: Int) {
        val path = _state.value.currentPath
        if (index < path.size) {
            val item = path[index]
            val newPath = path.take(index + 1)
            viewModelScope.launch {
                _state.value = _state.value.copy(isLoading = true, currentPath = newPath)
                try {
                    val folderId = if (item.id == "root") null else item.id
                    val files = driveRepo.listFiles(folderId)
                    val sortedFiles = files.sortedWith(
                        compareByDescending<DriveFile> { it.isFolder }
                            .thenByDescending { it.modifiedTime }
                    )
                    _state.value = _state.value.copy(files = sortedFiles, isLoading = false)
                } catch (e: Exception) {
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun goBack(): Boolean {
        val path = _state.value.currentPath
        if (_state.value.isSearching) {
            // Exit search mode, reload current folder
            val current = path.last()
            loadFolder(
                if (current.id == "root") null else current.id,
                current.name
            )
            return true
        }
        if (path.size > 1) {
            navigateToPath(path.size - 2)
            return true
        }
        return false
    }

    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        if (query.length < 2) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, isSearching = true)
            try {
                val files = driveRepo.searchFlvFiles(query)
                _state.value = _state.value.copy(files = files, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
