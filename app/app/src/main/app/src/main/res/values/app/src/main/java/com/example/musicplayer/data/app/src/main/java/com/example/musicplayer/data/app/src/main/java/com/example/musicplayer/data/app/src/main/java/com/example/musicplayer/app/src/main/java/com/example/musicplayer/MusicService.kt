package com.example.musicplayer

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.data.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex = 0
    private var sleepJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    data class PlaybackState(
        val isPlaying: Boolean = false,
        val position: Long = 0L,
        val duration: Long = 0L,
        val currentIndex: Int = 0
    )

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.DEFAULT, true)
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) { updateState() }
                override fun onPlaybackStateChanged(state: Int) {
                    updateState()
                    if (state == Player.STATE_ENDED) skipNext()
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentIndex = player?.currentMediaItemIndex ?: 0
                    updateState()
                }
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) { updateState() }
            })
        }

        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(object : MediaSession.Callback {
                override fun onPlay() { player?.play() }
                override fun onPause() { player?.pause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onSeekTo(position: Long) { seekTo(position) }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        sleepJob?.cancel()
        player?.release()
        mediaSession?.release()
        super.onDestroy()
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int) {
        currentPlaylist = songs
        currentIndex = startIndex
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(Uri.parse(song.path))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri?.let { Uri.parse(it) })
                        .build()
                )
                .build()
        }
        player?.setMediaItems(mediaItems, startIndex, 0L)
        player?.prepare()
        player?.play()
        updateState()
    }

    fun togglePlayPause() {
        if (player?.isPlaying == true) player?.pause() else player?.play()
    }

    fun skipNext() {
        if (currentPlaylist.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % currentPlaylist.size
            playPlaylist(currentPlaylist, nextIndex)
        }
    }

    fun skipPrevious() {
        if (currentPlaylist.isNotEmpty() && (player?.currentPosition ?: 0) > 5000) {
            player?.seekTo(0)
        } else {
            val prevIndex = if (currentIndex - 1 < 0) currentPlaylist.size - 1 else currentIndex - 1
            playPlaylist(currentPlaylist, prevIndex)
        }
    }

    fun seekTo(pos: Long) { player?.seekTo(pos) }

    fun seekBy(ms: Long) {
        val newPos = (player?.currentPosition ?: 0) + ms
        player?.seekTo(newPos.coerceIn(0, player?.duration ?: 0))
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes > 0) {
            sleepJob = CoroutineScope(Dispatchers.Main).launch {
                delay(minutes * 60_000L)
                player?.pause()
            }
        }
    }

    private fun updateState() {
        _playbackState.value = PlaybackState(
            isPlaying = player?.isPlaying ?: false,
            position = player?.currentPosition ?: 0L,
            duration = player?.duration?.takeIf { it >= 0 } ?: 0L,
            currentIndex = currentIndex
        )
    }
}
