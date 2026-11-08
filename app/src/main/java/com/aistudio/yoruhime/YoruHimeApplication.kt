package com.aistudio.yoruhime

import android.app.Application
import com.aistudio.yoruhime.data.local.AppDatabase
import com.aistudio.yoruhime.data.repository.ChatRepository
import com.aistudio.yoruhime.service.BedtimeWorker

class YoruHimeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ChatRepository
        private set

    lateinit var ttsManager: com.aistudio.yoruhime.ui.audio.AyakaTtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = ChatRepository(database)
        ttsManager = com.aistudio.yoruhime.ui.audio.AyakaTtsManager(this)

        // Schedule Daily Midnight Bedtime Worker
        BedtimeWorker.scheduleDailyMidnight(this)
    }
}
