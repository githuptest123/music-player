package com.example.musicplayer.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.musicplayer.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToLyrics: () -> Unit = {}
) {
    val songs by viewModel.songs.collectAsState()
    val currentIndex by viewModel.currentSongIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val song = songs.getOrNull(currentIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song?.albumArtUri)
                .crossfade(true)
                .build(),
            contentDescription = "Album Art",
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = song?.title ?: "No song",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(text = song?.artist ?: "", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // Progress slider - drag to seek
        Slider(
            value = if (duration > 0) position.toFloat() / duration else 0f,
            onValueChange = { fraction ->
                viewModel.seekTo((fraction * duration).toLong())
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(position), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(duration), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            IconButton(onClick = { viewModel.skipPrevious() }) {
                Text("⏮", style = MaterialTheme.typography.headlineMedium)
            }

            // -10 seconds (hold to continue)
            HoldToSeekButton(
                label = "⏪",
                onSingleTap = { viewModel.skipBackward() },
                onHold = { viewModel.skipBackward() }
            )

            // Play/Pause
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier.size(64.dp)
            ) {
                Text(
                    if (isPlaying) "⏸" else "▶",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // +10 seconds (hold to continue)
            HoldToSeekButton(
                label = "⏩",
                onSingleTap = { viewModel.skipForward() },
                onHold = { viewModel.skipForward() }
            )

            // Next
            IconButton(onClick = { viewModel.skipNext() }) {
                Text("⏭", style = MaterialTheme.typography.headlineMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extra buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            var showTimerDialog by remember { mutableStateOf(false) }
            TextButton(onClick = { showTimerDialog = true }) {
                Text("⏰ Timer")
            }
            if (showTimerDialog) {
                SleepTimerDialog(viewModel) { showTimerDialog = false }
            }

            TextButton(onClick = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                LocalContext.current.startActivity(intent)
            }) {
                Text("🎚️ EQ")
            }
        }

        // Lyrics button
        TextButton(onClick = onNavigateToLyrics) {
            Text("🎤 Lyrics")
        }
    }
}

@Composable
fun HoldToSeekButton(
    label: String,
    onSingleTap: () -> Unit,
    onHold: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }

    IconButton(
        onClick = { onSingleTap() },
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    isHolding = true
                    coroutineScope.launch {
                        delay(200)
                        while (isHolding) {
                            onHold()
                            delay(150)
                        }
                    }
                },
                onPress = {
                    tryAwaitRelease()
                    isHolding = false
                }
            )
        }
    ) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun SleepTimerDialog(viewModel: PlayerViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(5, 15, 30, 60, 90, 0).forEach { minutes ->
                    TextButton(onClick = {
                        viewModel.setSleepTimer(minutes)
                        onDismiss()
                    }) {
                        Text(if (minutes == 0) "Turn Off" else "$minutes minutes")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
