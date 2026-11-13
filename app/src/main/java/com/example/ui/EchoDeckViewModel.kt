package com.example.ui

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.EchoDeckApplication
import com.example.audio.PlaybackState
import com.example.data.local.GreetingEntity
import com.example.data.local.GreetingType
import com.example.data.local.MachineSettings
import com.example.data.local.ScreeningAction
import com.example.data.local.ScreeningRuleEntity
import com.example.data.local.VoicemailMessageEntity
import com.example.telecom.CallSessionState
import com.example.telecom.SystemCapabilityHelper
import com.example.telecom.SystemReadiness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class GreetingRecordingUiState(
    val isRecording: Boolean = false,
    val elapsedSeconds: Int = 0,
    val amplitude: Int = 0,
    val pendingAudioPath: String? = null,
    val pendingDurationSeconds: Int = 0
)

data class EchoDeckUiState(
    val settings: MachineSettings = MachineSettings(),
    val messages: List<VoicemailMessageEntity> = emptyList(),
    val unreadCount: Int = 0,
    val greetings: List<GreetingEntity> = emptyList(),
    val activeGreeting: GreetingEntity? = null,
    val screeningRules: List<ScreeningRuleEntity> = emptyList(),
    val playbackState: PlaybackState = PlaybackState(),
    val callSession: CallSessionState = CallSessionState(),
    val readiness: SystemReadiness = SystemReadiness(),
    val greetingRecording: GreetingRecordingUiState = GreetingRecordingUiState(),
    val dialerInput: String = "",
    val previewingTtsGreetingId: Long? = null
)

class EchoDeckViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EchoDeckApplication
    private val repository = app.repository
    private val preferences = app.preferencesRepository
    private val playerController = app.playerController
    private val recorderController = app.recorderController
    private val ttsSpeaker = app.ttsSpeaker
    private val callSessionManager = app.callSessionManager

    private val readinessFlow = MutableStateFlow(SystemCapabilityHelper.checkReadiness(app))
    private val dialerInputFlow = MutableStateFlow("")
    private val pendingGreetingFlow = MutableStateFlow<Pair<String, Int>?>(null)
    private val previewingTtsIdFlow = MutableStateFlow<Long?>(null)

    private val greetingRecordingFlow: StateFlow<GreetingRecordingUiState> = combine(
        recorderController.isRecording,
        recorderController.elapsedSeconds,
        recorderController.amplitudeLevel,
        pendingGreetingFlow
    ) { isRec, elapsed, amp, pending ->
        val sessionRunning = callSessionManager.sessionState.value.isAcousticTest
        GreetingRecordingUiState(
            isRecording = isRec && !sessionRunning,
            elapsedSeconds = if (sessionRunning) 0 else elapsed,
            amplitude = if (sessionRunning) 0 else amp,
            pendingAudioPath = pending?.first,
            pendingDurationSeconds = pending?.second ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GreetingRecordingUiState())

    private val baseDataFlow = combine(
        preferences.settingsFlow,
        repository.messages,
        repository.unreadCount,
        repository.greetings,
        repository.activeGreeting
    ) { settings, messages, unread, greetings, activeGreeting ->
        BaseDataBundle(settings, messages, unread, greetings, activeGreeting)
    }

    private val runtimeFlow = combine(
        repository.screeningRules,
        playerController.playbackState,
        callSessionManager.sessionState,
        readinessFlow,
        greetingRecordingFlow
    ) { rules, playback, session, readiness, recUi ->
        RuntimeBundle(rules, playback, session, readiness, recUi)
    }

    val uiState: StateFlow<EchoDeckUiState> = combine(
        baseDataFlow,
        runtimeFlow,
        dialerInputFlow,
        previewingTtsIdFlow
    ) { base, runtime, dialer, previewTtsId ->
        EchoDeckUiState(
            settings = base.settings,
            messages = base.messages,
            unreadCount = base.unreadCount,
            greetings = base.greetings,
            activeGreeting = base.activeGreeting,
            screeningRules = runtime.rules,
            playbackState = runtime.playback,
            callSession = runtime.session,
            readiness = runtime.readiness,
            greetingRecording = runtime.recUi,
            dialerInput = dialer,
            previewingTtsGreetingId = previewTtsId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EchoDeckUiState())

    fun refreshReadiness() {
        readinessFlow.value = SystemCapabilityHelper.checkReadiness(app)
    }

    // Machine Settings
    fun setMachineEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setMachineEnabled(enabled) }
    }

    fun setRingsBeforeAnswer(rings: Int) {
        viewModelScope.launch { preferences.setRingsBeforeAnswer(rings) }
    }

    fun setMaxRecordingSeconds(seconds: Int) {
        viewModelScope.launch { preferences.setMaxRecordingSeconds(seconds) }
    }

    fun setSpeakerphoneCapture(enabled: Boolean) {
        viewModelScope.launch { preferences.setSpeakerphoneCapture(enabled) }
    }

    fun setPlayBeepAfterGreeting(enabled: Boolean) {
        viewModelScope.launch { preferences.setPlayBeepAfterGreeting(enabled) }
    }

    fun setScreenUnknownCallers(enabled: Boolean) {
        viewModelScope.launch { preferences.setScreenUnknownCallers(enabled) }
    }

    // Acoustic Hardware Self-Test
    fun startAcousticSelfTest() {
        playerController.stop()
        ttsSpeaker.stop()
        callSessionManager.startAcousticSelfTest()
    }

    fun stopAcousticSelfTest() {
        callSessionManager.stopAcousticSelfTest()
    }

    // Inbox Playback & Actions
    fun toggleMessagePlayback(message: VoicemailMessageEntity) {
        ttsSpeaker.stop()
        previewingTtsIdFlow.value = null
        val key = "msg_${message.id}"
        playerController.togglePlayPause(key, message.audioFilePath)
        if (!message.isRead) {
            viewModelScope.launch { repository.markMessageRead(message.id, true) }
        }
    }

    fun seekPlayback(positionMs: Int) {
        playerController.seekTo(positionMs)
    }

    fun togglePlaybackSpeaker() {
        playerController.setSpeakerphone(!playerController.playbackState.value.useSpeakerphone)
    }

    fun stopPlayback() {
        playerController.stop()
    }

    fun toggleStarMessage(message: VoicemailMessageEntity) {
        viewModelScope.launch { repository.toggleStarred(message.id, message.isStarred) }
    }

    fun markMessageRead(id: Long, isRead: Boolean) {
        viewModelScope.launch { repository.markMessageRead(id, isRead) }
    }

    fun updateMessageNotes(id: Long, notes: String) {
        viewModelScope.launch { repository.updateMessageNotes(id, notes) }
    }

    fun deleteMessage(id: Long) {
        if (playerController.playbackState.value.activeKey == "msg_$id") {
            playerController.stop()
        }
        viewModelScope.launch { repository.deleteMessage(id) }
    }

    fun createShareAudioIntent(message: VoicemailMessageEntity): Intent? {
        val file = File(message.audioFilePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Voicemail from ${message.callerName}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // Greetings Studio
    fun activateGreeting(id: Long) {
        viewModelScope.launch { repository.activateGreeting(id) }
    }

    fun previewGreeting(greeting: GreetingEntity) {
        val key = "greet_${greeting.id}"
        when (greeting.type) {
            GreetingType.RECORDED -> {
                ttsSpeaker.stop()
                previewingTtsIdFlow.value = null
                playerController.togglePlayPause(key, greeting.audioFilePath, useSpeakerphone = true)
            }
            GreetingType.TTS -> {
                playerController.stop()
                if (previewingTtsIdFlow.value == greeting.id) {
                    ttsSpeaker.stop()
                    previewingTtsIdFlow.value = null
                } else {
                    viewModelScope.launch {
                        previewingTtsIdFlow.value = greeting.id
                        ttsSpeaker.speakAndAwait(greeting.ttsScript, forVoiceCall = false)
                        if (previewingTtsIdFlow.value == greeting.id) {
                            previewingTtsIdFlow.value = null
                        }
                    }
                }
            }
        }
    }

    fun previewPendingRecording() {
        val pending = pendingGreetingFlow.value ?: return
        playerController.togglePlayPause("pending_greet", pending.first, useSpeakerphone = true)
    }

    fun startRecordingNewGreeting(): Boolean {
        playerController.stop()
        ttsSpeaker.stop()
        previewingTtsIdFlow.value = null
        discardPendingGreetingFile()
        val file = repository.createNewGreetingFile()
        return recorderController.startRecording(file)
    }

    fun stopRecordingNewGreeting() {
        val result = recorderController.stopRecording()
        if (result != null) {
            pendingGreetingFlow.value = result.filePath to result.durationSeconds
        }
    }

    fun savePendingRecordedGreeting(title: String, activateImmediately: Boolean = true) {
        val pending = pendingGreetingFlow.value ?: return
        playerController.stop()
        pendingGreetingFlow.value = null
        viewModelScope.launch {
            repository.saveRecordedGreeting(
                title = title,
                audioFilePath = pending.first,
                durationSeconds = pending.second,
                activateImmediately = activateImmediately
            )
        }
    }

    fun discardPendingGreetingFile() {
        playerController.stop()
        val pending = pendingGreetingFlow.value
        pendingGreetingFlow.value = null
        if (pending != null) {
            runCatching { File(pending.first).delete() }
        }
        recorderController.cancelRecording()
    }

    fun saveTtsGreeting(title: String, script: String, activateImmediately: Boolean = true) {
        if (script.isBlank()) return
        viewModelScope.launch {
            repository.saveTtsGreeting(
                title = title,
                script = script,
                activateImmediately = activateImmediately
            )
        }
    }

    fun deleteGreeting(id: Long) {
        if (playerController.playbackState.value.activeKey == "greet_$id") {
            playerController.stop()
        }
        viewModelScope.launch { repository.deleteGreeting(id) }
    }

    // Dialer & Call Screening
    fun appendDialerChar(ch: Char) {
        if (dialerInputFlow.value.length < 24) {
            dialerInputFlow.value += ch
        }
    }

    fun backspaceDialer() {
        val current = dialerInputFlow.value
        if (current.isNotEmpty()) {
            dialerInputFlow.value = current.dropLast(1)
        }
    }

    fun clearDialer() {
        dialerInputFlow.value = ""
    }

    fun setDialerNumber(number: String) {
        dialerInputFlow.value = number
    }

    fun placeCall(number: String = dialerInputFlow.value): Boolean {
        return SystemCapabilityHelper.placeCall(app, number)
    }

    fun addScreeningRule(pattern: String, label: String, action: ScreeningAction) {
        if (pattern.isBlank()) return
        viewModelScope.launch { repository.addScreeningRule(pattern, label, action) }
    }

    fun deleteScreeningRule(id: Long) {
        viewModelScope.launch { repository.deleteScreeningRule(id) }
    }

    override fun onCleared() {
        playerController.stop()
        ttsSpeaker.stop()
        super.onCleared()
    }
}

private data class BaseDataBundle(
    val settings: MachineSettings,
    val messages: List<VoicemailMessageEntity>,
    val unreadCount: Int,
    val greetings: List<GreetingEntity>,
    val activeGreeting: GreetingEntity?
)

private data class RuntimeBundle(
    val rules: List<ScreeningRuleEntity>,
    val playback: PlaybackState,
    val session: CallSessionState,
    val readiness: SystemReadiness,
    val recUi: GreetingRecordingUiState
)
