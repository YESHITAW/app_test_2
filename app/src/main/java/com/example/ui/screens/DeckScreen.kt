package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.telecom.CallPhase
import com.example.telecom.SystemCapabilityHelper
import com.example.ui.EchoDeckUiState
import com.example.ui.components.CassetteReelDeckVisual
import com.example.ui.components.LiveVuMeter
import com.example.ui.components.formatDuration
import com.example.ui.theme.EchoMonoLarge
import com.example.ui.theme.EchoMonoMedium
import com.example.ui.theme.EchoMonoSmall
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeckScreen(
    uiState: EchoDeckUiState,
    onRefreshReadiness: () -> Unit,
    onSetMachineEnabled: (Boolean) -> Unit,
    onSetRingsBeforeAnswer: (Int) -> Unit,
    onSetMaxRecordingSeconds: (Int) -> Unit,
    onSetSpeakerphoneCapture: (Boolean) -> Unit,
    onSetPlayBeepAfterGreeting: (Boolean) -> Unit,
    onStartAcousticTest: () -> Unit,
    onStopAcousticTest: () -> Unit,
    onOpenGreetings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshReadiness()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        onRefreshReadiness()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshReadiness()
    }

    val audioPermForTestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onRefreshReadiness()
        if (granted) onStartAcousticTest()
    }

    val session = uiState.callSession
    val isTestingOrCalling = session.phase != CallPhase.IDLE

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Studio Hero Deck Console
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_deck_hero),
                                contentDescription = "EchoDeck desktop answering machine console",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.78f)
                                            )
                                        )
                                    )
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (uiState.settings.isMachineEnabled)
                                                        MaterialTheme.colorScheme.secondary
                                                    else MaterialTheme.colorScheme.outline
                                                )
                                        )
                                        Text(
                                            text = if (uiState.settings.isMachineEnabled) "MACHINE ARMED" else "STANDBY MODE",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "EchoDeck",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (uiState.settings.isMachineEnabled) {
                                            "Auto-answers after ${uiState.settings.ringsBeforeAnswer} rings"
                                        } else {
                                            "Incoming calls ring until answered manually"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = uiState.settings.isMachineEnabled,
                                    onCheckedChange = onSetMachineEnabled,
                                    modifier = Modifier.testTag("machine_armed_switch")
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CassetteReelDeckVisual(
                                isSpinning = uiState.settings.isMachineEnabled && (isTestingOrCalling || uiState.playbackState.isPlaying),
                                accentColor = if (session.phase == CallPhase.RECORDING_MESSAGE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ACTIVE GREETING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = uiState.activeGreeting?.title ?: "Standard Studio Greeting",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onOpenGreetings,
                                    modifier = Modifier.testTag("change_greeting_button")
                                ) {
                                    Text("Change")
                                }
                            }
                        }
                    }
                }
            }

            // 2. Live Acoustic Sequence Test / Active Call Banner
            item {
                AnimatedVisibility(visible = isTestingOrCalling) {
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RadioButtonChecked,
                                        contentDescription = "Active recording status",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = when (session.phase) {
                                            CallPhase.GREETING_PLAYING -> "Playing Active Greeting..."
                                            CallPhase.BEEP_PLAYING -> "Playing 980Hz Tone..."
                                            CallPhase.RECORDING_MESSAGE -> "Recording Caller Audio"
                                            CallPhase.RINGING -> "Incoming Call • Ring ${session.currentRing}/${session.targetRings}"
                                            else -> "Call Active"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = formatDuration(session.recordingSeconds),
                                    style = EchoMonoLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (session.phase == CallPhase.RECORDING_MESSAGE) {
                                LiveVuMeter(amplitude = session.liveAmplitude)
                            }

                            Button(
                                onClick = onStopAcousticTest,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stop_test_button")
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Stop & Save to Inbox")
                            }
                        }
                    }
                }
            }

            // 3. System Roles & Permissions Readiness
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Telephony Readiness",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = if (uiState.readiness.allReady) {
                                        "Ready to auto-answer incoming calls"
                                    } else {
                                        "Grant required roles & permissions for full call control"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (uiState.readiness.allReady) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.tertiaryContainer
                                }
                            ) {
                                Text(
                                    text = if (uiState.readiness.allReady) "READY" else "SETUP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.readiness.allReady) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        ReadinessRow(
                            icon = Icons.Filled.PhoneCallback,
                            title = "Default Phone / Dialer App",
                            subtitle = "Required for InCallService auto-answer & call UI",
                            isReady = uiState.readiness.isDefaultDialer,
                            actionLabel = "Set Default",
                            testTag = "grant_dialer_role_button",
                            onAction = {
                                SystemCapabilityHelper.createRequestDefaultDialerIntent(context)?.let {
                                    roleLauncher.launch(it)
                                }
                            }
                        )

                        ReadinessRow(
                            icon = Icons.Filled.Security,
                            title = "Call Screening Service",
                            subtitle = "Enables local spam blocking & VIP routing",
                            isReady = uiState.readiness.isCallScreeningApp,
                            actionLabel = "Enable",
                            testTag = "grant_screening_role_button",
                            onAction = {
                                SystemCapabilityHelper.createRequestCallScreeningIntent(context)?.let {
                                    roleLauncher.launch(it)
                                }
                            }
                        )

                        val missingPerms = buildList {
                            if (!uiState.readiness.hasRecordAudio) add(Manifest.permission.RECORD_AUDIO)
                            if (!uiState.readiness.hasPhonePermissions) {
                                add(Manifest.permission.READ_PHONE_STATE)
                                add(Manifest.permission.CALL_PHONE)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    add(Manifest.permission.ANSWER_PHONE_CALLS)
                                }
                            }
                            if (!uiState.readiness.hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        ReadinessRow(
                            icon = Icons.Filled.Mic,
                            title = "Microphone, Phone & Alerts",
                            subtitle = "Audio capture, call state, and voicemail alerts",
                            isReady = missingPerms.isEmpty(),
                            actionLabel = "Grant All",
                            testTag = "grant_permissions_button",
                            onAction = {
                                if (missingPerms.isNotEmpty()) {
                                    permissionsLauncher.launch(missingPerms.toTypedArray())
                                }
                            }
                        )
                    }
                }
            }

            // 4. Answering Machine Timing & Audio Routing Settings
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Pickup & Capture Controls",
                            style = MaterialTheme.typography.titleLarge
                        )

                        // Rings slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rings Before Auto-Answer",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${uiState.settings.ringsBeforeAnswer} rings (~${uiState.settings.ringsBeforeAnswer * 4}s)",
                                    style = EchoMonoMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = uiState.settings.ringsBeforeAnswer.toFloat(),
                                onValueChange = { onSetRingsBeforeAnswer(it.roundToInt()) },
                                valueRange = 1f..8f,
                                steps = 6,
                                modifier = Modifier.testTag("rings_slider")
                            )
                        }

                        // Max message duration chips
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Max Caller Recording Duration",
                                style = MaterialTheme.typography.titleSmall
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(30, 60, 120, 180).forEach { seconds ->
                                    FilterChip(
                                        selected = uiState.settings.maxRecordingSeconds == seconds,
                                        onClick = { onSetMaxRecordingSeconds(seconds) },
                                        label = {
                                            Text(
                                                text = if (seconds < 60) "${seconds}s" else "${seconds / 60}m",
                                                style = EchoMonoSmall
                                            )
                                        },
                                        modifier = Modifier.testTag("duration_chip_$seconds")
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Speakerphone acoustic capture toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Speakerphone Acoustic Capture",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Routes answered call to speaker so device mic can record caller voice on Android 10+.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = uiState.settings.speakerphoneCapture,
                                onCheckedChange = onSetSpeakerphoneCapture,
                                modifier = Modifier.testTag("speaker_capture_switch")
                            )
                        }

                        // Beep after greeting toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column {
                                    Text(
                                        text = "Play 980Hz Tone After Greeting",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Signals caller to begin speaking before recording starts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = uiState.settings.playBeepAfterGreeting,
                                onCheckedChange = onSetPlayBeepAfterGreeting,
                                modifier = Modifier.testTag("beep_toggle_switch")
                            )
                        }
                    }
                }
            }

            // 5. Hardware Acoustic Pipeline Test & Android Telephony Note
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Acoustic Pipeline Verification",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = "Runs your active greeting, synthesizes the beep, and records 12 seconds via microphone directly into your Room Inbox so you can test playback and levels locally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                if (uiState.readiness.hasRecordAudio) {
                                    onStartAcousticTest()
                                } else {
                                    audioPermForTestLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            enabled = !isTestingOrCalling,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_acoustic_test_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test Greeting + Beep + Mic Recording")
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Android 10+ restricts direct uplink/downlink telephony audio capture for third-party dialers. Keep Speakerphone Acoustic Capture enabled for reliable caller voice recording.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isReady: Boolean,
    actionLabel: String,
    testTag: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isReady) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isReady) Icons.Filled.CheckCircle else icon,
                        contentDescription = null,
                        tint = if (isReady) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isReady) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "$title granted",
                tint = MaterialTheme.colorScheme.secondary
            )
        } else {
            FilledTonalButton(
                onClick = onAction,
                modifier = Modifier.testTag(testTag)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(actionLabel)
            }
        }
    }
}
