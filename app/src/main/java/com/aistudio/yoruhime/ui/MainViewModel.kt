package com.aistudio.yoruhime.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.yoruhime.data.local.ChatMessageEntity
import com.aistudio.yoruhime.data.local.UserBondEntity
import com.aistudio.yoruhime.data.repository.ChatRepository
import com.aistudio.yoruhime.service.BedtimeWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    val messages: StateFlow<List<ChatMessageEntity>> = repository.messagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBond: StateFlow<UserBondEntity?> = repository.bondFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Post friendly in-character greeting on first launch if chat is empty
        viewModelScope.launch {
            val bond = repository.getBond()
            if (messages.value.isEmpty()) {
                val greeting = if (bond.bondLevel > 1) {
                    "You're back? Don't just stand there staring, sit down. Did you eat proper food today?"
                } else {
                    "Hmph, you're finally here. I prepared your schedule for today. Make sure you don't slack off on my watch!"
                }
                _currentEmotion.value = "smile"
            }
        }
    }

    fun setEmotion(emotion: String) {
        _currentEmotion.value = emotion
    }

    fun toggleOnlineMode(enabled: Boolean) {
        _isOnlineMode.value = enabled
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
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
            val response = repository.sendUserMessage(
                userText = userText,
                isOnlineMode = _isOnlineMode.value,
                apiKey = _apiKey.value,
                imageBitmap = imageBitmap
            )
            _currentEmotion.value = response.emotion
            _isLoading.value = false
        }
    }

    fun onModelTapped(zone: String) {
        viewModelScope.launch {
            val response = repository.recordTouchInteraction(zone)
            _currentEmotion.value = response.emotion
        }
    }

    fun onStudyCompleted(minutes: Int) {
        viewModelScope.launch {
            val response = repository.recordStudySessionComplete(minutes)
            _currentEmotion.value = response.emotion
        }
    }

    fun onAppSwitchedAwayDuringStudy() {
        viewModelScope.launch {
            val response = repository.recordStudyInterrupted()
            _currentEmotion.value = response.emotion
        }
    }

    fun triggerTestMidnightAlert(context: Context) {
        BedtimeWorker.showBedtimeNotification(context)
        _currentEmotion.value = "angry"
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    class Factory(private val repository: ChatRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
