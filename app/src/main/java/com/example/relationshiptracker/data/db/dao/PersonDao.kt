package com.example.relationshiptracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.relationshiptracker.data.db.entities.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("SELECT * FROM persons ORDER BY lastContactTime DESC")
    fun getAllPersons(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Int): Person?

    @Query("SELECT * FROM persons WHERE category IN (:categories) ORDER BY lastContactTime DESC")
    fun getPersonsByCategories(categories: Set<String>): Flow<List<Person>>

}