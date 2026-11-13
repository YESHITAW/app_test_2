package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voicemail_messages")
data class VoicemailMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callerName: String,
    val callerNumber: String,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val audioFilePath: String,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val handledByMachine: Boolean = true,
    val isAcousticTest: Boolean = false,
    val notes: String = "",
    val waveformPeaks: String = ""
)
