package com.example.relationshiptracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.data.db.entities.ConversationCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Query("SELECT * FROM conversations WHERE personId = :personId ORDER BY timestamp DESC")
    fun getConversationsByPerson(personId: Int): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE personId = :personId AND tag = :tag ORDER BY timestamp DESC")
    fun getConversationsByPersonAndTag(personId: Int, tag: String): Flow<List<Conversation>>

    @Query("""
        SELECT category, COUNT(*) as count 
        FROM conversations 
        WHERE personId = :personId 
        GROUP BY category
    """)
    fun getConversationStats(personId: Int): Flow<List<ConversationStat>>

    @Query("SELECT MAX(timestamp) FROM conversations WHERE personId = :personId")
    suspend fun getLastConversationTime(personId: Int): Long?
}

data class ConversationStat(val category: ConversationCategory, val count: Int)