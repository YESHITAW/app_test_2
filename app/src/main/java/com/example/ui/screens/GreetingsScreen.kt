package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.GreetingEntity
import com.example.data.local.GreetingType
import com.example.ui.EchoDeckUiState
import com.example.ui.components.LiveVuMeter
import com.example.ui.components.formatDuration
import com.example.ui.theme.EchoMonoLarge
import com.example.ui.theme.EchoMonoSmall

@Composable
fun GreetingsScreen(
    uiState: EchoDeckUiState,
    onActivateGreeting: (Long) -> Unit,
    onPreviewGreeting: (GreetingEntity) -> Unit,
    onDeleteGreeting: (Long) -> Unit,
    onStartRecording: () -> Boolean,
    onStopRecording: () -> Unit,
    onPreviewPending: () -> Unit,
    onSavePendingRecording: (String) -> Unit,
    onDiscardPending: () -> Unit,
    onSaveTtsGreeting: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTtsDialog by remember { mutableStateOf(false) }
    var pendingTitleInput by remember { mutableStateOf("My Voice Greeting") }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        }
    }

    val recState = uiState.greetingRecording

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Greeting Studio",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Record your own voice outgoing message or write a spoken TTS script.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Studio Recorder Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Voice Recorder",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            if (recState.isRecording) {
                                Text(
                                    text = formatDuration(recState.elapsedSeconds),
                                    style = EchoMonoLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AnimatedVisibility(visible = recState.isRecording) {
                            LiveVuMeter(amplitude = recState.amplitude)
                        }

                        if (recState.pendingAudioPath == null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!recState.isRecording) {
                                    Button(
                                        onClick = {
                                            val hasMic = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (hasMic) {
                                                onStartRecording()
                                            } else {
                                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("record_greeting_button")
                                    ) {
                                        Icon(Icons.Filled.Mic, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Record Voice")
                                    }
                                    FilledTonalButton(
                                        onClick = { showTtsDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("add_tts_greeting_button")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("TTS Script")
                                    }
                                } else {
                                    Button(
                                        onClick = onStopRecording,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("stop_recording_greeting_button")
                                    ) {
                                        Icon(Icons.Filled.Stop, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Stop Recording")
                                    }
                                }
                            }
                        } else {
                            // Pending recorded greeting ready to preview/save
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = pendingTitleInput,
                                    onValueChange = { pendingTitleInput = it },
                                    label = { Text("Greeting Title") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isPreviewing = uiState.playbackState.activeKey == "pending_greet" &&
                                        uiState.playbackState.isPlaying
                                    OutlinedButton(
                                        onClick = onPreviewPending,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (isPreviewing) "Pause" else "Preview (${recState.pendingDurationSeconds}s)")
                                    }
                                    Button(
                                        onClick = {
                                            onSavePendingRecording(pendingTitleInput)
                                            pendingTitleInput = "My Voice Greeting"
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("save_recorded_greeting_button")
                                    ) {
                                        Icon(Icons.Filled.Save, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Save & Use")
                                    }
                                }
                                TextButton(
                                    onClick = onDiscardPending,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Discard Take", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Saved Greetings List
            item {
                Text(
                    text = "Saved Outgoing Greetings (${uiState.greetings.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            items(uiState.greetings, key = { it.id }) { greeting ->
                val isPlayingAudio = uiState.playbackState.activeKey == "greet_${greeting.id}" &&
                    uiState.playbackState.isPlaying
                val isPlayingTts = uiState.previewingTtsGreetingId == greeting.id
                val isPreviewing = isPlayingAudio || isPlayingTts

                Card(
                    shape = MaterialTheme.shapes.large,
                    border = if (greeting.isActive) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (greeting.isActive) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    modifier = Modifier.testTag("greeting_card_${greeting.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = greeting.isActive,
                                    onClick = { onActivateGreeting(greeting.id) }
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = greeting.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = if (greeting.type == GreetingType.RECORDED) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.secondaryContainer
                                            }
                                        ) {
                                            Text(
                                                text = if (greeting.type == GreetingType.RECORDED) "MIC" else "TTS",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${formatDuration(greeting.durationSeconds)} • ${if (greeting.isActive) "Active on Auto-Answer" else "Tap to activate"}",
                                        style = EchoMonoSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { onPreviewGreeting(greeting) }) {
                                    Icon(
                                        imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Preview greeting ${greeting.title}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (uiState.greetings.size > 1) {
                                    IconButton(onClick = { onDeleteGreeting(greeting.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete greeting",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        if (greeting.type == GreetingType.TTS && greeting.ttsScript.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "\"${greeting.ttsScript}\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (greeting.isActive) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Currently active outgoing greeting",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTtsDialog) {
        var title by remember { mutableStateOf("") }
        var script by remember { mutableStateOf("Hello! I can't answer right now. Please leave your name and message after the beep.") }
        AlertDialog(
            onDismissRequest = { showTtsDialog = false },
            title = { Text("New Spoken TTS Greeting") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (e.g., After Hours)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = script,
                        onValueChange = { script = it },
                        label = { Text("Spoken Script") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveTtsGreeting(title.ifBlank { "Custom TTS Greeting" }, script)
                        showTtsDialog = false
                    },
                    enabled = script.isNotBlank()
                ) {
                    Text("Save & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTtsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
