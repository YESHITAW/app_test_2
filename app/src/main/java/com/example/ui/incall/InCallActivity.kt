package com.example.ui.incall

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.EchoDeckApplication
import com.example.telecom.CallPhase
import com.example.telecom.CallSessionState
import com.example.ui.components.CassetteReelDeckVisual
import com.example.ui.components.LiveVuMeter
import com.example.ui.components.formatDuration
import com.example.ui.theme.EchoDeckTheme
import com.example.ui.theme.EchoMonoLarge
import com.example.ui.theme.EchoMonoMedium

class InCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()

        val sessionManager = (applicationContext as EchoDeckApplication).callSessionManager

        setContent {
            EchoDeckTheme(darkTheme = true) {
                val state by sessionManager.sessionState.collectAsStateWithLifecycle()

                LaunchedEffect(state.phase) {
                    if (state.phase == CallPhase.IDLE) {
                        finish()
                    }
                }

                InCallHudScreen(
                    state = state,
                    onAnswer = { sessionManager.answerNormally() },
                    onSendToMachine = { sessionManager.sendToAnsweringMachineNow() },
                    onDeclineOrHangUp = { sessionManager.declineOrHangUp() },
                    onToggleMute = { sessionManager.toggleMute() },
                    onToggleSpeaker = { sessionManager.toggleSpeaker() },
                    onSendDtmf = { sessionManager.sendDtmf(it) }
                )
            }
        }
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}

@Composable
private fun InCallHudScreen(
    state: CallSessionState,
    onAnswer: () -> Unit,
    onSendToMachine: () -> Unit,
    onDeclineOrHangUp: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendDtmf: (Char) -> Unit
) {
    var showDtmfPad by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Caller Info & Phase Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (state.phase) {
                            CallPhase.RECORDING_MESSAGE, CallPhase.GREETING_PLAYING, CallPhase.BEEP_PLAYING ->
                                MaterialTheme.colorScheme.primaryContainer
                            CallPhase.ACTIVE_HUMAN -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Text(
                            text = when (state.phase) {
                                CallPhase.RINGING -> "INCOMING CALL"
                                CallPhase.DIALING -> "DIALING..."
                                CallPhase.GREETING_PLAYING -> "MACHINE • PLAYING GREETING"
                                CallPhase.BEEP_PLAYING -> "MACHINE • TONE"
                                CallPhase.RECORDING_MESSAGE -> "MACHINE • RECORDING"
                                CallPhase.ACTIVE_HUMAN -> "CONNECTED"
                                CallPhase.DISCONNECTED -> "CALL ENDED"
                                CallPhase.IDLE -> "STANDBY"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.callerName.ifBlank { state.callerNumber },
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = state.callerNumber,
                        style = EchoMonoMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Center Telemetry Card (Ring Countdown or Machine Deck or Human Timer)
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (state.phase) {
                            CallPhase.RINGING -> {
                                if (state.autoAnswerArmed && state.targetRings > 0) {
                                    Text(
                                        text = "Auto-answering on Ring ${state.currentRing} of ${state.targetRings}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LinearProgressIndicator(
                                        progress = {
                                            (state.currentRing.toFloat() / state.targetRings.toFloat()).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Text(
                                        text = "Answering Machine in Standby",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            CallPhase.GREETING_PLAYING, CallPhase.BEEP_PLAYING, CallPhase.RECORDING_MESSAGE -> {
                                CassetteReelDeckVisual(
                                    isSpinning = true,
                                    accentColor = if (state.phase == CallPhase.RECORDING_MESSAGE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.secondary
                                    }
                                )
                                if (state.phase == CallPhase.RECORDING_MESSAGE) {
                                    Text(
                                        text = "${formatDuration(state.recordingSeconds)} / ${formatDuration(state.maxRecordingSeconds)}",
                                        style = EchoMonoLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    LiveVuMeter(amplitude = state.liveAmplitude)
                                } else {
                                    Text(
                                        text = "Playing outgoing greeting to caller...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            CallPhase.ACTIVE_HUMAN -> {
                                Text(
                                    text = formatDuration(state.callDurationSeconds),
                                    style = EchoMonoLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                if (showDtmfPad) {
                                    val pad = listOf(
                                        listOf('1', '2', '3'),
                                        listOf('4', '5', '6'),
                                        listOf('7', '8', '9'),
                                        listOf('*', '0', '#')
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        pad.forEach { r ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                r.forEach { d ->
                                                    Surface(
                                                        onClick = { onSendDtmf(d) },
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        modifier = Modifier.size(56.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = d.toString(),
                                                                style = MaterialTheme.typography.titleLarge,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    text = if (state.isOutgoing) "Connecting call..." else "Closing line...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Bottom Call Action Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state.phase) {
                        CallPhase.RINGING -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onDeclineOrHangUp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("incall_decline_button")
                                ) {
                                    Icon(Icons.Filled.CallEnd, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Decline")
                                }
                                Button(
                                    onClick = onAnswer,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(58.dp)
                                        .testTag("incall_answer_button")
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Answer")
                                }
                            }
                            Button(
                                onClick = onSendToMachine,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("incall_send_machine_button")
                            ) {
                                Icon(Icons.Filled.Voicemail, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Send to Answering Machine Now")
                            }
                        }
                        CallPhase.GREETING_PLAYING, CallPhase.BEEP_PLAYING, CallPhase.RECORDING_MESSAGE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onAnswer,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Take Over")
                                }
                                Button(
                                    onClick = onDeclineOrHangUp,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                ) {
                                    Icon(Icons.Filled.CallEnd, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("End & Save")
                                }
                            }
                        }
                        else -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(
                                    onClick = onToggleMute,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.MicOff,
                                        contentDescription = "Mute microphone",
                                        tint = if (state.isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = onToggleSpeaker,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.VolumeUp,
                                        contentDescription = "Speakerphone",
                                        tint = if (state.isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { showDtmfPad = !showDtmfPad },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Dialpad,
                                        contentDescription = "Toggle DTMF keypad",
                                        tint = if (showDtmfPad) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Button(
                                onClick = onDeclineOrHangUp,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = CircleShape,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                            ) {
                                Icon(Icons.Filled.CallEnd, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Hang Up")
                            }
                        }
                    }
                }
            }
        }
    }
}
