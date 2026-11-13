package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScreeningAction
import com.example.ui.EchoDeckUiState
import com.example.ui.theme.EchoMonoLarge
import com.example.ui.theme.EchoMonoSmall

@Composable
fun DialerScreen(
    uiState: EchoDeckUiState,
    onAppendChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPlaceCall: (String) -> Unit,
    onSetScreenUnknown: (Boolean) -> Unit,
    onAddRule: (String, String, ScreeningAction) -> Unit,
    onDeleteRule: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    val toneGenerator = remember {
        runCatching { ToneGenerator(AudioManager.STREAM_DTMF, 65) }.getOrNull()
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { toneGenerator?.release() }
        }
    }

    fun playKeyTone(ch: Char) {
        val tone = when (ch) {
            '0' -> ToneGenerator.TONE_DTMF_0
            '1' -> ToneGenerator.TONE_DTMF_1
            '2' -> ToneGenerator.TONE_DTMF_2
            '3' -> ToneGenerator.TONE_DTMF_3
            '4' -> ToneGenerator.TONE_DTMF_4
            '5' -> ToneGenerator.TONE_DTMF_5
            '6' -> ToneGenerator.TONE_DTMF_6
            '7' -> ToneGenerator.TONE_DTMF_7
            '8' -> ToneGenerator.TONE_DTMF_8
            '9' -> ToneGenerator.TONE_DTMF_9
            '*' -> ToneGenerator.TONE_DTMF_S
            '#' -> ToneGenerator.TONE_DTMF_P
            else -> return
        }
        runCatching { toneGenerator?.startTone(tone, 120) }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Filled.Dialpad, contentDescription = null) },
                    modifier = Modifier.testTag("dialer_subtab_keypad")
                ) {
                    Text("Keypad")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Filled.Security, contentDescription = null) },
                    modifier = Modifier.testTag("dialer_subtab_rules")
                ) {
                    Text("Screening (${uiState.screeningRules.size})")
                }
            }

            if (selectedTab == 0) {
                // Keypad view
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Number display
                    Card(
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.dialerInput.ifEmpty { "Enter number" },
                                style = EchoMonoLarge,
                                color = if (uiState.dialerInput.isEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.dialerInput.isNotEmpty()) {
                                IconButton(
                                    onClick = onBackspace,
                                    modifier = Modifier.testTag("dialer_backspace_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace digit"
                                    )
                                }
                            }
                        }
                    }

                    // 4x3 Tactile Keypad
                    val rows = listOf(
                        listOf('1' to "", '2' to "ABC", '3' to "DEF"),
                        listOf('4' to "GHI", '5' to "JKL", '6' to "MNO"),
                        listOf('7' to "PQRS", '8' to "TUV", '9' to "WXYZ"),
                        listOf('*' to "", '0' to "+", '#' to "")
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        rows.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                row.forEach { (digit, letters) ->
                                    Surface(
                                        onClick = {
                                            playKeyTone(digit)
                                            onAppendChar(digit)
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        modifier = Modifier
                                            .size(76.dp)
                                            .testTag("dial_key_$digit")
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = digit.toString(),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (letters.isNotEmpty()) {
                                                Text(
                                                    text = letters,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Call button row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlaceCall(uiState.dialerInput) },
                            enabled = uiState.dialerInput.isNotBlank(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .height(60.dp)
                                .width(180.dp)
                                .testTag("place_call_button")
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Call", style = MaterialTheme.typography.titleMedium)
                        }
                        if (uiState.dialerInput.isNotEmpty()) {
                            Spacer(Modifier.width(12.dp))
                            TextButton(onClick = onClear) {
                                Text("Clear")
                            }
                        }
                    }
                }
            } else {
                // Screening rules view
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Reject Private / Unknown Callers",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Calls without caller ID are blocked by CallScreeningService.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Switch(
                                    checked = uiState.settings.screenUnknownCallers,
                                    onCheckedChange = onSetScreenUnknown
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Number Routing Rules",
                                style = MaterialTheme.typography.titleMedium
                            )
                            FilledTonalButton(
                                onClick = { showAddRuleDialog = true },
                                modifier = Modifier.testTag("add_screening_rule_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Add Rule")
                            }
                        }
                    }

                    if (uiState.screeningRules.isEmpty()) {
                        item {
                            Card(
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Text(
                                    text = "No custom rules yet. Add a number to always allow VIP callers (bypassing the answering machine), send callers straight to the machine, or block spam.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(uiState.screeningRules, key = { it.id }) { rule ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (rule.action) {
                                                ScreeningAction.ALLOW -> Icons.Filled.Check
                                                ScreeningAction.DIRECT_TO_MACHINE -> Icons.Filled.Voicemail
                                                ScreeningAction.BLOCK -> Icons.Filled.Block
                                            },
                                            contentDescription = null,
                                            tint = when (rule.action) {
                                                ScreeningAction.ALLOW -> MaterialTheme.colorScheme.secondary
                                                ScreeningAction.DIRECT_TO_MACHINE -> MaterialTheme.colorScheme.primary
                                                ScreeningAction.BLOCK -> MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Column {
                                            Text(rule.label, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "${rule.phoneNumberPattern} • ${rule.action.displayLabel()}",
                                                style = EchoMonoSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteRule(rule.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete rule",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddRuleDialog) {
        var numberPattern by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }
        var action by remember { mutableStateOf(ScreeningAction.DIRECT_TO_MACHINE) }

        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Add Screening Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = numberPattern,
                        onValueChange = { numberPattern = it },
                        label = { Text("Phone Number or Suffix") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label (e.g., Mom, Telemarketer)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Action", style = MaterialTheme.typography.labelLarge)
                    ScreeningAction.entries.forEach { option ->
                        Surface(
                            onClick = { action = option },
                            shape = MaterialTheme.shapes.small,
                            color = if (action == option) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = option.displayLabel(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAddRule(numberPattern, label, action)
                        showAddRuleDialog = false
                    },
                    enabled = numberPattern.any { it.isDigit() }
                ) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun ScreeningAction.displayLabel(): String = when (this) {
    ScreeningAction.ALLOW -> "Bypass Machine (Always Ring)"
    ScreeningAction.DIRECT_TO_MACHINE -> "Send Straight to Machine"
    ScreeningAction.BLOCK -> "Block & Silence"
}
