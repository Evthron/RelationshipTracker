package com.example.relationshiptracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val impression: String,
    val interests: String,
    val goals: String,
    val category: String,
    val lastContactTime: Long = 0, // Timestamp of last conversation
)