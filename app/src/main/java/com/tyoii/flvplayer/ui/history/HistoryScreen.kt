package com.tyoii.flvplayer.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyoii.flvplayer.data.db.PlayHistory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    historyList: List<PlayHistory>,
    onItemClick: (PlayHistory) -> Unit,
    onClearAll: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (historyList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("暂无播放记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("播放历史", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { showClearDialog = true }) {
                    Text("清空")
                }
            }

            LazyColumn {
                items(historyList, key = { it.fileId }) { item ->
                    HistoryItem(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空播放记录") },
            text = { Text("确定要清空所有播放记录吗？") },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun HistoryItem(item: PlayHistory, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(
                text = item.hostName?.let { "$it - ${item.title ?: ""}" } ?: item.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { item.progressPercent / 100f },
                    modifier = Modifier.width(60.dp).height(4.dp),
                    color = if (item.isFinished) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${item.progressPercent}% · ${dateFormat.format(Date(item.lastPlayed))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Icon(
                if (item.isFinished) Icons.Default.CheckCircle else Icons.Default.PlayCircleOutline,
                null,
                tint = if (item.isFinished) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.primary
            )
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
}
