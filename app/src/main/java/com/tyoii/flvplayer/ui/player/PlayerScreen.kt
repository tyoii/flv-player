package com.tyoii.flvplayer.ui.player

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.flv.FlvExtractor
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request

import androidx.media3.datasource.okhttp.OkHttpDataSource

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun PlayerScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onProgress: (Long, Long) -> Unit,
    onError: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onSaveProgress: () -> Unit
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    // Set up media source with auth header
    LaunchedEffect(state.streamUrl, state.accessToken) {
        if (state.streamUrl.isNotEmpty() && state.accessToken.isNotEmpty()) {
            val okHttpClient = OkHttpClient.Builder().build()
            val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
                .setDefaultRequestProperties(
                    mapOf("Authorization" to "Bearer ${state.accessToken}")
                )

            val extractorsFactory = DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true)
                .setConstantBitrateSeekingAlwaysEnabled(true)

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(MediaItem.fromUri(state.streamUrl))

            player.setMediaSource(mediaSource)
            player.prepare()

            // Seek to saved position
            if (state.savedPosition > 0) {
                player.seekTo(state.savedPosition)
            }

            player.playWhenReady = true
        }
    }

    // Player listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                onError(error.message ?: "Playback error")
            }
        }
        player.addListener(listener)

        onDispose {
            onSaveProgress()
            player.removeListener(listener)
            player.release()
        }
    }

    // Progress tracker
    LaunchedEffect(player) {
        while (true) {
            if (player.isPlaying) {
                onProgress(player.currentPosition, player.duration.coerceAtLeast(0))
            }
            delay(1000)
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Speed change
    LaunchedEffect(state.playbackSpeed) {
        player.setPlaybackSpeed(state.playbackSpeed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { showControls = !showControls },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 3) {
                            player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                        } else if (offset.x > width * 2 / 3) {
                            player.seekTo(player.currentPosition + 10000)
                        } else {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                        showControls = true
                    }
                )
            }
    ) {
        // Player view
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        // Error
        if (state.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Error, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.error, color = Color.White)
            }
        }

        // Controls overlay
        if (showControls) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .align(Alignment.TopCenter),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    onSaveProgress()
                    onBack()
                }) {
                    Icon(Icons.Default.ArrowBack, "返回", tint = Color.White)
                }
                Text(
                    text = state.fileName,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
            ) {
                // Progress bar
                Slider(
                    value = if (state.duration > 0) state.position.toFloat() / state.duration else 0f,
                    onValueChange = { fraction ->
                        player.seekTo((fraction * state.duration).toLong())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time
                    Text(
                        text = "${formatTime(state.position)} / ${formatTime(state.duration)}",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Rewind 15s
                        IconButton(onClick = {
                            player.seekTo((player.currentPosition - 15000).coerceAtLeast(0))
                        }) {
                            Icon(Icons.Default.Replay10, "后退", tint = Color.White)
                        }

                        // Play/Pause
                        IconButton(onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        }) {
                            Icon(
                                if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                "播放/暂停",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 15s
                        IconButton(onClick = {
                            player.seekTo(player.currentPosition + 15000)
                        }) {
                            Icon(Icons.Default.Forward10, "快进", tint = Color.White)
                        }

                        // Speed
                        Box {
                            TextButton(onClick = { showSpeedMenu = true }) {
                                Text("${state.playbackSpeed}x", color = Color.White)
                            }
                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            onSpeedChange(speed)
                                            showSpeedMenu = false
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
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}
