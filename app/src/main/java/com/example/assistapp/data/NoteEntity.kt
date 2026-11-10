package com.assisted.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val timestamp: Long,
    val subjectTag: String // "Math", "Science", "English", "Tamil", "History", "Other"
)
