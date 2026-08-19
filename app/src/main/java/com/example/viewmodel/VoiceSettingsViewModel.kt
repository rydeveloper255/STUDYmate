package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NovaVoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.voiceDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_voice_settings")

/**
 * ViewModel managing NOVA's voice configuration preferences persisted via Android Jetpack DataStore.
 */
class VoiceSettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    companion object {
        val KEY_VOICE_ENABLED = booleanPreferencesKey("nova_voice_enabled")
        val KEY_VOICE_VOLUME = floatPreferencesKey("nova_voice_volume")
        val KEY_VOICE_SPEED = floatPreferencesKey("nova_voice_speed")
    }

    /**
     * Observable voice settings backed by Jetpack DataStore.
     */
    val voiceSettings: StateFlow<NovaVoiceSettings> = context.voiceDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            NovaVoiceSettings(
                isEnabled = preferences[KEY_VOICE_ENABLED] ?: true,
                volume = preferences[KEY_VOICE_VOLUME] ?: 1.0f,
                speed = preferences[KEY_VOICE_SPEED] ?: 1.0f
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NovaVoiceSettings()
        )

    /**
     * Toggle voice playback capability.
     */
    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            context.voiceDataStore.edit { preferences ->
                preferences[KEY_VOICE_ENABLED] = enabled
            }
        }
    }

    /**
     * Update voice volume (clamped between 0.0f and 1.0f).
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        viewModelScope.launch {
            context.voiceDataStore.edit { preferences ->
                preferences[KEY_VOICE_VOLUME] = clamped
            }
        }
    }

    /**
     * Update voice speech rate speed multiplier (clamped between 0.5f and 2.0f).
     */
    fun setSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        viewModelScope.launch {
            context.voiceDataStore.edit { preferences ->
                preferences[KEY_VOICE_SPEED] = clamped
            }
        }
    }

    /**
     * Reset voice preferences to factory defaults.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            context.voiceDataStore.edit { preferences ->
                preferences[KEY_VOICE_ENABLED] = true
                preferences[KEY_VOICE_VOLUME] = 1.0f
                preferences[KEY_VOICE_SPEED] = 1.0f
            }
        }
    }
}
