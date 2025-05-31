package com.example.relationshiptracker.ui.viewmodel

import android.content.Context
import android.net.Uri
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
import com.opencsv.CSVWriter
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(context: Context) : ViewModel() {
    private val personDao = AppDatabase.getDatabase(context).personDao()
    private val conversationDao = AppDatabase.getDatabase(context).conversationDao()
    private val appContext = context.applicationContext

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

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationDao.delete(conversation)
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

    fun exportDatabaseToCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val persons = personDao.getAllPersonsSync()
                val conversations = conversationDao.getAllConversationsSync()

                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    CSVWriter(outputStream.writer()).use { csvWriter ->
                        // Write Persons table
                        csvWriter.writeNext(arrayOf("Persons Table"))
                        csvWriter.writeNext(
                            arrayOf(
                                "id",
                                "name",
                                "impression",
                                "interests",
                                "goals",
                                "category",
                                "lastContactTime"
                            )
                        )
                        persons.forEach { person ->
                            csvWriter.writeNext(
                                arrayOf(
                                    person.id.toString(),
                                    person.name,
                                    person.impression,
                                    person.interests,
                                    person.goals,
                                    person.category,
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(person.lastContactTime))
                                )
                            )
                        }

                        // Write Conversations table
                        csvWriter.writeNext(arrayOf("")) // Separator
                        csvWriter.writeNext(arrayOf("Conversations Table"))
                        csvWriter.writeNext(
                            arrayOf(
                                "id",
                                "personId",
                                "content",
                                "tag",
                                "timestamp",
                                "category"
                            )
                        )
                        conversations.forEach { conversation ->
                            csvWriter.writeNext(
                                arrayOf(
                                    conversation.id.toString(),
                                    conversation.personId.toString(),
                                    conversation.content,
                                    conversation.tag ?: "",
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(conversation.timestamp)),
                                    conversation.category.toString()
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error (consider adding a Toast or Snackbar in UI to notify user)
                e.printStackTrace()
            }
        }
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