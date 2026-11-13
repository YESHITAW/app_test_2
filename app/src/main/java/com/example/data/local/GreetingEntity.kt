package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GreetingType {
    RECORDED,
    TTS
}

@Entity(tableName = "greetings")
data class GreetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: GreetingType,
    val audioFilePath: String = "",
    val ttsScript: String = "",
    val durationSeconds: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)
