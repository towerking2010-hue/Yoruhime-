package com.aistudio.yoruhime

import android.app.Application
import com.aistudio.yoruhime.data.local.AppDatabase
import com.aistudio.yoruhime.data.repository.ChatRepository
import com.aistudio.yoruhime.data.local.SecureApiKeyStore
import com.aistudio.yoruhime.service.BedtimeWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YoruHimeApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: ChatRepository
        private set

    lateinit var apiKeyStore: SecureApiKeyStore
        private set

    lateinit var ttsManager: com.aistudio.yoruhime.ui.audio.AyakaTtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = ChatRepository(database)
        apiKeyStore = SecureApiKeyStore(this)
        ttsManager = com.aistudio.yoruhime.ui.audio.AyakaTtsManager(this)

        // Restore the user's saved curfew; new installs default to 11:00 PM.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val saved = runCatching { database.bondDao().getBond() }.getOrNull()
            BedtimeWorker.scheduleCurfew(
                this@YoruHimeApplication,
                saved?.bedtimeHour ?: 23,
                saved?.bedtimeMinute ?: 0
            )
        }
    }
}
