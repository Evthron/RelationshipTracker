package com.example.relationshiptracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.relationshiptracker.data.db.dao.ConversationDao
import com.example.relationshiptracker.data.db.dao.PersonDao
import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.data.db.entities.Person

@Database(entities = [Person::class, Conversation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "relationship_tracker_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}