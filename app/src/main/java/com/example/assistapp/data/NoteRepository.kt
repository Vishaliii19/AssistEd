package com.assisted.app.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insert(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return noteDao.searchNotes(query)
    }
}
