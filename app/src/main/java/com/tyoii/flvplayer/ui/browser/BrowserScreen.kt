package com.tyoii.flvplayer.ui.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyoii.flvplayer.data.model.DriveFile
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    state: BrowserState,
    onFolderClick: (DriveFile) -> Unit,
    onFileClick: (DriveFile) -> Unit,
    onPathClick: (Int) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    var showSearch by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = {
                                searchText = it
                                onSearch(it)
                            },
                            placeholder = { Text("搜索视频...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("FLV Player")
                    }
                },
                navigationIcon = {
                    if (state.currentPath.size > 1 || state.isSearching) {
                        IconButton(onClick = {
                            if (showSearch) {
                                showSearch = false
                                searchText = ""
                            }
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showSearch = !showSearch
                        if (!showSearch) searchText = ""
                    }) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            "搜索"
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.Logout, "退出登录")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Breadcrumb path
            if (!state.isSearching) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(state.currentPath) { index, item ->
                        if (index > 0) {
                            Text(" > ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onPathClick(index) }) {
                            Text(
                                item.name,
                                color = if (index == state.currentPath.lastIndex)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    state.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Error, "错误", tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    state.files.isEmpty() -> {
                        Text(
                            if (state.isSearching) "未找到匹配文件" else "空文件夹",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.files, key = { it.id }) { file ->
                                FileItem(
                                    file = file,
                                    onClick = {
                                        if (file.isFolder) onFolderClick(file)
                                        else onFileClick(file)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(file: DriveFile, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = file.displayName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (file.isFolder) FontWeight.Medium else FontWeight.Normal
            )
        },
        supportingContent = {
            if (!file.isFolder) {
                Text(
                    text = buildString {
                        append(file.sizeFormatted)
                        if (file.modifiedTime > 0) {
                            append(" · ")
                            append(dateFormat.format(Date(file.modifiedTime)))
                        }
                        file.parsedInfo?.let {
                            append(" · ")
                            append(it.hostName)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = when {
                    file.isFolder -> Icons.Default.Folder
                    file.name.endsWith(".flv") -> Icons.Default.VideoFile
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when {
                    file.isFolder -> MaterialTheme.colorScheme.primary
                    file.name.endsWith(".flv") -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = {
            if (file.isFolder) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}
