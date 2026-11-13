package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.PlaybackState
import com.example.data.local.VoicemailMessageEntity
import com.example.ui.EchoDeckUiState
import com.example.ui.components.InteractiveWaveformBar
import com.example.ui.components.formatDuration
import com.example.ui.components.formatMillis
import com.example.ui.theme.EchoMonoSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class InboxFilter(val label: String) {
    ALL("All"),
    UNHEARD("Unheard"),
    STARRED("Starred")
}

@Composable
fun InboxScreen(
    uiState: EchoDeckUiState,
    onTogglePlayPause: (VoicemailMessageEntity) -> Unit,
    onSeekPlayback: (Int) -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleStar: (VoicemailMessageEntity) -> Unit,
    onToggleRead: (Long, Boolean) -> Unit,
    onUpdateNotes: (Long, String) -> Unit,
    onDeleteMessage: (Long) -> Unit,
    onCreateShareIntent: (VoicemailMessageEntity) -> Intent?,
    onCallBack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
    var editingNoteMessage by remember { mutableStateOf<VoicemailMessageEntity?>(null) }
    var deletingMessage by remember { mutableStateOf<VoicemailMessageEntity?>(null) }

    val filteredMessages = remember(uiState.messages, selectedFilter) {
        when (selectedFilter) {
            InboxFilter.ALL -> uiState.messages
            InboxFilter.UNHEARD -> uiState.messages.filter { !it.isRead }
            InboxFilter.STARRED -> uiState.messages.filter { it.isStarred }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
        ) {
            // Header + Filter Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tape Inbox",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${uiState.messages.size} saved • ${uiState.unreadCount} unheard",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "LOCAL ROOM DB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InboxFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                val countSuffix = when (filter) {
                                    InboxFilter.ALL -> if (uiState.messages.isNotEmpty()) " (${uiState.messages.size})" else ""
                                    InboxFilter.UNHEARD -> if (uiState.unreadCount > 0) " (${uiState.unreadCount})" else ""
                                    InboxFilter.STARRED -> ""
                                }
                                Text("${filter.label}$countSuffix")
                            },
                            modifier = Modifier.testTag("inbox_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Voicemail,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Text(
                            text = when (selectedFilter) {
                                InboxFilter.ALL -> "Tape Reel Empty"
                                InboxFilter.UNHEARD -> "All Caught Up"
                                InboxFilter.STARRED -> "No Starred Messages"
                            },
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = when (selectedFilter) {
                                InboxFilter.ALL -> "When EchoDeck answers an incoming call—or when you run the Acoustic Hardware Test on the Deck tab—recorded caller messages appear here."
                                InboxFilter.UNHEARD -> "Every recorded message has been listened to."
                                InboxFilter.STARRED -> "Tap the star icon on any voicemail to pin important callers."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMessages, key = { it.id }) { message ->
                        VoicemailMessageCard(
                            message = message,
                            playbackState = uiState.playbackState,
                            onTogglePlayPause = { onTogglePlayPause(message) },
                            onSeekPlayback = onSeekPlayback,
                            onToggleSpeaker = onToggleSpeaker,
                            onToggleStar = { onToggleStar(message) },
                            onToggleRead = { onToggleRead(message.id, !message.isRead) },
                            onEditNotes = { editingNoteMessage = message },
                            onShare = {
                                onCreateShareIntent(message)?.let { shareIntent ->
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Voicemail Audio"))
                                }
                            },
                            onCallBack = { onCallBack(message.callerNumber) },
                            onDelete = { deletingMessage = message }
                        )
                    }
                }
            }
        }
    }

    editingNoteMessage?.let { target ->
        var noteText by remember(target.id) { mutableStateOf(target.notes) }
        AlertDialog(
            onDismissRequest = { editingNoteMessage = null },
            title = { Text("Caller Note") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Summary or follow-up note") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNotes(target.id, noteText.trim())
                        editingNoteMessage = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    deletingMessage?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingMessage = null },
            title = { Text("Delete Recording?") },
            text = {
                Text("This permanently removes the message from ${target.callerName} and deletes its local audio file.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMessage(target.id)
                        deletingMessage = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMessage = null }) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
private fun VoicemailMessageCard(
    message: VoicemailMessageEntity,
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onSeekPlayback: (Int) -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleStar: () -> Unit,
    onToggleRead: () -> Unit,
    onEditNotes: () -> Unit,
    onShare: () -> Unit,
    onCallBack: () -> Unit,
    onDelete: () -> Unit
) {
    val itemKey = "msg_${message.id}"
    val isThisActive = playbackState.activeKey == itemKey
    val isThisPlaying = isThisActive && playbackState.isPlaying
    val totalMs = if (isThisActive && playbackState.durationMs > 0) {
        playbackState.durationMs
    } else {
        (message.durationSeconds * 1000).coerceAtLeast(1000)
    }
    val currentMs = if (isThisActive) playbackState.positionMs else 0
    val progress = (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)

    val formattedDate = remember(message.timestampMillis) {
        SimpleDateFormat("MMM d • HH:mm", Locale.getDefault()).format(Date(message.timestampMillis))
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (!message.isRead) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        modifier = Modifier.testTag("voicemail_card_${message.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!message.isRead) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = message.callerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.SemiBold
                            )
                            if (message.isAcousticTest) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "TEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${message.callerNumber}  •  $formattedDate",
                            style = EchoMonoSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (message.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (message.isStarred) "Unstar message" else "Star message",
                        tint = if (message.isStarred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playback + Interactive Waveform row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isThisPlaying) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("play_message_${message.id}")
                ) {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isThisPlaying) "Pause message" else "Play message",
                        tint = if (isThisPlaying) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    InteractiveWaveformBar(
                        peaksCsv = message.waveformPeaks,
                        progressFraction = progress,
                        isActive = isThisActive,
                        onSeekFraction = { frac ->
                            if (!isThisActive) {
                                onTogglePlayPause()
                            }
                            onSeekPlayback((frac * totalMs).toInt())
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isThisActive) formatMillis(currentMs) else "00:00",
                            style = EchoMonoSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(message.durationSeconds),
                            style = EchoMonoSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = message.notes.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = message.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Action strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onToggleSpeaker) {
                        Icon(
                            imageVector = Icons.Filled.SpeakerPhone,
                            contentDescription = "Toggle speakerphone playback",
                            tint = if (playbackState.useSpeakerphone) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onEditNotes) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = "Edit caller note"
                        )
                    }
                    IconButton(onClick = onToggleRead) {
                        Icon(
                            imageVector = if (message.isRead) {
                                Icons.Filled.MarkEmailUnread
                            } else {
                                Icons.Filled.MarkEmailRead
                            },
                            contentDescription = if (message.isRead) "Mark as unheard" else "Mark as heard"
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share audio file"
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!message.isAcousticTest && message.callerNumber.any { it.isDigit() }) {
                        IconButton(onClick = onCallBack) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call back ${message.callerName}",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete voicemail",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
