package com.example.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.musicplayer.ui.screens.*
import com.example.musicplayer.ui.theme.MusicPlayerTheme

class MainActivity : ComponentActivity() {

    private var musicService: MusicService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicService = (service as? MusicService.MediaSessionBinder)?.service
            musicService?.let { viewModel.connectService(it) }
        }
        override fun onServiceDisconnected(name: ComponentName?) { musicService = null }
    }

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()

        bindService(
            Intent(this, MusicService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
        startService(Intent(this, MusicService::class.java))

        setContent {
            MusicPlayerTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == "library",
                                onClick = {
                                    navController.navigate("library") {
                                        popUpTo("library") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text("🎵") },
                                label = { Text("Library") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "folders",
                                onClick = {
                                    navController.navigate("folders") {
                                        popUpTo("library") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text("📁") },
                                label = { Text("Folders") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == "player",
                                onClick = {
                                    navController.navigate("player") {
                                        popUpTo("library") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Text("▶️") },
                                label = { Text("Player") }
                            )
                        }
                    }
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "library",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("library") { LibraryScreen(viewModel, navController) }
                        composable("folders") { FoldersScreen(viewModel, navController) }
                        composable("player") {
                            PlayerScreen(
                                viewModel,
                                onNavigateToLyrics = { navController.navigate("lyrics") }
                            )
                        }
                        composable("lyrics") { LyricsScreen(viewModel) }
                    }
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }) {
            requestPermissions(permissions, 0)
        }
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
    }
}
