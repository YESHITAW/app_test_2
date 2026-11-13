package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScreeningAction {
    ALLOW,
    DIRECT_TO_MACHINE,
    BLOCK
}

@Entity(tableName = "screening_rules")
data class ScreeningRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumberPattern: String,
    val label: String,
    val action: ScreeningAction,
    val createdAtMillis: Long = System.currentTimeMillis()
)
