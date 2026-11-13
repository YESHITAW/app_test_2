package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

class EchoDeckRepository(
    private val context: Context,
    private val dao: EchoDeckDao
) {
    val messages: Flow<List<VoicemailMessageEntity>> = dao.observeAllMessages()
    val unreadCount: Flow<Int> = dao.observeUnreadCount()
    val greetings: Flow<List<GreetingEntity>> = dao.observeAllGreetings()
    val activeGreeting: Flow<GreetingEntity?> = dao.observeActiveGreeting()
    val screeningRules: Flow<List<ScreeningRuleEntity>> = dao.observeAllScreeningRules()

    val messagesDir: File
        get() = File(context.filesDir, "messages").apply { mkdirs() }

    val greetingsDir: File
        get() = File(context.filesDir, "greetings").apply { mkdirs() }

    fun createNewMessageFile(prefix: String = "msg"): File =
        File(messagesDir, "${prefix}_${System.currentTimeMillis()}.m4a")

    fun createNewGreetingFile(): File =
        File(greetingsDir, "greeting_${System.currentTimeMillis()}.m4a")

    suspend fun ensureDefaultGreetingsSeeded() {
        if (dao.getGreetingCount() == 0) {
            val defaultId = dao.insertGreeting(
                GreetingEntity(
                    title = "Standard Studio Greeting",
                    type = GreetingType.TTS,
                    ttsScript = "Hello, you have reached EchoDeck. I am unavailable right now. Please leave your name, number, and a brief message after the tone.",
                    durationSeconds = 9,
                    isActive = true
                )
            )
            dao.insertGreeting(
                GreetingEntity(
                    title = "Quick Away Notice",
                    type = GreetingType.TTS,
                    ttsScript = "Hi there, I cannot take your call at the moment. Leave a message after the beep and I will call you back shortly.",
                    durationSeconds = 7,
                    isActive = false
                )
            )
            dao.setActiveGreeting(defaultId)
        }
    }

    suspend fun getActiveGreetingOnce(): GreetingEntity? {
        ensureDefaultGreetingsSeeded()
        return dao.getActiveGreeting()
    }

    suspend fun saveMessage(message: VoicemailMessageEntity): Long = dao.insertMessage(message)

    suspend fun markMessageRead(id: Long, isRead: Boolean = true) = dao.setMessageRead(id, isRead)

    suspend fun toggleStarred(id: Long, currentStarred: Boolean) =
        dao.setMessageStarred(id, !currentStarred)

    suspend fun updateMessageNotes(id: Long, notes: String) = dao.updateMessageNotes(id, notes)

    suspend fun deleteMessage(id: Long) {
        val existing = dao.getMessageById(id)
        dao.deleteMessageById(id)
        if (existing != null && existing.audioFilePath.isNotBlank()) {
            runCatching { File(existing.audioFilePath).delete() }
        }
    }

    suspend fun saveRecordedGreeting(
        title: String,
        audioFilePath: String,
        durationSeconds: Int,
        activateImmediately: Boolean = true
    ): Long {
        if (activateImmediately) dao.clearActiveGreeting()
        return dao.insertGreeting(
            GreetingEntity(
                title = title.trim().ifEmpty { "Voice Greeting" },
                type = GreetingType.RECORDED,
                audioFilePath = audioFilePath,
                durationSeconds = durationSeconds,
                isActive = activateImmediately
            )
        )
    }

    suspend fun saveTtsGreeting(
        title: String,
        script: String,
        activateImmediately: Boolean = true
    ): Long {
        if (activateImmediately) dao.clearActiveGreeting()
        val estSeconds = (script.split("\\s+".toRegex()).size / 2.3f).toInt().coerceAtLeast(3)
        return dao.insertGreeting(
            GreetingEntity(
                title = title.trim().ifEmpty { "Spoken Greeting" },
                type = GreetingType.TTS,
                ttsScript = script.trim(),
                durationSeconds = estSeconds,
                isActive = activateImmediately
            )
        )
    }

    suspend fun activateGreeting(id: Long) {
        dao.clearActiveGreeting()
        dao.setActiveGreeting(id)
    }

    suspend fun deleteGreeting(id: Long) {
        val existing = dao.getGreetingById(id)
        dao.deleteGreetingById(id)
        if (existing != null && existing.audioFilePath.isNotBlank()) {
            runCatching { File(existing.audioFilePath).delete() }
        }
        ensureDefaultGreetingsSeeded()
        if (dao.getActiveGreeting() == null) {
            dao.getAnyGreeting()?.let { dao.setActiveGreeting(it.id) }
        }
    }

    suspend fun getScreeningRulesOnce(): List<ScreeningRuleEntity> = dao.getAllScreeningRulesOnce()

    suspend fun addScreeningRule(
        phoneNumberPattern: String,
        label: String,
        action: ScreeningAction
    ): Long = dao.insertScreeningRule(
        ScreeningRuleEntity(
            phoneNumberPattern = phoneNumberPattern.trim(),
            label = label.trim().ifEmpty { phoneNumberPattern.trim() },
            action = action
        )
    )

    suspend fun deleteScreeningRule(id: Long) = dao.deleteScreeningRuleById(id)
}
