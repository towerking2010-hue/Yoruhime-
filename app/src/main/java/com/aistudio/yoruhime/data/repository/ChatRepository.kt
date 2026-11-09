package com.aistudio.yoruhime.data.repository

import android.graphics.Bitmap
import com.aistudio.yoruhime.data.local.AppDatabase
import com.aistudio.yoruhime.data.local.ChatMessageEntity
import com.aistudio.yoruhime.data.local.OfflineDialogueBank
import com.aistudio.yoruhime.data.local.InCharacterResponse
import com.aistudio.yoruhime.data.local.UserBondEntity
import com.aistudio.yoruhime.data.local.MemoryNoteEntity
import com.aistudio.yoruhime.data.remote.AyakaResponse
import com.aistudio.yoruhime.data.remote.OpenRouterService
import com.aistudio.yoruhime.data.remote.ConversationTurn
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val database: AppDatabase,
    private val openRouterService: OpenRouterService = OpenRouterService()
) {
    val messagesFlow: Flow<List<ChatMessageEntity>> = database.chatDao().getAllMessagesFlow()
    val bondFlow: Flow<UserBondEntity?> = database.bondDao().getBondFlow()
    val memoriesFlow: Flow<List<MemoryNoteEntity>> = database.memoryDao().getAllMemoriesFlow()

    suspend fun getBond(): UserBondEntity {
        return database.bondDao().getBond() ?: UserBondEntity().also {
            database.bondDao().insertOrUpdateBond(it)
        }
    }

    suspend fun sendUserMessage(
        userText: String,
        isOnlineMode: Boolean,
        apiKey: String,
        imageBitmap: Bitmap? = null,
        isFrontCamera: Boolean = false
    ): AyakaResponse {
        // Persist only facts the user explicitly asks YoruHime to remember.
        updateExplicitMemory(userText)

        val memoryNotes = database.memoryDao()
            .getRelevantMemories(limit = 12)
            .map { it.content }

        // Load the latest exchange history before saving this turn.
        // ChatDao returns newest-first; the model needs chronological order.
        val conversationHistory = database.chatDao()
            .getRecentMessages(limit = 12)
            .asReversed()
            .map { message ->
                ConversationTurn(
                    role = if (message.isFromUser) "user" else "assistant",
                    content = message.text
                )
            }

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

        // 2. Try OpenRouter generation if cloud mode is enabled and a key is provided
        if (isOnlineMode && apiKey.isNotBlank()) {
            try {
                response = openRouterService.generateReply(
                    apiKey = apiKey,
                    prompt = userText,
                    imageBitmap = imageBitmap,
                    history = conversationHistory,
                    memoryNotes = memoryNotes
                )
            } catch (e: Exception) {
                // OpenRouter failed (network, auth, rate limit, or provider error) -> seamless offline fallback
                response = null
            }
        }

        // 3. Honest offline fallback: never pretend to have analyzed an image locally.
        if (response == null) {
            val cameraLabel = if (isFrontCamera) "front-camera" else "rear-camera"
            val fallback = if (imageBitmap != null) {
                InCharacterResponse(
                    emotion = "normal",
                    text = "I can see that you sent an image, but I cannot actually inspect it while cloud vision is unavailable. Turn on Cloud mode and configure your API key if you want me to examine this $cameraLabel snapshot."
                )
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

    suspend fun clearMemories() {
        database.memoryDao().clearMemories()
    }

    suspend fun forgetMemory(content: String) {
        database.memoryDao().forgetMemory(content)
    }

    private suspend fun updateExplicitMemory(userText: String) {
        val rememberMatch = Regex(
            "^\\s*(?:remember that|remember:)\\s*(.+)$",
            RegexOption.IGNORE_CASE
        ).find(userText)

        if (rememberMatch != null) {
            val note = rememberMatch.groupValues[1].trim().take(240)
            if (note.isNotBlank()) {
                val existing = database.memoryDao().findMemory(note)
                database.memoryDao().saveMemory(
                    MemoryNoteEntity(
                        id = existing?.id ?: 0,
                        content = note,
                        category = "user_note",
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        importance = 3
                    )
                )
            }
            return
        }

        val forgetMatch = Regex(
            "^\\s*(?:forget that|forget:)\\s*(.+)$",
            RegexOption.IGNORE_CASE
        ).find(userText)
        if (forgetMatch != null) {
            val note = forgetMatch.groupValues[1].trim()
            if (note.isNotBlank()) database.memoryDao().forgetMemory(note)
        }
    }
}
