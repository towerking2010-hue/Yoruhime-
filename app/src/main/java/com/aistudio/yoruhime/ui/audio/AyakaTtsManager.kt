package com.aistudio.yoruhime.ui.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AyakaTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // The Android TTS engine can take time to initialize. Do not start it while
    // the user is merely opening YoruHime; initialize on the first spoken reply.
    private fun ensureEngine() {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.ENGLISH)
            }

            // Attempt to select an expressive high-pitch female voice if available
            try {
                val femaleVoice = tts?.voices?.firstOrNull { voice ->
                    voice.locale.language == Locale.ENGLISH.language &&
                            (voice.name.contains("female", ignoreCase = true) ||
                             voice.name.contains("#female", ignoreCase = true) ||
                             voice.name.contains("en-us-x-sfg", ignoreCase = true) ||
                             voice.name.contains("en-us-x-tpf", ignoreCase = true))
                }
                if (femaleVoice != null) {
                    tts?.voice = femaleVoice
                }
            } catch (e: Exception) {
                // Fall back to default engine voice
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })

            isInitialized = true
            pendingSpeech?.let { queued ->
                pendingSpeech = null
                speakNow(queued)
            }
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        if (newMuted) {
            stop()
        }
    }

    /**
     * Sentiment Pitch & Rate Matrix:
     * - "angry": Sharp scolding, elevated pitch and brisk tempo (pitch 1.25f, rate 1.15f)
     * - "blush": Flustered stammering, high frantic pitch (pitch 1.35f, rate 1.20f)
     * - "smile" / "happy": Warm mommy-mode, soft reassuring pacing (pitch 1.02f, rate 0.94f)
     * - "sad": Worried, tender, slower pacing (pitch 0.92f, rate 0.88f)
     * - "normal": Poised, dignified student council tone (pitch 1.08f, rate 1.00f)
     */
    fun speak(
        text: String,
        emotion: String,
        isCritical: Boolean = false
    ) {
        // Respect master mute unless a critical event explicitly overrides it.
        if (_isMuted.value && !isCritical) return
        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        val request = PendingSpeech(cleanText, emotion, isCritical)
        if (!isInitialized || tts == null) {
            pendingSpeech = request
            ensureEngine()
            return
        }
        speakNow(request)
    }

    private fun speakNow(request: PendingSpeech) {
        val (pitch, rate) = when (request.emotion.lowercase()) {
            "angry" -> Pair(1.26f, 1.16f)   // Sharp scolding
            "blush" -> Pair(1.36f, 1.22f)   // Flustered stuttering panic
            "smile", "happy" -> Pair(1.02f, 0.94f) // Warm maternal comfort
            "sad"   -> Pair(0.92f, 0.88f)   // Soft & worried
            else    -> Pair(1.08f, 1.00f)   // Dignified student council VP
        }

        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)

        val utteranceId = "ayaka_${System.currentTimeMillis()}"
        tts?.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            .replace(Regex("\\[(smile|blush|happy|sad|angry|normal)\\]", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\*.*?\\*"), "") // Strip action asterisks like *sigh* or *pats head*
            .replace(Regex("[\"']"), "")
            .trim()
    }

    fun stop() {
        pendingSpeech = null
        tts?.stop()
        _isSpeaking.value = false
    }

    fun release() {
        pendingSpeech = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
