package com.example.relationshiptracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.data.db.entities.ConversationCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Dao
interface ConversationDao {
    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Update
    suspend fun update(conversation: Conversation)

    @Delete
    suspend fun delete(conversation: Conversation)

    @Query("SELECT * FROM conversations WHERE personId = :personId ORDER BY timestamp DESC")
    fun getConversationsByPerson(personId: Int): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE personId = :personId AND tag = :tag ORDER BY timestamp DESC")
    fun getConversationsByPersonAndTag(personId: Int, tag: String): Flow<List<Conversation>>

    @Query("""
        SELECT c.*, p.name as personName 
        FROM conversations c 
        INNER JOIN persons p ON c.personId = p.id 
        WHERE c.personId = :personId 
        ORDER BY c.timestamp DESC
    """)
    fun getConversationsWithPersonByPerson(personId: Int): Flow<List<ConversationWithPerson>>

    @Query("""
        SELECT c.*, p.name as personName 
        FROM conversations c 
        INNER JOIN persons p ON c.personId = p.id 
        WHERE c.personId = :personId AND c.tag = :tag 
        ORDER BY c.timestamp DESC
    """)
    fun getConversationsWithPersonByPersonAndTag(personId: Int, tag: String): Flow<List<ConversationWithPerson>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE tag = :tag ORDER BY timestamp DESC")
    fun getAllConversationsByTag(tag: String): Flow<List<Conversation>>

    @Query("""
        SELECT c.*, p.name as personName 
        FROM conversations c 
        INNER JOIN persons p ON c.personId = p.id 
        ORDER BY c.timestamp DESC
    """)
    fun getAllConversationsWithPerson(): Flow<List<ConversationWithPerson>>

    @Query("""
        SELECT c.*, p.name as personName 
        FROM conversations c 
        INNER JOIN persons p ON c.personId = p.id 
        WHERE c.tag = :tag 
        ORDER BY c.timestamp DESC
    """)
    fun getAllConversationsWithPersonByTag(tag: String): Flow<List<ConversationWithPerson>>

    @Query("""
        SELECT category, COUNT(*) as count 
        FROM conversations 
        WHERE personId = :personId 
        GROUP BY category
    """)
    fun getConversationStatsRaw(personId: Long): Flow<List<ConversationStat>>

    fun getConversationStats(personId: Long): Flow<Map<ConversationCategory, Int>> {
        return getConversationStatsRaw(personId).map { stats ->
            stats.associate { it.category to it.count }
        }
    }

    @Query("""
        SELECT category, COUNT(*) as count 
        FROM conversations 
        GROUP BY category
    """)
    fun getAllConversationStatsRaw(): Flow<List<ConversationStat>>

    fun getAllConversationStats(): Flow<Map<ConversationCategory, Int>> {
        return getAllConversationStatsRaw().map { stats ->
            stats.associate { it.category to it.count }
        }
    }

    @Query("SELECT MAX(timestamp) FROM conversations WHERE personId = :personId")
    suspend fun getLastConversationTime(personId: Int): Long?

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsSync(): List<Conversation>
}

data class ConversationStat(val category: ConversationCategory, val count: Int)

// Non-entity data class
data class ConversationWithPerson(
    val id: Long,
    val personId: Int,
    val content: String,
    val timestamp: Long,
    val category: ConversationCategory,
    val tag: String?,
    val personName: String // Allow quick access the person's name without doing lookup
)