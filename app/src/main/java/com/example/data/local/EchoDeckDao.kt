package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EchoDeckDao {

    // Voicemail Messages
    @Query("SELECT * FROM voicemail_messages ORDER BY timestampMillis DESC")
    fun observeAllMessages(): Flow<List<VoicemailMessageEntity>>

    @Query("SELECT COUNT(*) FROM voicemail_messages WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VoicemailMessageEntity): Long

    @Update
    suspend fun updateMessage(message: VoicemailMessageEntity)

    @Query("UPDATE voicemail_messages SET isRead = :isRead WHERE id = :id")
    suspend fun setMessageRead(id: Long, isRead: Boolean)

    @Query("UPDATE voicemail_messages SET isStarred = :isStarred WHERE id = :id")
    suspend fun setMessageStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE voicemail_messages SET notes = :notes WHERE id = :id")
    suspend fun updateMessageNotes(id: Long, notes: String)

    @Query("SELECT * FROM voicemail_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): VoicemailMessageEntity?

    @Query("DELETE FROM voicemail_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    // Greetings
    @Query("SELECT * FROM greetings ORDER BY isActive DESC, createdAtMillis DESC")
    fun observeAllGreetings(): Flow<List<GreetingEntity>>

    @Query("SELECT * FROM greetings WHERE isActive = 1 LIMIT 1")
    fun observeActiveGreeting(): Flow<GreetingEntity?>

    @Query("SELECT * FROM greetings WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveGreeting(): GreetingEntity?

    @Query("SELECT COUNT(*) FROM greetings")
    suspend fun getGreetingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGreeting(greeting: GreetingEntity): Long

    @Query("UPDATE greetings SET isActive = 0")
    suspend fun clearActiveGreeting()

    @Query("UPDATE greetings SET isActive = 1 WHERE id = :id")
    suspend fun setActiveGreeting(id: Long)

    @Query("SELECT * FROM greetings WHERE id = :id LIMIT 1")
    suspend fun getGreetingById(id: Long): GreetingEntity?

    @Query("SELECT * FROM greetings ORDER BY createdAtMillis DESC LIMIT 1")
    suspend fun getAnyGreeting(): GreetingEntity?

    @Query("DELETE FROM greetings WHERE id = :id")
    suspend fun deleteGreetingById(id: Long)

    // Screening Rules
    @Query("SELECT * FROM screening_rules ORDER BY createdAtMillis DESC")
    fun observeAllScreeningRules(): Flow<List<ScreeningRuleEntity>>

    @Query("SELECT * FROM screening_rules")
    suspend fun getAllScreeningRulesOnce(): List<ScreeningRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreeningRule(rule: ScreeningRuleEntity): Long

    @Query("DELETE FROM screening_rules WHERE id = :id")
    suspend fun deleteScreeningRuleById(id: Long)
}
