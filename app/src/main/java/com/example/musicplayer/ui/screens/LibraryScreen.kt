package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.musicplayer.PlayerViewModel

@Composable
fun LibraryScreen(viewModel: PlayerViewModel, navController: NavController) {
    LaunchedEffect(Unit) { viewModel.initLibrary() }
    val songs by viewModel.songs.collectAsState()

    if (songs.isEmpty()) {
        Text(
            "No songs found on device",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge
        )
    } else {
        LazyColumn {
            itemsIndexed(songs) { index, song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text("${song.artist} · ${song.album}") },
                    modifier = Modifier.clickable {
                        viewModel.playSong(index)
                        navController.navigate("player")
                    }
                )
            }
        }
    }
}
