package com.example.musicplayer

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.MusicRepository
import com.example.musicplayer.data.Song
import com.example.musicplayer.utils.LrcParser
import com.example.musicplayer.utils.LyricsFetcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val lyricsFetcher = LyricsFetcher()

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _folders = MutableStateFlow<Map<String, List<Song>>>(emptyMap())
    val folders: StateFlow<Map<String, List<Song>>> = _folders

    private val _currentSongIndex = MutableStateFlow(0)
    val currentSongIndex: StateFlow<Int> = _currentSongIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _lyrics = MutableStateFlow<List<LrcParser.LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LrcParser.LyricLine>> = _lyrics

    private val _sleepTimerMinutes = MutableStateFlow(0)
    val sleepTimerMinutes: StateFlow<Int> = _sleepTimerMinutes

    private var musicService: MusicService? = null

    fun connectService(service: MusicService) {
        musicService = service
        observeService()
    }

    private fun observeService() {
        viewModelScope.launch {
            musicService?.playbackState?.collect { state ->
                _isPlaying.value = state.isPlaying
                _playbackPosition.value = state.position
                _duration.value = state.duration
                _currentSongIndex.value = state.currentIndex
                if (_songs.value.isNotEmpty() && _currentSongIndex.value < _songs.value.size) {
                    fetchLyrics(_songs.value[_currentSongIndex.value])
                }
            }
        }
    }

    fun initLibrary() {
        viewModelScope.launch {
            _songs.value = repository.getAllSongs()
            _folders.value = repository.getFoldersWithSongs()
        }
    }

    fun playSong(index: Int) {
        val playlist = _songs.value
        if (playlist.isNotEmpty() && index in playlist.indices) {
            musicService?.playPlaylist(playlist, index)
        }
    }

    fun playFolderSongs(songs: List<Song>, index: Int) {
        if (songs.isNotEmpty() && index in songs.indices) {
            musicService?.playPlaylist(songs, index)
        }
    }

    fun togglePlayPause() { musicService?.togglePlayPause() }
    fun skipNext() { musicService?.skipNext() }
    fun skipPrevious() { musicService?.skipPrevious() }
    fun seekTo(position: Long) { musicService?.seekTo(position) }
    fun skipForward(ms: Long = 10000L) { musicService?.seekBy(ms) }
    fun skipBackward(ms: Long = 10000L) { musicService?.seekBy(-ms) }

    fun setSleepTimer(minutes: Int) {
        _sleepTimerMinutes.value = minutes
        musicService?.setSleepTimer(minutes)
    }

    private fun fetchLyrics(song: Song) {
        viewModelScope.launch {
            _lyrics.value = emptyList()
            val online = lyricsFetcher.fetchLyrics(song.title, song.artist)
            if (online != null) {
                _lyrics.value = LrcParser.parse(online)
            }
        }
    }

    fun translateLyrics() {
        val lyricsText = _lyrics.value.joinToString("\n") { it.text }
        if (lyricsText.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://translate.google.com/?sl=auto&tl=en&text=${Uri.encode(lyricsText)}")
        }
        getApplication<Application>().startActivity(intent)
    }
}
