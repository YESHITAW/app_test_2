package com.example.telecom

import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import com.example.EchoDeckApplication
import com.example.audio.BeepGenerator
import com.example.data.local.GreetingType
import com.example.data.local.ScreeningAction
import com.example.data.local.VoicemailMessageEntity
import com.example.ui.incall.InCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EchoInCallService : InCallService(), CallSessionManager.CallControlBridge {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var currentCall: Call? = null
    private var autoAnswerJob: Job? = null
    private var machineSequenceJob: Job? = null
    private var humanTimerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var isAnsweringMachineActive = false
    private var isMutedState = false

    private val app: EchoDeckApplication
        get() = applicationContext as EchoDeckApplication

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            handleCallState(call, state)
        }
    }

    override fun onCreate() {
        super.onCreate()
        app.callSessionManager.attachBridge(this)
    }

    override fun onDestroy() {
        cleanupActiveJobs()
        app.callSessionManager.attachBridge(null)
        app.callSessionManager.resetToIdle()
        NotificationHelper.cancelCallNotification(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        isAnsweringMachineActive = false
        call.registerCallback(callCallback)

        val number = call.details.handle?.schemeSpecificPart.orEmpty().ifBlank { "Private Number" }
        val displayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            call.details.contactDisplayName?.toString()
                ?: call.details.callerDisplayName?.toString()
                ?: number
        } else {
            call.details.callerDisplayName?.toString() ?: number
        }

        val initialState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }

        val isOutgoing = initialState == Call.STATE_DIALING || initialState == Call.STATE_CONNECTING
        app.callSessionManager.updateState {
            CallSessionState(
                phase = if (isOutgoing) CallPhase.DIALING else CallPhase.RINGING,
                callerName = displayName,
                callerNumber = number,
                isOutgoing = isOutgoing
            )
        }

        if (!isOutgoing) {
            NotificationHelper.showIncomingCallNotification(this, displayName, number)
            launchInCallUi()
            evaluateIncomingAutoAnswer(call, number)
        } else {
            launchInCallUi()
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            finalizeMachineRecordingIfNeeded()
            cleanupActiveJobs()
            currentCall = null
            NotificationHelper.cancelCallNotification(this)
            app.callSessionManager.resetToIdle()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        isMutedState = audioState.isMuted
        val isSpeaker = (audioState.route and CallAudioState.ROUTE_SPEAKER) != 0
        app.callSessionManager.updateState {
            it.copy(isMuted = isMutedState, isSpeakerOn = isSpeaker)
        }
    }

    private fun handleCallState(call: Call, state: Int) {
        when (state) {
            Call.STATE_ACTIVE -> {
                if (isAnsweringMachineActive) {
                    beginMachineGreetingAndRecord(call)
                } else {
                    autoAnswerJob?.cancel()
                    NotificationHelper.cancelCallNotification(this)
                    app.callSessionManager.updateState {
                        it.copy(phase = CallPhase.ACTIVE_HUMAN, callDurationSeconds = 0)
                    }
                    startHumanDurationTimer()
                }
            }
            Call.STATE_DISCONNECTED -> {
                finalizeMachineRecordingIfNeeded()
                cleanupActiveJobs()
                NotificationHelper.cancelCallNotification(this)
                app.callSessionManager.updateState { it.copy(phase = CallPhase.DISCONNECTED) }
                serviceScope.launch {
                    delay(900L)
                    if (currentCall == null || currentCall == call) {
                        app.callSessionManager.resetToIdle()
                    }
                }
            }
        }
    }

    private fun evaluateIncomingAutoAnswer(call: Call, callerNumber: String) {
        autoAnswerJob?.cancel()
        autoAnswerJob = serviceScope.launch {
            val settings = app.preferencesRepository.getSettingsOnce()
            val rules = app.repository.getScreeningRulesOnce()
            val normalizedIncoming = callerNumber.filter { it.isDigit() }
            val matchedRule = rules.firstOrNull { rule ->
                val patternDigits = rule.phoneNumberPattern.filter { it.isDigit() }
                patternDigits.isNotEmpty() && normalizedIncoming.endsWith(patternDigits)
            }

            if (matchedRule?.action == ScreeningAction.DIRECT_TO_MACHINE) {
                app.callSessionManager.updateState {
                    it.copy(autoAnswerArmed = true, currentRing = 1, targetRings = 1)
                }
                delay(1200L)
                triggerMachinePickup(call)
                return@launch
            }

            if (matchedRule?.action == ScreeningAction.ALLOW || !settings.isMachineEnabled) {
                app.callSessionManager.updateState {
                    it.copy(autoAnswerArmed = false, currentRing = 0, targetRings = settings.ringsBeforeAnswer)
                }
                return@launch
            }

            val targetRings = settings.ringsBeforeAnswer.coerceIn(1, 8)
            app.callSessionManager.updateState {
                it.copy(autoAnswerArmed = true, currentRing = 0, targetRings = targetRings)
            }

            for (ring in 1..targetRings) {
                if (!isActive || isAnsweringMachineActive) return@launch
                app.callSessionManager.updateState { it.copy(currentRing = ring) }
                delay(4000L)
            }

            if (isActive && !isAnsweringMachineActive) {
                triggerMachinePickup(call)
            }
        }
    }

    private fun triggerMachinePickup(call: Call) {
        isAnsweringMachineActive = true
        autoAnswerJob?.cancel()
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    private fun beginMachineGreetingAndRecord(call: Call) {
        machineSequenceJob?.cancel()
        machineSequenceJob = serviceScope.launch {
            val settings = app.preferencesRepository.getSettingsOnce()
            val activeGreeting = app.repository.getActiveGreetingOnce()
            val session = app.callSessionManager.sessionState.value

            @Suppress("DEPRECATION")
            setAudioRoute(
                if (settings.speakerphoneCapture) CallAudioState.ROUTE_SPEAKER
                else CallAudioState.ROUTE_EARPIECE
            )

            NotificationHelper.showAnsweringMachineActiveNotification(
                context = this@EchoInCallService,
                callerDisplay = session.callerName,
                stageLabel = "Playing answering machine greeting..."
            )
            app.callSessionManager.updateState {
                it.copy(
                    phase = CallPhase.GREETING_PLAYING,
                    maxRecordingSeconds = settings.maxRecordingSeconds
                )
            }

            if (activeGreeting != null) {
                when (activeGreeting.type) {
                    GreetingType.RECORDED -> {
                        if (activeGreeting.audioFilePath.isNotBlank()) {
                            app.playerController.playFileAndAwait(activeGreeting.audioFilePath, forVoiceCall = true)
                        } else {
                            app.ttsSpeaker.speakAndAwait(
                                "Hello, please leave a message after the tone.",
                                forVoiceCall = true
                            )
                        }
                    }
                    GreetingType.TTS -> {
                        val script = activeGreeting.ttsScript.ifBlank {
                            "Hello, you have reached EchoDeck. Please leave a message after the tone."
                        }
                        app.ttsSpeaker.speakAndAwait(script, forVoiceCall = true)
                    }
                }
            }

            if (!isActive) return@launch

            if (settings.playBeepAfterGreeting) {
                app.callSessionManager.updateState { it.copy(phase = CallPhase.BEEP_PLAYING) }
                BeepGenerator.playAnsweringBeep(forVoiceCall = true)
            }

            if (!isActive) return@launch

            val messageFile = app.repository.createNewMessageFile(prefix = "msg")
            val started = app.recorderController.startRecording(
                outputFile = messageFile,
                audioSource = MediaRecorder.AudioSource.MIC
            )

            if (!started) {
                call.disconnect()
                return@launch
            }

            NotificationHelper.showAnsweringMachineActiveNotification(
                context = this@EchoInCallService,
                callerDisplay = session.callerName,
                stageLabel = "Recording caller message..."
            )
            app.callSessionManager.updateState {
                it.copy(
                    phase = CallPhase.RECORDING_MESSAGE,
                    recordingSeconds = 0,
                    maxRecordingSeconds = settings.maxRecordingSeconds
                )
            }

            amplitudeJob?.cancel()
            amplitudeJob = launch {
                app.recorderController.amplitudeLevel.collect { level ->
                    app.callSessionManager.updateState { it.copy(liveAmplitude = level) }
                }
            }

            for (sec in 1..settings.maxRecordingSeconds) {
                if (!isActive || app.callSessionManager.sessionState.value.phase != CallPhase.RECORDING_MESSAGE) break
                delay(1000L)
                app.callSessionManager.updateState { it.copy(recordingSeconds = sec) }
            }

            finalizeMachineRecordingIfNeeded()
            runCatching { call.disconnect() }
        }
    }

    private fun finalizeMachineRecordingIfNeeded() {
        if (!app.recorderController.isRecording.value) return
        amplitudeJob?.cancel()
        val result = app.recorderController.stopRecording() ?: return
        val session = app.callSessionManager.sessionState.value

        serviceScope.launch {
            val id = app.repository.saveMessage(
                VoicemailMessageEntity(
                    callerName = session.callerName.ifBlank { session.callerNumber },
                    callerNumber = session.callerNumber.ifBlank { "Unknown" },
                    timestampMillis = System.currentTimeMillis(),
                    durationSeconds = result.durationSeconds,
                    audioFilePath = result.filePath,
                    isRead = false,
                    handledByMachine = true,
                    isAcousticTest = false,
                    waveformPeaks = result.waveformPeaksCsv
                )
            )
            NotificationHelper.showNewVoicemailNotification(
                context = this@EchoInCallService,
                messageId = id,
                callerDisplay = session.callerName.ifBlank { session.callerNumber },
                durationSeconds = result.durationSeconds
            )
        }
    }

    private fun startHumanDurationTimer() {
        humanTimerJob?.cancel()
        humanTimerJob = serviceScope.launch {
            var secs = 0
            while (isActive) {
                delay(1000L)
                secs++
                app.callSessionManager.updateState { it.copy(callDurationSeconds = secs) }
            }
        }
    }

    private fun cleanupActiveJobs() {
        autoAnswerJob?.cancel()
        machineSequenceJob?.cancel()
        humanTimerJob?.cancel()
        amplitudeJob?.cancel()
        app.playerController.stop()
        app.ttsSpeaker.stop()
    }

    private fun launchInCallUi() {
        runCatching {
            val intent = Intent(this, InCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    override fun answerNormally() {
        val call = currentCall ?: return
        isAnsweringMachineActive = false
        autoAnswerJob?.cancel()
        machineSequenceJob?.cancel()
        app.playerController.stop()
        app.ttsSpeaker.stop()
        if (app.recorderController.isRecording.value) {
            finalizeMachineRecordingIfNeeded()
        }
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        if (state == Call.STATE_RINGING) {
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        } else {
            app.callSessionManager.updateState { it.copy(phase = CallPhase.ACTIVE_HUMAN) }
            startHumanDurationTimer()
        }
    }

    override fun sendToAnsweringMachineNow() {
        val call = currentCall ?: return
        if (isAnsweringMachineActive) return
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        if (state == Call.STATE_RINGING) {
            triggerMachinePickup(call)
        } else if (state == Call.STATE_ACTIVE) {
            isAnsweringMachineActive = true
            beginMachineGreetingAndRecord(call)
        }
    }

    override fun declineOrHangUp() {
        val call = currentCall ?: return
        finalizeMachineRecordingIfNeeded()
        cleanupActiveJobs()
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            call.details.state
        } else {
            @Suppress("DEPRECATION")
            call.state
        }
        if (state == Call.STATE_RINGING) {
            call.reject(false, null)
        } else {
            call.disconnect()
        }
    }

    override fun toggleMute() {
        val next = !isMutedState
        setMuted(next)
        isMutedState = next
        app.callSessionManager.updateState { it.copy(isMuted = next) }
    }

    @Suppress("DEPRECATION")
    override fun toggleSpeaker() {
        val next = !app.callSessionManager.sessionState.value.isSpeakerOn
        setAudioRoute(if (next) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        app.callSessionManager.updateState { it.copy(isSpeakerOn = next) }
    }

    override fun sendDtmf(digit: Char) {
        val call = currentCall ?: return
        runCatching {
            call.playDtmfTone(digit)
            serviceScope.launch {
                delay(180L)
                call.stopDtmfTone()
            }
        }
    }
}
