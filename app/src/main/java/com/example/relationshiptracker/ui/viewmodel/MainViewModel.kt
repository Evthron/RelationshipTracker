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

    suspend fun getPersonById(id: Int): Person? {
        return personDao.getPersonById(id)
    }

    fun addConversation(personId: Int, content: String, tag: String?) {
        viewModelScope.launch {
            val conversation = Conversation(
                personId = personId,
                content = content,
                tag = tag,
                timestamp = System.currentTimeMillis(),
                category = ConversationCategory.CASUAL // Adjust based on your logic
            )
            conversationDao.insert(conversation)
            val person = personDao.getPersonById(personId)
            person?.let {
                personDao.update(it.copy(lastContactTime = conversation.timestamp))
            }
        }
    }

    fun getConversationsByPerson(personId: Int): Flow<List<Conversation>> {
        return conversationDao.getConversationsByPerson(personId)
    }

    fun getConversationsByTag(personId: Int, tag: String): Flow<List<Conversation>> {
        return conversationDao.getConversationsByPersonAndTag(personId, tag)
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
