package com.aistudio.yoruhime.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.yoruhime.data.local.ChatMessageEntity
import com.aistudio.yoruhime.data.local.MemoryNoteEntity
import com.aistudio.yoruhime.data.local.UserBondEntity
import com.aistudio.yoruhime.data.repository.ChatRepository
import com.aistudio.yoruhime.data.local.SecureApiKeyStore
import com.aistudio.yoruhime.service.BedtimeWorker
import com.aistudio.yoruhime.ui.audio.AyakaTtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ChatRepository,
    private val ttsManager: AyakaTtsManager,
    private val apiKeyStore: SecureApiKeyStore
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.messagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBond: StateFlow<UserBondEntity?> = repository.bondFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val memories: StateFlow<List<MemoryNoteEntity>> = repository.memoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentEmotion = MutableStateFlow("normal")
    val currentEmotion: StateFlow<String> = _currentEmotion.asStateFlow()

    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isCameraDockVisible = MutableStateFlow(false)
    val isCameraDockVisible: StateFlow<Boolean> = _isCameraDockVisible.asStateFlow()

    private val _isPomodoroVisible = MutableStateFlow(false)
    val isPomodoroVisible: StateFlow<Boolean> = _isPomodoroVisible.asStateFlow()

    private val _isArCameraActive = MutableStateFlow(false)
    val isArCameraActive: StateFlow<Boolean> = _isArCameraActive.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false) // Default to back camera for world-feed
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    val isAudioMuted: StateFlow<Boolean> = ttsManager.isMuted
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Initial in-character check
        viewModelScope.launch {
            repository.getBond()
            val savedKey = withContext(Dispatchers.IO) { apiKeyStore.readApiKey() }
            _apiKey.value = savedKey
        }
    }

    fun setEmotion(emotion: String) {
        _currentEmotion.value = emotion
    }

    fun toggleAudioMute() {
        ttsManager.toggleMute()
    }

    fun speakMessage(text: String, emotion: String, isCritical: Boolean = false) {
        ttsManager.speak(text, emotion, isCritical)
    }

    fun toggleOnlineMode(enabled: Boolean) {
        _isOnlineMode.value = enabled
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        viewModelScope.launch(Dispatchers.IO) {
            apiKeyStore.saveApiKey(key)
        }
    }

    fun toggleCameraDock(visible: Boolean) {
        _isCameraDockVisible.value = visible
    }

    fun togglePomodoro(visible: Boolean) {
        _isPomodoroVisible.value = visible
    }

    fun toggleArCamera(enabled: Boolean? = null) {
        _isArCameraActive.value = enabled ?: !_isArCameraActive.value
    }

    fun toggleCameraFacing() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun sendMessage(userText: String, imageBitmap: Bitmap? = null) {
        if (userText.isBlank() && imageBitmap == null) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.sendUserMessage(
                    userText = userText,
                    isOnlineMode = _isOnlineMode.value,
                    apiKey = _apiKey.value,
                    imageBitmap = imageBitmap
                )
                _currentEmotion.value = response.emotion

                // Auto voice output for high-tier tsundere emotional states (blush, angry)
                if (response.emotion == "blush" || response.emotion == "angry") {
                    ttsManager.speak(response.text, response.emotion, isCritical = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onModelTapped(zone: String) {
        viewModelScope.launch {
            val response = repository.recordTouchInteraction(zone)
            _currentEmotion.value = response.emotion
            // Voice response on touch if high-tier tsundere reaction
            if (response.emotion == "blush" || response.emotion == "angry") {
                ttsManager.speak(response.text, response.emotion, isCritical = false)
            }
        }
    }

    fun onStudyCompleted(minutes: Int) {
        viewModelScope.launch {
            val response = repository.recordStudySessionComplete(minutes)
            _currentEmotion.value = response.emotion
            // Critical Event: Pomodoro Timer Completed alert
            ttsManager.speak(response.text, response.emotion, isCritical = true)
        }
    }

    fun onAppSwitchedAwayDuringStudy() {
        viewModelScope.launch {
            val response = repository.recordStudyInterrupted()
            _currentEmotion.value = response.emotion
            // Critical Event: Pomodoro focus interruption warning
            ttsManager.speak(response.text, response.emotion, isCritical = true)
        }
    }

    fun triggerTestMidnightAlert(context: Context) {
        val currentPhrase = userBond.value?.customBedtimePhrase?.ifBlank { null }
            ?: BedtimeWorker.DEFAULT_CURFEW_PHRASE
        BedtimeWorker.showBedtimeNotification(context, currentPhrase)
        _currentEmotion.value = "angry"
        // Critical Event: Midnight / Curfew Lock warning
        ttsManager.speak(
            currentPhrase,
            "angry",
            isCritical = true
        )
    }

    fun updateBedtimeConfig(context: Context, phrase: String, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateBedtimeConfig(phrase, hour, minute)
            BedtimeWorker.scheduleCurfew(context, hour, minute)
        }
    }

    fun previewCustomPhrase(phrase: String) {
        if (phrase.isNotBlank()) {
            _currentEmotion.value = "angry"
            ttsManager.speak(phrase, "angry", isCritical = true)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun forgetMemory(content: String) {
        viewModelScope.launch {
            repository.forgetMemory(content)
        }
    }

    fun clearMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    class Factory(
        private val repository: ChatRepository,
        private val ttsManager: AyakaTtsManager,
        private val apiKeyStore: SecureApiKeyStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository, ttsManager, apiKeyStore) as T
        }
    }
}
