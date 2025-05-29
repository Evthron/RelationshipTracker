package com.example.relationshiptracker.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Int,
    val content: String,
    val timestamp: Long,
    val category: ConversationCategory,
    val tag: String? // Make tag nullable to avoid schema issues if not always provided
)

enum class ConversationCategory {
    EMOTIONAL,
    PRACTICAL,
    VALIDATION,
    SHARE,
    INFORMATION,
    CASUAL
}