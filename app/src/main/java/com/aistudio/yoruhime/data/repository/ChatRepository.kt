package com.aistudio.yoruhime.data.repository

import android.graphics.Bitmap
import com.aistudio.yoruhime.data.local.AppDatabase
import com.aistudio.yoruhime.data.local.ChatMessageEntity
import com.aistudio.yoruhime.data.local.OfflineDialogueBank
import com.aistudio.yoruhime.data.local.UserBondEntity
import com.aistudio.yoruhime.data.remote.AyakaResponse
import com.aistudio.yoruhime.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val database: AppDatabase,
    private val geminiService: GeminiService = GeminiService()
) {
    val messagesFlow: Flow<List<ChatMessageEntity>> = database.chatDao().getAllMessagesFlow()
    val bondFlow: Flow<UserBondEntity?> = database.bondDao().getBondFlow()

    suspend fun getBond(): UserBondEntity {
        return database.bondDao().getBond() ?: UserBondEntity().also {
            database.bondDao().insertOrUpdateBond(it)
        }
    }

    suspend fun sendUserMessage(
        userText: String,
        isOnlineMode: Boolean,
        apiKey: String,
        imageBitmap: Bitmap? = null
    ): AyakaResponse {
        // 1. Save user message to Room
        database.chatDao().insertMessage(
            ChatMessageEntity(
                text = userText,
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
        )

        val currentBond = getBond()
        var response: AyakaResponse? = null

        // 2. Try online generation if enabled and key provided
        if (isOnlineMode && apiKey.isNotBlank()) {
            try {
                response = geminiService.generateReply(
                    apiKey = apiKey,
                    prompt = userText,
                    imageBitmap = imageBitmap
                )
            } catch (e: Exception) {
                // Online failed (e.g. no connection, 429 quota exhaustion) -> seamless fallback
                response = null
            }
        }

        // 3. Seamless Offline Fallback
        if (response == null) {
            val fallback = if (imageBitmap != null) {
                OfflineDialogueBank.getCameraVisionReaction(isFrontCamera = false)
            } else {
                OfflineDialogueBank.generateReply(userText, currentBond.bondLevel)
            }
            response = AyakaResponse(
                emotion = fallback.emotion,
                text = fallback.text,
                isOnline = false
            )
        }

        // 4. Save Ayaka response to Room
        database.chatDao().insertMessage(
            ChatMessageEntity(
                text = response.text,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                emotionTag = response.emotion,
                isOffline = !response.isOnline
            )
        )

        // 5. Add Bond EXP & Level up
        val gainedExp = if (response.isOnline) 15 else 10
        val newExp = currentBond.bondExp + gainedExp
        val neededForNext = currentBond.getNextLevelExp()
        val newLevel = if (newExp >= neededForNext) currentBond.bondLevel + 1 else currentBond.bondLevel

        database.bondDao().insertOrUpdateBond(
            currentBond.copy(
                bondExp = newExp,
                bondLevel = newLevel,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        )

        return response
    }

    suspend fun recordTouchInteraction(zone: String): AyakaResponse {
        val reaction = OfflineDialogueBank.getTouchReaction(zone)
        val response = AyakaResponse(
            emotion = reaction.emotion,
            text = reaction.text,
            isOnline = false
        )

        database.chatDao().insertMessage(
            ChatMessageEntity(
                text = reaction.text,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                emotionTag = reaction.emotion,
                isOffline = true
            )
        )

        val currentBond = getBond()
        val newExp = currentBond.bondExp + reaction.expGain
        val neededForNext = currentBond.getNextLevelExp()
        val newLevel = if (newExp >= neededForNext) currentBond.bondLevel + 1 else currentBond.bondLevel

        database.bondDao().insertOrUpdateBond(
            currentBond.copy(
                bondExp = newExp,
                bondLevel = newLevel,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        )

        return response
    }

    suspend fun recordStudySessionComplete(minutes: Int): AyakaResponse {
        val currentBond = getBond()
        val bonusExp = minutes * 2
        val newTotalMins = currentBond.totalStudyMinutes + minutes
        val newExp = currentBond.bondExp + bonusExp
        val newLevel = if (newExp >= currentBond.getNextLevelExp()) currentBond.bondLevel + 1 else currentBond.bondLevel

        database.bondDao().insertOrUpdateBond(
            currentBond.copy(
                bondExp = newExp,
                bondLevel = newLevel,
                totalStudyMinutes = newTotalMins
            )
        )

        val text = "You completed $minutes minutes of focused study! Hmph... I won't lie, I'm slightly impressed. Drink some tea and rest your eyes."
        val response = AyakaResponse("smile", text, isOnline = false)

        database.chatDao().insertMessage(
            ChatMessageEntity(
                text = text,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                emotionTag = "smile",
                isOffline = true
            )
        )

        return response
    }

    suspend fun recordStudyInterrupted(): AyakaResponse {
        val text = "Hey! Did you just switch apps in the middle of your focus block?! Do you take your goals as a joke? Get back here and finish!"
        val response = AyakaResponse("angry", text, isOnline = false)

        database.chatDao().insertMessage(
            ChatMessageEntity(
                text = text,
                isFromUser = false,
                timestamp = System.currentTimeMillis(),
                emotionTag = "angry",
                isOffline = true
            )
        )

        return response
    }

    suspend fun updateBedtimeConfig(phrase: String, hour: Int, minute: Int) {
        val currentBond = getBond()
        database.bondDao().insertOrUpdateBond(
            currentBond.copy(
                customBedtimePhrase = phrase,
                bedtimeHour = hour,
                bedtimeMinute = minute
            )
        )
    }

    suspend fun clearHistory() {
        database.chatDao().clearAllMessages()
    }
}
