package com.example.musicplayer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.musicplayer.PlayerViewModel
import com.example.musicplayer.utils.LrcParser.LyricLine

@Composable
fun LyricsScreen(viewModel: PlayerViewModel) {
    val lyrics by viewModel.lyrics.collectAsState()
    val currentPosition by viewModel.playbackPosition.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Lyrics",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (lyrics.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No lyrics available for this song")
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(lyrics) { index, line ->
                    val isCurrent = index == getCurrentLyricIndex(lyrics, currentPosition)
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (lyrics.isNotEmpty()) {
            Button(
                onClick = { viewModel.translateLyrics() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🌐 Translate Lyrics to English")
            }
        }
    }
}

private fun getCurrentLyricIndex(lyrics: List<LyricLine>, positionMs: Long): Int {
    return lyrics.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
}
