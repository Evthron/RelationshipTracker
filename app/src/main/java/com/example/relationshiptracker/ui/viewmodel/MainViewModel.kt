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
import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import java.text.SimpleDateFormat
import java.util.*
import java.text.ParseException

class MainViewModel(context: Context) : ViewModel() {
    private val personDao = AppDatabase.getDatabase(context).personDao()
    private val conversationDao = AppDatabase.getDatabase(context).conversationDao()
    private val appContext = context.applicationContext

    val allPersons: Flow<List<Person>> = personDao.getAllPersons()

    fun addPerson(name: String, impression: String, interests : String, goals: String, category: String) {
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

    fun addConversation(personId: Int, content: String, tag: String?, timestamp: Long) {
        viewModelScope.launch {
            val conversation = Conversation(
                personId = personId,
                content = content,
                tag = tag ?: "Casual",
                timestamp = timestamp,
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
            val person = personDao.getPersonById(conversation.personId)
            person?.let {
                personDao.update(it.copy(lastContactTime = conversation.timestamp))
            }
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

    fun importDatabaseFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    CSVReader(inputStream.reader()).use { csvReader ->
                        val allRecords = csvReader.readAll()
                        val header = allRecords.firstOrNull()
                        if (header == null || header.size < 5 || header[0] != "FeatureName" || header[1] != "Timestamp" || header[2] != "Value" || header[3] != "Label" || header[4] != "Note") {
                            // Log error or notify user of invalid CSV format
                            return@use
                        }

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                        allRecords.drop(1).forEach { record ->
                            if (record.size < 5) return@forEach // Skip incomplete records
                            val featureName = record[0].trim().replace(Regex("[😭🤝🎉🎁ℹ️]"), "") // Remove emojis
                            val timestampStr = record[1].trim()
                            val value = record[2].trim()
                            val label = record[3].trim()
                            val note = record[4].trim()

                            if (label.isBlank() || note.isBlank()) return@forEach // Skip records with empty label or note
                            if (value.toDoubleOrNull() != 1.0) return@forEach // Skip if value is not 1.0

                            try {
                                val timestamp = dateFormat.parse(timestampStr)?.time ?: return@forEach
                                val category = when (featureName) {
                                    "Emotional" -> ConversationCategory.EMOTIONAL
                                    "Practical" -> ConversationCategory.PRACTICAL
                                    "Validation" -> ConversationCategory.VALIDATION
                                    "Share" -> ConversationCategory.SHARE
                                    "Information" -> ConversationCategory.INFORMATION
                                    else -> ConversationCategory.CASUAL
                                }

                                // Check if person exists, or create a new one
                                val existingPerson = personDao.getAllPersonsSync().find { it.name == label }
                                val personId = if (existingPerson != null) {
                                    existingPerson.id
                                } else {
                                    personDao.insert(Person(
                                        name = label, category = "Imported",
                                        impression = "",
                                        interests = "",
                                        goals = ""
                                    )).toInt()
                                }

                                // Insert conversation
                                conversationDao.insert(
                                    Conversation(
                                        personId = personId,
                                        content = note,
                                        tag = featureName,
                                        timestamp = timestamp,
                                        category = category
                                    )
                                )

                                // Update person's lastContactTime
                                val person = personDao.getPersonById(personId)
                                person?.let {
                                    personDao.update(it.copy(lastContactTime = maxOf(it.lastContactTime, timestamp)))
                                }
                            } catch (e: ParseException) {
                                // Log error or notify user of invalid timestamp
                                e.printStackTrace()
                            }
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