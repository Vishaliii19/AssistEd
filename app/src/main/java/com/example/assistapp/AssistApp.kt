package com.assisted.app

import android.app.Application
import android.util.Log
import com.assisted.app.data.AppDatabase
import com.assisted.app.data.NoteRepository
import com.assisted.app.managers.TtsManager
import com.assisted.app.managers.VoiceNavManager

class AssistApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: NoteRepository
        private set

    lateinit var ttsManager: TtsManager
        private set

    lateinit var voiceNavManager: VoiceNavManager
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("AssistApp", "Application Created")

        try {
            database = AppDatabase.getDatabase(this)
            repository = NoteRepository(database.noteDao())
            Log.d("AssistApp", "Database initialized")
        } catch (e: Exception) {
            Log.e("AssistApp", "Database init failed", e)
        }

        try {
            ttsManager = TtsManager(this)
            Log.d("AssistApp", "TtsManager initialized")
        } catch (e: Exception) {
            Log.e("AssistApp", "TtsManager init failed", e)
        }

        try {
            voiceNavManager = VoiceNavManager(this)
            Log.d("AssistApp", "VoiceNavManager initialized")
        } catch (e: Exception) {
            Log.e("AssistApp", "VoiceNavManager init failed", e)
        }
    }
}
