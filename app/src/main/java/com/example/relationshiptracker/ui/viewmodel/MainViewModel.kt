package com.example.relationshiptracker.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.relationshiptracker.data.db.AppDatabase
import com.example.relationshiptracker.data.db.entities.Conversation
import com.example.relationshiptracker.data.db.entities.ConversationCategory
import com.example.relationshiptracker.data.db.entities.Person
import com.example.relationshiptracker.data.db.dao.ConversationWithPerson

class MainViewModel(context: Context) : ViewModel() {
    private val personDao = AppDatabase.getDatabase(context).personDao()
    private val conversationDao = AppDatabase.getDatabase(context).conversationDao()

    val allPersons: Flow<List<Person>> = personDao.getAllPersons()

    fun addPerson(name: String, impression: String, interests: String, goals: String, category: String) {
        viewModelScope.launch {
            personDao.insert(
                Person(
                    name = name,
                    impression = impression,
                    interests = interests,
                    goals = goals,
                    category = category
                )
            )
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            personDao.update(person)
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            personDao.delete(person)
        }
    }

    suspend fun getPersonById(id: Int): Person? {
        return personDao.getPersonById(id)
    }

    fun addConversation(personId: Int, content: String, tag: String?) {
        viewModelScope.launch {
            val conversation = Conversation(
                personId = personId,
                content = content,
                tag = tag ?: "Casual",
                timestamp = System.currentTimeMillis(),
                category = when (tag) {
                    "Emotional" -> ConversationCategory.EMOTIONAL
                    "Practical" -> ConversationCategory.PRACTICAL
                    "Validation" -> ConversationCategory.VALIDATION
                    "Share" -> ConversationCategory.SHARE
                    "Information" -> ConversationCategory.INFORMATION
                    else -> ConversationCategory.CASUAL
                }
            )
            conversationDao.insert(conversation)
            val person = personDao.getPersonById(personId)
            person?.let {
                personDao.update(it.copy(lastContactTime = conversation.timestamp))
            }
        }
    }

    fun updateConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationDao.update(conversation)
        }
    }

    fun getConversationsByPerson(personId: Int): Flow<List<Conversation>> {
        return conversationDao.getConversationsByPerson(personId)
    }

    fun getConversationsByTag(personId: Int, tag: String): Flow<List<Conversation>> {
        return conversationDao.getConversationsByPersonAndTag(personId, tag)
    }

    fun getConversationsWithPersonByPerson(personId: Int): Flow<List<ConversationWithPerson>> {
        return conversationDao.getConversationsWithPersonByPerson(personId)
    }

    fun getConversationsWithPersonByTag(personId: Int, tag: String): Flow<List<ConversationWithPerson>> {
        return conversationDao.getConversationsWithPersonByPersonAndTag(personId, tag)
    }

    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
    }

    fun getAllConversationsByTag(tag: String): Flow<List<Conversation>> {
        return conversationDao.getAllConversationsByTag(tag)
    }

    fun getAllConversationsWithPerson(): Flow<List<ConversationWithPerson>> {
        return conversationDao.getAllConversationsWithPerson()
    }

    fun getAllConversationsWithPersonByTag(tag: String): Flow<List<ConversationWithPerson>> {
        return conversationDao.getAllConversationsWithPersonByTag(tag)
    }

    fun getPersonsByCategories(categories: Set<String>): Flow<List<Person>> {
        return if (categories.isEmpty()) {
            personDao.getAllPersons()
        } else {
            personDao.getPersonsByCategories(categories)
        }
    }

    fun getConversationStats(personId: Long): Flow<Map<ConversationCategory, Int>> {
        return conversationDao.getConversationStats(personId)
    }

    fun getAllConversationStats(): Flow<Map<ConversationCategory, Int>> {
        return conversationDao.getAllConversationStats()
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}