package com.example.musicplayer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.musicplayer.PlayerViewModel
import com.example.musicplayer.data.Song

@Composable
fun FoldersScreen(viewModel: PlayerViewModel, navController: NavController) {
    val folders by viewModel.folders.collectAsState()
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var folderSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    if (selectedFolder != null) {
        LazyColumn {
            items(folderSongs) { song ->
                ListItem(
                    headlineContent = { Text(song.title) },
                    supportingContent = { Text(song.artist) },
                    modifier = Modifier.clickable {
                        viewModel.playFolderSongs(folderSongs, folderSongs.indexOf(song))
                        navController.navigate("player")
                    }
                )
            }
            item {
                TextButton(onClick = { selectedFolder = null }) {
                    Text("← Back to folders")
                }
            }
        }
    } else {
        if (folders.isEmpty()) {
            Text("No folders found", modifier = Modifier.fillMaxWidth())
        } else {
            LazyColumn {
                items(folders.keys.toList()) { folder ->
                    ListItem(
                        headlineContent = { Text(folder) },
                        supportingContent = {
                            Text("${folders[folder]?.size ?: 0} tracks")
                        },
                        modifier = Modifier.clickable {
                            selectedFolder = folder
                            folderSongs = folders[folder] ?: emptyList()
                        }
                    )
                }
            }
        }
    }
}
