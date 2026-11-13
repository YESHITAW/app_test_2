package com.example.telecom

import android.content.Context
import com.example.audio.AudioPlayerController
import com.example.audio.AudioRecorderController
import com.example.audio.BeepGenerator
import com.example.audio.TtsSpeaker
import com.example.data.local.EchoDeckRepository
import com.example.data.local.GreetingType
import com.example.data.local.MachinePreferencesRepository
import com.example.data.local.VoicemailMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CallPhase {
    IDLE,
    RINGING,
    DIALING,
    GREETING_PLAYING,
    BEEP_PLAYING,
    RECORDING_MESSAGE,
    ACTIVE_HUMAN,
    DISCONNECTED
}

data class CallSessionState(
    val phase: CallPhase = CallPhase.IDLE,
    val callerName: String = "",
    val callerNumber: String = "",
    val isOutgoing: Boolean = false,
    val isAcousticTest: Boolean = false,
    val autoAnswerArmed: Boolean = false,
    val currentRing: Int = 0,
    val targetRings: Int = 3,
    val recordingSeconds: Int = 0,
    val maxRecordingSeconds: Int = 60,
    val liveAmplitude: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val callDurationSeconds: Int = 0
)

class CallSessionManager(
    private val context: Context,
    private val repository: EchoDeckRepository,
    private val preferencesRepository: MachinePreferencesRepository,
    private val playerController: AudioPlayerController,
    private val recorderController: AudioRecorderController,
    private val ttsSpeaker: TtsSpeaker
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _sessionState = MutableStateFlow(CallSessionState())
    val sessionState: StateFlow<CallSessionState> = _sessionState.asStateFlow()

    private var activeBridge: CallControlBridge? = null
    private var testJob: Job? = null
    private var amplitudeCollectorJob: Job? = null

    interface CallControlBridge {
        fun answerNormally()
        fun sendToAnsweringMachineNow()
        fun declineOrHangUp()
        fun toggleMute()
        fun toggleSpeaker()
        fun sendDtmf(digit: Char)
    }

    fun attachBridge(bridge: CallControlBridge?) {
        activeBridge = bridge
    }

    fun updateState(transform: (CallSessionState) -> CallSessionState) {
        _sessionState.value = transform(_sessionState.value)
    }

    fun resetToIdle() {
        amplitudeCollectorJob?.cancel()
        _sessionState.value = CallSessionState()
    }

    fun answerNormally() {
        activeBridge?.answerNormally()
    }

    fun sendToAnsweringMachineNow() {
        activeBridge?.sendToAnsweringMachineNow()
    }

    fun declineOrHangUp() {
        if (_sessionState.value.isAcousticTest) {
            stopAcousticSelfTest()
        } else {
            activeBridge?.declineOrHangUp()
        }
    }

    fun toggleMute() {
        activeBridge?.toggleMute()
    }

    fun toggleSpeaker() {
        activeBridge?.toggleSpeaker()
    }

    fun sendDtmf(digit: Char) {
        activeBridge?.sendDtmf(digit)
    }

    /**
     * Runs the real on-device answering machine audio pipeline (Greeting -> Beep -> Mic Recording -> Room Save)
     * as an acoustic dry-run test so users can verify microphone capture and greeting playback directly.
     */
    fun startAcousticSelfTest() {
        if (_sessionState.value.phase != CallPhase.IDLE) return

        testJob?.cancel()
        testJob = scope.launch {
            val settings = preferencesRepository.getSettingsOnce()
            val activeGreeting = repository.getActiveGreetingOnce()

            _sessionState.value = CallSessionState(
                phase = CallPhase.GREETING_PLAYING,
                callerName = "Acoustic Hardware Test",
                callerNumber = "Local Loopback",
                isAcousticTest = true,
                maxRecordingSeconds = 12
            )

            if (activeGreeting != null) {
                when (activeGreeting.type) {
                    GreetingType.RECORDED -> {
                        if (activeGreeting.audioFilePath.isNotBlank()) {
                            playerController.playFileAndAwait(activeGreeting.audioFilePath, forVoiceCall = false)
                        } else {
                            ttsSpeaker.speakAndAwait("Acoustic check. Leave a test message after the tone.", forVoiceCall = false)
                        }
                    }
                    GreetingType.TTS -> {
                        val script = activeGreeting.ttsScript.ifBlank {
                            "Hello, you have reached EchoDeck. Leave a message after the tone."
                        }
                        ttsSpeaker.speakAndAwait(script, forVoiceCall = false)
                    }
                }
            }

            if (!isActive) return@launch

            if (settings.playBeepAfterGreeting) {
                _sessionState.value = _sessionState.value.copy(phase = CallPhase.BEEP_PLAYING)
                BeepGenerator.playAnsweringBeep(forVoiceCall = false)
            }

            if (!isActive) return@launch

            val outFile = repository.createNewMessageFile(prefix = "test")
            val started = recorderController.startRecording(outFile)
            if (!started) {
                resetToIdle()
                return@launch
            }

            _sessionState.value = _sessionState.value.copy(
                phase = CallPhase.RECORDING_MESSAGE,
                recordingSeconds = 0,
                maxRecordingSeconds = 12
            )

            amplitudeCollectorJob?.cancel()
            amplitudeCollectorJob = launch {
                recorderController.amplitudeLevel.collect { amp ->
                    _sessionState.value = _sessionState.value.copy(liveAmplitude = amp)
                }
            }

            for (sec in 1..12) {
                if (!isActive || _sessionState.value.phase != CallPhase.RECORDING_MESSAGE) break
                delay(1000L)
                _sessionState.value = _sessionState.value.copy(recordingSeconds = sec)
            }

            finalizeAcousticTestRecording()
        }
    }

    fun stopAcousticSelfTest() {
        testJob?.cancel()
        testJob = null
        playerController.stop()
        ttsSpeaker.stop()
        if (_sessionState.value.phase == CallPhase.RECORDING_MESSAGE) {
            scope.launch { finalizeAcousticTestRecording() }
        } else {
            recorderController.cancelRecording()
            resetToIdle()
        }
    }

    private suspend fun finalizeAcousticTestRecording() {
        amplitudeCollectorJob?.cancel()
        val result = recorderController.stopRecording()
        if (result != null) {
            val savedId = repository.saveMessage(
                VoicemailMessageEntity(
                    callerName = "Acoustic Hardware Test",
                    callerNumber = "Local Mic Check",
                    timestampMillis = System.currentTimeMillis(),
                    durationSeconds = result.durationSeconds,
                    audioFilePath = result.filePath,
                    isRead = false,
                    handledByMachine = true,
                    isAcousticTest = true,
                    notes = "Recorded during local answering machine acoustic test.",
                    waveformPeaks = result.waveformPeaksCsv
                )
            )
            NotificationHelper.showNewVoicemailNotification(
                context = context,
                messageId = savedId,
                callerDisplay = "Acoustic Hardware Test",
                durationSeconds = result.durationSeconds
            )
        }
        resetToIdle()
    }
}
