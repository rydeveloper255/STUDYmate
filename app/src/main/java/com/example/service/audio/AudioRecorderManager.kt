package com.example.service.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

private const val TAG = "AudioRecorderManager"

class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _durationMillis = MutableStateFlow(0L)
    val durationMillis: StateFlow<Long> = _durationMillis.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private var startTimeMillis: Long = 0L
    private var accumulatedDurationMillis: Long = 0L

    fun startRecording(): File? {
        if (_isRecording.value) {
            Log.w(TAG, "Recording is already in progress.")
            return currentOutputFile
        }

        val voiceNotesDir = File(context.filesDir, "voice_notes")
        if (!voiceNotesDir.exists()) {
            voiceNotesDir.mkdirs()
        }

        val outputFile = File(voiceNotesDir, "voice_note_${System.currentTimeMillis()}.m4a")
        currentOutputFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _isPaused.value = false
            startTimeMillis = System.currentTimeMillis()
            accumulatedDurationMillis = 0L
            _durationMillis.value = 0L
            _amplitudeHistory.value = emptyList()

            startAmplitudePolling()
            return outputFile
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording: ${e.message}", e)
            cleanup()
            return null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state starting recorder: ${e.message}", e)
            cleanup()
            return null
        }
    }

    fun pauseRecording() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.pause()
                accumulatedDurationMillis += System.currentTimeMillis() - startTimeMillis
                _isPaused.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recording", e)
        }
    }

    fun resumeRecording() {
        if (!_isRecording.value || !_isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaRecorder?.resume()
                startTimeMillis = System.currentTimeMillis()
                _isPaused.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recording", e)
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        val file = currentOutputFile
        try {
            recordingJob?.cancel()
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error calling stop() on MediaRecorder", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            _isPaused.value = false
            _currentAmplitude.value = 0f
        }
        return file
    }

    fun cancelRecording() {
        stopRecording()
        currentOutputFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        currentOutputFile = null
        _durationMillis.value = 0L
        _amplitudeHistory.value = emptyList()
    }

    private fun startAmplitudePolling() {
        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.Default) {
            while (_isRecording.value) {
                if (!_isPaused.value) {
                    val currentElapsed = accumulatedDurationMillis + (System.currentTimeMillis() - startTimeMillis)
                    _durationMillis.value = currentElapsed

                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }

                    // Normalize amplitude: maxAmplitude returns 0..32767
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0.05f, 1f)
                    _currentAmplitude.value = normalized

                    // Keep a rolling history of recent amplitudes for waveform (last 40 points)
                    val currentList = _amplitudeHistory.value.toMutableList()
                    currentList.add(normalized)
                    if (currentList.size > 50) {
                        currentList.removeAt(0)
                    }
                    _amplitudeHistory.value = currentList
                }
                delay(100)
            }
        }
    }

    private fun cleanup() {
        try {
            recordingJob?.cancel()
            mediaRecorder?.release()
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        _isRecording.value = false
        _isPaused.value = false
        _currentAmplitude.value = 0f
        currentOutputFile = null
    }
}
