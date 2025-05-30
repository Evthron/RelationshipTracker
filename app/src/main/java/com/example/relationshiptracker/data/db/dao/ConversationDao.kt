package com.example.relationshiptracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.data.db.entities.ConversationCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    fun getConversationStatsRaw(personId: Long): Flow<List<ConversationStat>>

    fun getConversationStats(personId: Long): Flow<Map<ConversationCategory, Int>> {
        return getConversationStatsRaw(personId).map { stats ->
            stats.associate { it.category to it.count }
        }
    }

    @Query("SELECT MAX(timestamp) FROM conversations WHERE personId = :personId")
    suspend fun getLastConversationTime(personId: Int): Long?
}

data class ConversationStat(val category: ConversationCategory, val count: Int)