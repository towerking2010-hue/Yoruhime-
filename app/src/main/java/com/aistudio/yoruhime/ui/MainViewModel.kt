package com.aistudio.yoruhime.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(
    context: Context,
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

    private val _mood = MutableStateFlow(YoruMoodState())
    val mood: StateFlow<YoruMoodState> = _mood.asStateFlow()

    private val _currentEmotion = MutableStateFlow(_mood.value.toLive2DExpression())
    val currentEmotion: StateFlow<String> = _currentEmotion.asStateFlow()

    private fun updateMood(emotion: String) {
        _mood.value = _mood.value.reactTo(emotion)
        _currentEmotion.value = emotion.lowercase().takeIf {
            it in setOf("normal", "smile", "blush", "happy", "sad", "angry")
        } ?: "normal"
    }

    // Cloud is selected automatically when the user has configured a key and Android
    // reports validated internet. Explicit manual mode selection is respected.
    private val _isOnlineMode = MutableStateFlow(false)
    private var automaticCloudSelection = true
    private val connectivityManager = context.applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun hasValidatedInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun refreshAutomaticCloudMode() {
        if (!automaticCloudSelection) return
        _isOnlineMode.value = _apiKey.value.isNotBlank() && hasValidatedInternet()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshAutomaticCloudMode()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            refreshAutomaticCloudMode()
        override fun onLost(network: Network) = refreshAutomaticCloudMode()
    }
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

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()
    private val sendMutex = Mutex()
    private val sendInFlight = AtomicBoolean(false)

    init {
        // Initial in-character check
        viewModelScope.launch {
            repository.getBond()
            val savedKey = withContext(Dispatchers.IO) { apiKeyStore.readApiKey() }
            _apiKey.value = savedKey
            refreshAutomaticCloudMode()
        }
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }.onFailure { it.printStackTrace() }
        // Gentle mood decay keeps expressions from remaining stuck indefinitely.
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _mood.value = _mood.value.decay()
                // Decay internal mood quietly; don't overwrite a selected/reply expression.
            }
        }
    }

    fun setEmotion(emotion: String) {
        val selected = emotion.lowercase().takeIf {
            it in setOf("normal", "smile", "blush", "happy", "sad", "angry")
        } ?: "normal"
        _mood.value = _mood.value.reactTo(selected)
        _currentEmotion.value = selected
    }

    fun toggleAudioMute() {
        ttsManager.toggleMute()
    }

    fun speakMessage(text: String, emotion: String, isCritical: Boolean = false) {
        ttsManager.speak(text, emotion, isCritical)
    }

    fun toggleOnlineMode(enabled: Boolean) {
        automaticCloudSelection = false
        _isOnlineMode.value = enabled
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        // Never leave Cloud selected after the user removes their key, even when
        // they previously chose a manual mode override.
        if (key.isBlank()) _isOnlineMode.value = false else refreshAutomaticCloudMode()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { apiKeyStore.saveApiKey(key) }
                .onFailure { it.printStackTrace() }
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

    fun sendMessage(
        userText: String,
        imageBitmap: Bitmap? = null,
        isFrontCamera: Boolean? = null
    ) {
        if (userText.isBlank() && imageBitmap == null) return
        // Ignore duplicate taps while a reply is already being generated. Without
        // this guard, the mutex queues a second identical user/assistant exchange.
        if (!sendInFlight.compareAndSet(false, true)) {
            imageBitmap?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
            return
        }

        viewModelScope.launch {
            try {
                sendMutex.withLock {
                    _isLoading.value = true
                    _sendError.value = null
                    try {
                        val response = repository.sendUserMessage(
                            userText = userText,
                            isOnlineMode = _isOnlineMode.value,
                            apiKey = _apiKey.value,
                            imageBitmap = imageBitmap,
                            isFrontCamera = isFrontCamera
                        )
                        // A failed Cloud request is returned as a local response. Switch the mode
                        // control back to Offline so subsequent messages do not keep retrying a
                        // slow/unavailable free endpoint until the user explicitly re-enables Cloud.
                        if (_isOnlineMode.value && !response.isOnline) {
                            _isOnlineMode.value = false
                            // Re-enter automatic selection after a real connectivity loss;
                            // do not loop-retry a reachable but temporarily failing provider.
                            if (!hasValidatedInternet()) automaticCloudSelection = true
                        }
                        updateMood(response.emotion)
                        ttsManager.speak(response.text, response.emotion, isCritical = false)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        _sendError.value = "That message couldn't be completed. Please try again."
                    } finally {
                        _isLoading.value = false
                    }
                }
            } finally {
                sendInFlight.set(false)
                // Release one-shot attachments even if this coroutine is cancelled while
                // waiting for the send mutex and never enters the repository.
                imageBitmap?.let { bitmap ->
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }
    }

    fun onModelTapped(zone: String) {
        viewModelScope.launch {
            val response = repository.recordTouchInteraction(zone)
            updateMood(response.emotion)
            // Voice response on touch if high-tier tsundere reaction
            if (response.emotion == "blush" || response.emotion == "angry") {
                ttsManager.speak(response.text, response.emotion, isCritical = false)
            }
        }
    }

    fun onStudyCompleted(minutes: Int) {
        viewModelScope.launch {
            val response = repository.recordStudySessionComplete(minutes)
            updateMood(response.emotion)
            // Critical Event: Pomodoro Timer Completed alert
            ttsManager.speak(response.text, response.emotion, isCritical = true)
        }
    }

    fun onAppSwitchedAwayDuringStudy() {
        viewModelScope.launch {
            val response = repository.recordStudyInterrupted()
            updateMood(response.emotion)
            // Critical Event: Pomodoro focus interruption warning
            ttsManager.speak(response.text, response.emotion, isCritical = true)
        }
    }

    fun triggerTestMidnightAlert(context: Context) {
        val currentPhrase = userBond.value?.customBedtimePhrase?.ifBlank { null }
            ?: BedtimeWorker.DEFAULT_CURFEW_PHRASE
        BedtimeWorker.showBedtimeNotification(context, currentPhrase)
        updateMood("angry")
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
            updateMood("angry")
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

    fun editMemory(id: Long, content: String) {
        viewModelScope.launch {
            repository.editMemory(id, content)
        }
    }

    override fun onCleared() {
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        super.onCleared()
        // Release the lazily-created Android TTS engine when this ViewModel is permanently cleared.
        ttsManager.release()
    }

    class Factory(
        private val context: Context,
        private val repository: ChatRepository,
        private val ttsManager: AyakaTtsManager,
        private val apiKeyStore: SecureApiKeyStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(context, repository, ttsManager, apiKeyStore) as T
        }
    }
}
