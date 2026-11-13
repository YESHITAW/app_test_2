package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

data class PlaybackState(
    val activeKey: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val useSpeakerphone: Boolean = true
)

data class RecordedAudioResult(
    val filePath: String,
    val durationSeconds: Int,
    val waveformPeaksCsv: String
)

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun togglePlayPause(key: String, filePath: String, useSpeakerphone: Boolean = _playbackState.value.useSpeakerphone) {
        val current = _playbackState.value
        val player = mediaPlayer
        if (current.activeKey == key && player != null) {
            if (current.isPlaying) {
                runCatching { player.pause() }
                _playbackState.value = current.copy(isPlaying = false)
                progressJob?.cancel()
            } else {
                applyAudioRouting(useSpeakerphone)
                runCatching { player.start() }
                _playbackState.value = current.copy(isPlaying = true, useSpeakerphone = useSpeakerphone)
                startProgressLoop()
            }
            return
        }
        playFile(key, filePath, useSpeakerphone)
    }

    fun playFile(key: String, filePath: String, useSpeakerphone: Boolean = _playbackState.value.useSpeakerphone) {
        stop()
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return

        applyAudioRouting(useSpeakerphone)
        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (useSpeakerphone) AudioAttributes.USAGE_MEDIA else AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        runCatching {
            player.setDataSource(file.absolutePath)
            player.prepare()
            val totalMs = player.duration.coerceAtLeast(1)
            player.setOnCompletionListener {
                progressJob?.cancel()
                _playbackState.value = PlaybackState(useSpeakerphone = _playbackState.value.useSpeakerphone)
                releasePlayer()
            }
            player.start()
            mediaPlayer = player
            _playbackState.value = PlaybackState(
                activeKey = key,
                isPlaying = true,
                positionMs = 0,
                durationMs = totalMs,
                useSpeakerphone = useSpeakerphone
            )
            startProgressLoop()
        }.onFailure {
            player.release()
        }
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0, _playbackState.value.durationMs.coerceAtLeast(0))
        runCatching {
            player.seekTo(clamped)
            _playbackState.value = _playbackState.value.copy(positionMs = clamped)
        }
    }

    fun setSpeakerphone(enabled: Boolean) {
        _playbackState.value = _playbackState.value.copy(useSpeakerphone = enabled)
        applyAudioRouting(enabled)
    }

    @Suppress("DEPRECATION")
    private fun applyAudioRouting(speakerphone: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            audioManager.mode = if (speakerphone) AudioManager.MODE_NORMAL else AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = speakerphone
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val p = mediaPlayer
                if (p != null && _playbackState.value.isPlaying) {
                    val pos = runCatching { p.currentPosition }.getOrDefault(_playbackState.value.positionMs)
                    _playbackState.value = _playbackState.value.copy(positionMs = pos)
                }
                delay(120L)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        releasePlayer()
        _playbackState.value = PlaybackState(useSpeakerphone = _playbackState.value.useSpeakerphone)
    }

    private fun releasePlayer() {
        mediaPlayer?.let { p ->
            runCatching {
                if (p.isPlaying) p.stop()
                p.reset()
                p.release()
            }
        }
        mediaPlayer = null
    }

    suspend fun playFileAndAwait(filePath: String, forVoiceCall: Boolean): Boolean =
        withContext(Dispatchers.Main) {
            val file = File(filePath)
            if (!file.exists() || file.length() == 0L) return@withContext false

            suspendCancellableCoroutine { cont ->
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(
                                if (forVoiceCall) AudioAttributes.USAGE_VOICE_COMMUNICATION
                                else AudioAttributes.USAGE_MEDIA
                            )
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                }
                cont.invokeOnCancellation {
                    runCatching {
                        if (player.isPlaying) player.stop()
                        player.release()
                    }
                }
                runCatching {
                    player.setDataSource(file.absolutePath)
                    player.prepare()
                    player.setOnCompletionListener {
                        runCatching { player.release() }
                        if (cont.isActive) cont.resume(true)
                    }
                    player.setOnErrorListener { _, _, _ ->
                        runCatching { player.release() }
                        if (cont.isActive) cont.resume(false)
                        true
                    }
                    player.start()
                }.onFailure {
                    runCatching { player.release() }
                    if (cont.isActive) cont.resume(false)
                }
            }
        }
}

class AudioRecorderController(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recorder: MediaRecorder? = null
    private var amplitudeJob: Job? = null
    private var currentFile: File? = null
    private var startedAtMillis: Long = 0L
    private val rawSamples = mutableListOf<Int>()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _amplitudeLevel = MutableStateFlow(0)
    val amplitudeLevel: StateFlow<Int> = _amplitudeLevel.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    fun startRecording(outputFile: File, audioSource: Int = MediaRecorder.AudioSource.MIC): Boolean {
        stopRecordingInternal(discard = true)
        outputFile.parentFile?.mkdirs()
        rawSamples.clear()

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val started = runCatching {
            mr.setAudioSource(audioSource)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(128000)
            mr.setOutputFile(outputFile.absolutePath)
            mr.prepare()
            mr.start()
            true
        }.getOrDefault(false)

        if (!started) {
            runCatching { mr.release() }
            return false
        }

        recorder = mr
        currentFile = outputFile
        startedAtMillis = System.currentTimeMillis()
        _isRecording.value = true
        _elapsedSeconds.value = 0
        _amplitudeLevel.value = 0

        amplitudeJob = scope.launch {
            while (isActive && _isRecording.value) {
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                val normalized = ((amp / 32767f) * 100f).roundToInt().coerceIn(4, 100)
                _amplitudeLevel.value = normalized
                synchronized(rawSamples) {
                    rawSamples.add(normalized)
                }
                val secs = ((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt()
                _elapsedSeconds.value = secs
                delay(80L)
            }
        }
        return true
    }

    fun stopRecording(): RecordedAudioResult? = stopRecordingInternal(discard = false)

    fun cancelRecording() {
        stopRecordingInternal(discard = true)
    }

    private fun stopRecordingInternal(discard: Boolean): RecordedAudioResult? {
        amplitudeJob?.cancel()
        amplitudeJob = null
        val mr = recorder
        val file = currentFile
        recorder = null
        currentFile = null

        val durationSecs = (((System.currentTimeMillis() - startedAtMillis) / 1000L).toInt()).coerceAtLeast(1)
        _isRecording.value = false
        _amplitudeLevel.value = 0
        _elapsedSeconds.value = 0

        if (mr != null) {
            runCatching { mr.stop() }
            runCatching { mr.reset() }
            runCatching { mr.release() }
        }

        if (discard || file == null || !file.exists() || file.length() == 0L) {
            file?.let { runCatching { it.delete() } }
            return null
        }

        val snapshot = synchronized(rawSamples) { rawSamples.toList() }
        val peaksCsv = downsamplePeaks(snapshot, targetBuckets = 36).joinToString(",")
        return RecordedAudioResult(
            filePath = file.absolutePath,
            durationSeconds = durationSecs,
            waveformPeaksCsv = peaksCsv
        )
    }

    private fun downsamplePeaks(samples: List<Int>, targetBuckets: Int): List<Int> {
        if (samples.isEmpty()) return List(targetBuckets) { 18 }
        if (samples.size <= targetBuckets) return samples
        val step = samples.size.toFloat() / targetBuckets.toFloat()
        return (0 until targetBuckets).map { i ->
            val start = (i * step).toInt().coerceIn(0, samples.lastIndex)
            val end = (((i + 1) * step).toInt()).coerceIn(start + 1, samples.size)
            samples.subList(start, end).maxOrNull() ?: 12
        }
    }
}

object BeepGenerator {
    suspend fun playAnsweringBeep(forVoiceCall: Boolean = false) = withContext(Dispatchers.IO) {
        val sampleRate = 22050
        val durationMs = 450
        val numSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(numSamples)
        val freqHz = 980.0
        val fadeSamples = (sampleRate * 20) / 1000

        for (i in 0 until numSamples) {
            val angle = 2.0 * PI * i * freqHz / sampleRate
            var envelope = 1.0
            if (i < fadeSamples) {
                envelope = i.toDouble() / fadeSamples
            } else if (i > numSamples - fadeSamples) {
                envelope = (numSamples - i).toDouble() / fadeSamples
            }
            buffer[i] = (sin(angle) * envelope * Short.MAX_VALUE * 0.65).toInt().toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (forVoiceCall) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        runCatching {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            delay(durationMs.toLong() + 60L)
            audioTrack.stop()
        }
        runCatching { audioTrack.release() }
    }
}

class TtsSpeaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    private suspend fun ensureInitialized(): TextToSpeech? = withContext(Dispatchers.Main) {
        if (tts != null && isReady) return@withContext tts
        suspendCancellableCoroutine { cont ->
            var instance: TextToSpeech? = null
            instance = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    instance?.language = Locale.getDefault()
                    tts = instance
                    isReady = true
                    if (cont.isActive) cont.resume(instance)
                } else {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    suspend fun speakAndAwait(text: String, forVoiceCall: Boolean = false): Boolean =
        withContext(Dispatchers.Main) {
            val engine = ensureInitialized() ?: return@withContext false
            suspendCancellableCoroutine { cont ->
                val utteranceId = UUID.randomUUID().toString()
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (forVoiceCall) AudioAttributes.USAGE_VOICE_COMMUNICATION
                            else AudioAttributes.USAGE_MEDIA
                        )
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        if (id == utteranceId && cont.isActive) cont.resume(true)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId && cont.isActive) cont.resume(false)
                    }

                    override fun onError(id: String?, errorCode: Int) {
                        if (id == utteranceId && cont.isActive) cont.resume(false)
                    }
                })
                cont.invokeOnCancellation {
                    runCatching { engine.stop() }
                }
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                if (result != TextToSpeech.SUCCESS && cont.isActive) {
                    cont.resume(false)
                }
            }
        }

    fun stop() {
        runCatching { tts?.stop() }
    }
}
