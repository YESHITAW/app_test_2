package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.machineDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "echodeck_machine_prefs"
)

data class MachineSettings(
    val isMachineEnabled: Boolean = true,
    val ringsBeforeAnswer: Int = 3,
    val maxRecordingSeconds: Int = 60,
    val speakerphoneCapture: Boolean = true,
    val playBeepAfterGreeting: Boolean = true,
    val screenUnknownCallers: Boolean = false
)

class MachinePreferencesRepository(private val context: Context) {

    private object Keys {
        val MACHINE_ENABLED = booleanPreferencesKey("machine_enabled")
        val RINGS_BEFORE_ANSWER = intPreferencesKey("rings_before_answer")
        val MAX_RECORDING_SECONDS = intPreferencesKey("max_recording_seconds")
        val SPEAKERPHONE_CAPTURE = booleanPreferencesKey("speakerphone_capture")
        val PLAY_BEEP = booleanPreferencesKey("play_beep_after_greeting")
        val SCREEN_UNKNOWN = booleanPreferencesKey("screen_unknown_callers")
    }

    val settingsFlow: Flow<MachineSettings> = context.machineDataStore.data.map { prefs ->
        MachineSettings(
            isMachineEnabled = prefs[Keys.MACHINE_ENABLED] ?: true,
            ringsBeforeAnswer = (prefs[Keys.RINGS_BEFORE_ANSWER] ?: 3).coerceIn(1, 8),
            maxRecordingSeconds = prefs[Keys.MAX_RECORDING_SECONDS] ?: 60,
            speakerphoneCapture = prefs[Keys.SPEAKERPHONE_CAPTURE] ?: true,
            playBeepAfterGreeting = prefs[Keys.PLAY_BEEP] ?: true,
            screenUnknownCallers = prefs[Keys.SCREEN_UNKNOWN] ?: false
        )
    }

    suspend fun getSettingsOnce(): MachineSettings = settingsFlow.first()

    suspend fun setMachineEnabled(enabled: Boolean) {
        context.machineDataStore.edit { it[Keys.MACHINE_ENABLED] = enabled }
    }

    suspend fun setRingsBeforeAnswer(rings: Int) {
        context.machineDataStore.edit { it[Keys.RINGS_BEFORE_ANSWER] = rings.coerceIn(1, 8) }
    }

    suspend fun setMaxRecordingSeconds(seconds: Int) {
        context.machineDataStore.edit { it[Keys.MAX_RECORDING_SECONDS] = seconds.coerceIn(15, 180) }
    }

    suspend fun setSpeakerphoneCapture(enabled: Boolean) {
        context.machineDataStore.edit { it[Keys.SPEAKERPHONE_CAPTURE] = enabled }
    }

    suspend fun setPlayBeepAfterGreeting(enabled: Boolean) {
        context.machineDataStore.edit { it[Keys.PLAY_BEEP] = enabled }
    }

    suspend fun setScreenUnknownCallers(enabled: Boolean) {
        context.machineDataStore.edit { it[Keys.SCREEN_UNKNOWN] = enabled }
    }
}
