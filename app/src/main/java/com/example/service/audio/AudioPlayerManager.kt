package com.example.service.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "AudioPlayerManager"

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingId = MutableStateFlow<Long?>(null)
    val currentPlayingId: StateFlow<Long?> = _currentPlayingId.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _currentPositionMillis = MutableStateFlow(0L)
    val currentPositionMillis: StateFlow<Long> = _currentPositionMillis.asStateFlow()

    private val _totalDurationMillis = MutableStateFlow(0L)
    val totalDurationMillis: StateFlow<Long> = _totalDurationMillis.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun play(id: Long, filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist: $filePath")
            return
        }

        // If clicking same item and is playing -> pause
        if (_currentPlayingId.value == id && _isPlaying.value) {
            pause()
            return
        }

        // If clicking same item and is paused -> resume
        if (_currentPlayingId.value == id && mediaPlayer != null && !_isPlaying.value) {
            resume()
            return
        }

        // New item
        stop()

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMillis.value = 0L
                    progressJob?.cancel()
                }
            }

            // Apply current speed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = PlaybackParams().apply { speed = _playbackSpeed.value }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set playback speed", e)
                }
            }

            player.start()
            mediaPlayer = player
            _currentPlayingId.value = id
            _currentPlayingPath.value = filePath
            _totalDurationMillis.value = player.duration.toLong().coerceAtLeast(1000L)
            _isPlaying.value = true

            startProgressTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio file: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _isPlaying.value = false
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio", e)
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _isPlaying.value = true
            startProgressTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio", e)
        }
    }

    fun seekTo(positionMillis: Long) {
        try {
            mediaPlayer?.seekTo(positionMillis.toInt())
            _currentPositionMillis.value = positionMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio", e)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    val wasPlaying = it.isPlaying
                    it.playbackParams = PlaybackParams().apply { this.speed = speed }
                    if (!wasPlaying) {
                        it.pause()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply playback speed $speed", e)
            }
        }
    }

    fun cycleSpeed() {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        setSpeed(nextSpeed)
    }

    fun stop() {
        try {
            progressJob?.cancel()
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingId.value = null
            _currentPlayingPath.value = null
            _currentPositionMillis.value = 0L
            _totalDurationMillis.value = 0L
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (_isPlaying.value) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    _currentPositionMillis.value = pos
                } catch (e: Exception) {
                    // Ignore
                }
                delay(100)
            }
        }
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
