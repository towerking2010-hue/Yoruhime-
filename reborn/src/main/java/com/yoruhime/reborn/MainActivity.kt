package com.yoruhime.reborn

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.content.Intent
import android.app.TimePickerDialog
import android.view.WindowManager
import android.widget.Toast
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.Dialog

private val Ink = Color(0xFF090B16)
private val Panel = Color(0xFF14182A)
private val Panel2 = Color(0xFF1C2138)
private val Violet = Color(0xFFB69AFF)
private val Lilac = Color(0xFFE0D4FF)
private val Muted = Color(0xFF9299B4)
private val White = Color(0xFFF6F3FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 11, 22)
        window.navigationBarColor = android.graphics.Color.rgb(9, 11, 22)
        window.decorView.systemUiVisibility = 0
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent { YoruHimeApp() }
    }
}

private enum class Destination(val label: String) {
    HOME("Home"), MEMORIES("Memories"), CARE("Care"), SETTINGS("Settings")
}

@Composable
private fun YoruHimeApp() {
    val context = LocalContext.current
    val store = remember { RebornStore(context) }
    DisposableEffect(store) { onDispose { store.close() } }
    val vault = remember { ApiKeyVault(context) }
    val engine = remember { AyakaEngine(store, vault, context) }
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("yoruhime_settings", android.content.Context.MODE_PRIVATE) }
    var voiceEnabled by rememberSaveable { mutableStateOf(preferences.getBoolean("voice_replies_enabled", true)) }
    var voicePitch by rememberSaveable { mutableFloatStateOf(preferences.getFloat("voice_pitch", 1.06f)) }
    var voiceRate by rememberSaveable { mutableFloatStateOf(preferences.getFloat("voice_rate", 0.94f)) }
    var kokoroVoiceId by rememberSaveable { mutableStateOf(preferences.getString("kokoro_voice_id", "bf_emma") ?: "bf_emma") }
    var kokoroInstalled by remember { mutableStateOf(KokoroVoice.isInstalled(context)) }
    var kokoroDownloading by remember { mutableStateOf(false) }
    var kokoroProgress by remember { mutableIntStateOf(0) }
    var kokoroStatus by remember { mutableStateOf(if (kokoroInstalled) "Offline Kokoro voice installed" else "Android system voice active") }
    var draft by rememberSaveable { mutableStateOf("") }
    // Voice services are created only after the user asks to use them. Some Android
    // builds have missing or unstable speech providers; that must never block startup.
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var avatarSpeaking by remember { mutableStateOf(false) }
    var avatarViseme by remember { mutableStateOf("aa") }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun startVoiceRecognition() {
        try {
            val activeRecognizer = recognizer ?: run {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Toast.makeText(context, "Voice recognition is unavailable on this device.", Toast.LENGTH_SHORT).show()
                    return
                }
                SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
            }
            activeRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Please try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice input."
                        else -> "Voice input didn't work. Please try again."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                override fun onResults(results: Bundle?) {
                    draft = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                }
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            activeRecognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            })
        } catch (error: Throwable) {
            Toast.makeText(context, "Voice input is unavailable right now.", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) startVoiceRecognition()
        else Toast.makeText(context, "Microphone permission is needed for voice input.", Toast.LENGTH_SHORT).show()
    }
    DisposableEffect(recognizer) { onDispose { runCatching { recognizer?.destroy() } } }
    DisposableEffect(tts) {
        onDispose { runCatching { tts?.stop(); tts?.shutdown() } }
    }
    fun speakWithSystemTts(text: String) {
        try {
            fun configureAndSpeak(speaker: TextToSpeech) {
                speaker.language = Locale.US
                speaker.setPitch(voicePitch)
                speaker.setSpeechRate(voiceRate)
                speaker.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnMainThread { avatarSpeaking = true; avatarViseme = "aa" }
                    }
                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        val safeStart = start.coerceIn(0, text.length)
                        val safeEnd = end.coerceIn(safeStart, text.length)
                        runOnMainThread { avatarViseme = inferAyakaViseme(text.substring(safeStart, safeEnd)) }
                    }
                    override fun onDone(utteranceId: String?) {
                        runOnMainThread { avatarSpeaking = false; avatarViseme = "neutral" }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { runOnMainThread { avatarSpeaking = false; avatarViseme = "neutral" } }
                    override fun onError(utteranceId: String?, errorCode: Int) { runOnMainThread { avatarSpeaking = false; avatarViseme = "neutral" } }
                })
                if (speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ayaka-reply") == TextToSpeech.ERROR) avatarSpeaking = false
            }
            val existing = tts
            if (existing != null) configureAndSpeak(existing) else {
                lateinit var created: TextToSpeech
                created = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) runOnMainThread { configureAndSpeak(created) }
                    else runOnMainThread {
                        avatarSpeaking = false
                        Toast.makeText(context, "Speech output is unavailable right now.", Toast.LENGTH_SHORT).show()
                    }
                }
                tts = created
            }
        } catch (_: Throwable) {
            avatarSpeaking = false
            Toast.makeText(context, "Speech output is unavailable right now.", Toast.LENGTH_SHORT).show()
        }
    }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var messages by remember { mutableStateOf(store.messages().ifEmpty { listOf(ChatLine("assistant", "Good evening. You finally came back. Have you eaten yet?")) }) }
    var sending by remember { mutableStateOf(false) }
    var bondCount by remember { mutableIntStateOf(store.bondCount()) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var showPhoto by remember { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { photo = it; showPhoto = it != null }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Violet, onPrimary = Ink,
        onBackground = White, onSurface = White, secondary = Lilac
    )) {
        Scaffold(
            containerColor = Ink,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = { if (destination != Destination.HOME) NavigationDock(destination) { destination = it } }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (destination != Destination.HOME) TopBar(destination)
                when (destination) {
                    Destination.HOME -> HomeScreen(
                        messages = messages,
                        voiceEnabled = voiceEnabled,
                        bondCount = bondCount,
                        draft = draft,
                        onDraft = { draft = it },
                        sending = sending,
                        onSpeak = { text ->
                            if (!voiceEnabled) {
                                avatarSpeaking = false
                                avatarViseme = "neutral"
                            } else if (kokoroInstalled) {
                                KokoroVoice.stop()
                                scope.launch {
                                    try {
                                        val styleRate = when {
                                            text.contains("hmph", true) || text.contains("!", true) -> 1.06f
                                            text.contains("I'm here", true) || text.contains("it's okay", true) -> 0.94f
                                            else -> 1.0f
                                        }
                                        KokoroVoice.speak(
                                            context = context,
                                            text = text,
                                            voiceId = kokoroVoiceId,
                                            speakingRate = styleRate,
                                            onStart = { runOnMainThread { avatarSpeaking = true; avatarViseme = "aa" } },
                                            onViseme = { value -> runOnMainThread { avatarViseme = value } },
                                            onDone = { runOnMainThread { avatarSpeaking = false; avatarViseme = "neutral" } }
                                        )
                                    } catch (error: Throwable) {
                                        runOnMainThread {
                                            kokoroStatus = "Kokoro unavailable · using Android voice"
                                            avatarSpeaking = false
                                            speakWithSystemTts(text)
                                            Toast.makeText(context, "Kokoro couldn't run on this device. Using the installed Android voice instead.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else speakWithSystemTts(text)
                        },
                        avatarSpeaking = avatarSpeaking,
                        avatarViseme = avatarViseme,
                        onVoiceInput = { micPermission.launch(android.Manifest.permission.RECORD_AUDIO) },
                        onClear = { store.clearMessages(); messages = emptyList() },
                        onCare = { destination = Destination.CARE },
                        onMemories = { destination = Destination.MEMORIES },
                        onSettings = { destination = Destination.SETTINGS },
                        onSend = {
                            val value = draft.trim()
                            if (value.isNotEmpty() && !sending) {
                                draft = ""
                                messages = messages + ChatLine("user", value)
                                store.addMessage("user", value)
                                bondCount = store.bondCount()
                                sending = true
                                scope.launch {
                                    val answer = engine.reply(value)
                                    store.addMessage("assistant", answer)
                                    store.addBondPoint()
                                    bondCount = store.bondCount()
                                    messages = messages + ChatLine("assistant", answer)
                                    sending = false
                                }
                            }
                        },
                        onCamera = { camera.launch(null) }
                    )
                    Destination.MEMORIES -> MemoriesScreen(store)
                    Destination.CARE -> CareScreen(store)
                    Destination.SETTINGS -> SettingsScreen(
                        vault = vault,
                        voiceEnabled = voiceEnabled,
                        voicePitch = voicePitch,
                        voiceRate = voiceRate,
                        onVoiceEnabled = { enabled ->
                            voiceEnabled = enabled
                            preferences.edit().putBoolean("voice_replies_enabled", enabled).apply()
                        },
                        onVoicePitch = { value ->
                            voicePitch = value
                            preferences.edit().putFloat("voice_pitch", value).apply()
                        },
                        onVoiceRate = { value ->
                            voiceRate = value
                            preferences.edit().putFloat("voice_rate", value).apply()
                        },
                        kokoroInstalled = kokoroInstalled,
                        kokoroDownloading = kokoroDownloading,
                        kokoroProgress = kokoroProgress,
                        kokoroStatus = kokoroStatus,
                        kokoroVoiceId = kokoroVoiceId,
                        onKokoroVoice = { value ->
                            kokoroVoiceId = value
                            preferences.edit().putString("kokoro_voice_id", value).apply()
                        },
                        onInstallKokoro = {
                            if (!kokoroDownloading && !kokoroInstalled) {
                                kokoroDownloading = true
                                kokoroStatus = "Downloading encrypted-checksum-verified model…"
                                scope.launch {
                                    try {
                                        KokoroVoice.download(context) { progress ->
                                            runOnMainThread { kokoroProgress = progress }
                                        }
                                        kokoroInstalled = KokoroVoice.isInstalled(context)
                                        kokoroStatus = if (kokoroInstalled) "Kokoro installed · speech stays on this phone" else "Download did not complete"
                                    } catch (error: Throwable) {
                                        kokoroStatus = "Download failed: ${error.message ?: "unknown error"}"
                                    } finally {
                                        kokoroDownloading = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        if (showPhoto && photo != null) {
            Dialog(onDismissRequest = { showPhoto = false }) {
                Surface(shape = RoundedCornerShape(28.dp), color = Panel) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Your photo", fontWeight = FontWeight.Bold, color = White)
                        androidx.compose.foundation.Image(
                            bitmap = photo!!.asImageBitmap(), contentDescription = "Camera capture",
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        )
                        Text("Nothing is uploaded unless you tap Ask Ayaka. The image is resized before sending.", color = Muted, fontSize = 12.sp)
                        Button(onClick = {
                            val image = photo
                            if (image != null && !sending) {
                                val question = "Please describe what you notice in this photo."
                                messages = messages + ChatLine("user", question)
                                store.addMessage("user", question)
                                bondCount = store.bondCount()
                                showPhoto = false
                                sending = true
                                scope.launch {
                                    val answer = engine.replyAboutImage(question, image)
                                    store.addMessage("assistant", answer)
                                    store.addBondPoint()
                                    bondCount = store.bondCount()
                                    messages = messages + ChatLine("assistant", answer)
                                    sending = false
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !sending) { Text("Ask Ayaka about this photo") }
                        OutlinedButton(onClick = { showPhoto = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(destination: Destination) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Y O R U H I M E", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(
                when (destination) {
                    Destination.HOME -> "Your evening, together."
                    Destination.MEMORIES -> "Things worth remembering."
                    Destination.CARE -> "A little care, every day."
                    Destination.SETTINGS -> "Make this space yours."
                },
                color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold
            )
        }
        Box(Modifier.size(42.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsNone, null, tint = Lilac)
        }
    }
}

private fun runOnMainThread(action: () -> Unit) {
    android.os.Handler(android.os.Looper.getMainLooper()).post(action)
}

private fun inferAyakaViseme(chunk: String): String {
    val value = chunk.lowercase()
    return when {
        value.any { it == 'a' } -> "aa"
        value.any { it == 'e' } -> "ee"
        value.any { it == 'i' || it == 'y' } -> "ih"
        value.any { it == 'o' } -> "oh"
        value.any { it == 'u' } -> "ou"
        else -> "aa"
    }
}

private fun inferAyakaGesture(userText: String, replyText: String): String? {
    val user = userText.lowercase()
    val reply = replyText.lowercase()
    fun containsAny(value: String, terms: List<String>) = terms.any(value::contains)

    // Explicit requests always win; spontaneous gestures are deliberately restrained.
    if (containsAny(user, listOf("dance for me", "let's dance", "lets dance", "show me a dance"))) return "dance"
    if (containsAny(user, listOf("wave at me", "give me a wave"))) return "wave"
    if (containsAny(user, listOf("cross your arms", "fold your arms"))) return "armscrossed"
    if (containsAny(user, listOf("pout", "sulky", "tsundere"))) return "pout"
    if (containsAny(user, listOf("bow", "take a bow"))) return "bow"
    if (containsAny(user, listOf("say goodbye", "goodbye", "see you later", "see ya", "farewell"))) return "farewell"

    if (containsAny(user, listOf("good night", "goodnight", "see you tomorrow"))) return "farewell"
    if (containsAny(user, listOf("good morning", "good evening", "hello", "hey ayaka", "hi ayaka", "i'm back", "im back", "i am back", "missed you"))) return "wave"
    if (containsAny(user, listOf("you're cute", "you are cute", "beautiful", "pretty", "adorable", "blushing", "teasing you"))) return "shylookaway"
    if (containsAny(user, listOf("thank you", "thanks", "appreciate you", "i'm proud of you", "im proud of you"))) return "handonheart"
    if (containsAny(user, listOf("that's funny", "thats funny", "make me laugh", "joke"))) return "laugh"
    if (containsAny(user, listOf("what do you think", "hmm", "i wonder", "let me think"))) return "thinking"
    if (containsAny(user, listOf("yes", "exactly", "you're right", "you are right", "agreed"))) return "nod"
    if (user.contains("?")) return "headtilt"
    if (containsAny(user, listOf("annoyed", "irritated", "stop teasing", "don't tease", "dont tease")) ||
        containsAny(reply, listOf("hmph", "how rude", "ridiculous"))) return "armscrossed"
    return null
}

private fun inferAyakaMood(text: String): String {
    val value = text.lowercase()
    return when {
        listOf("angry", "annoyed", "irritated", "furious", "hate").any(value::contains) -> "angry"
        listOf("sad", "lonely", "depressed", "worried", "scared", "tired", "hurt").any(value::contains) -> "sad"
        listOf("wow", "whoa", "surprised", "unbelievable", "really?!").any(value::contains) -> "surprised"
        listOf("love", "thank you", "thanks", "happy", "great", "wonderful", "missed you").any(value::contains) -> "happy"
        listOf("calm", "relaxed", "okay", "fine").any(value::contains) -> "relaxed"
        else -> "neutral"
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    messages: List<ChatLine>, voiceEnabled: Boolean, bondCount: Int, draft: String, onDraft: (String) -> Unit,
    sending: Boolean, onSpeak: (String) -> Unit, avatarSpeaking: Boolean, avatarViseme: String, onVoiceInput: () -> Unit,
    onClear: () -> Unit, onCare: () -> Unit, onMemories: () -> Unit,
    onSettings: () -> Unit, onSend: () -> Unit, onCamera: () -> Unit
) {
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    var studioOpen by rememberSaveable { mutableStateOf(false) }
    var avatarMood by rememberSaveable { mutableStateOf("neutral") }
    var manualMood by rememberSaveable { mutableStateOf<String?>(null) }
    var avatarGesture by rememberSaveable { mutableStateOf("") }
    var gestureRequestId by rememberSaveable { mutableIntStateOf(0) }
    var lastAutoSpoken by remember { mutableStateOf(messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()) }
    LaunchedEffect(messages.lastOrNull()?.role, messages.lastOrNull()?.content, voiceEnabled) {
        val latest = messages.lastOrNull()
        if (latest?.role == "assistant" && latest.content != lastAutoSpoken) {
            lastAutoSpoken = latest.content
            if (voiceEnabled) onSpeak(latest.content)
        } else if (latest?.role != "assistant") {
            lastAutoSpoken = ""
        }
    }
    fun playGesture(name: String) { gestureRequestId += 1; avatarGesture = "$name#$gestureRequestId" }
    val latestAssistantText = messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()
    LaunchedEffect(sending, messages.lastOrNull()?.role, messages.lastOrNull()?.content) {
        if (sending) avatarMood = "listening"
        else if (messages.lastOrNull()?.role == "assistant") {
            val latestUserText = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
            val latestReply = messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()
            avatarMood = inferAyakaMood(latestUserText + " " + latestReply)
            // Local motion planner: gestures are chosen from conversational intent,
            // not fired for every message. The user can still trigger any gesture in Studio.
            inferAyakaGesture(latestUserText, latestReply)?.let(::playGesture)
        } else avatarMood = "neutral"
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF171329), Color(0xFF090B16), Color(0xFF090B16))))
    ) {
        // Ayaka owns the entire home surface. Conversation is a temporary layer,
        // never a permanently allocated half-screen panel.
        AyakaAvatar(Modifier.fillMaxSize(), mood = manualMood ?: avatarMood, speaking = avatarSpeaking, gesture = avatarGesture, viseme = avatarViseme)

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xA6090B16),
                    0.22f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to Color(0xE6090B16)
                )
            )
        )

        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Y O R U H I M E", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("A quiet place to return to.", color = Lilac, fontSize = 12.sp)
                }
                IconButton(
                    onClick = onMemories,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Panel.copy(alpha = 0.72f))
                ) { Icon(Icons.Rounded.Bookmarks, "Memories", tint = Lilac) }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { studioOpen = true },
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Panel.copy(alpha = 0.72f))
                ) { Icon(Icons.Rounded.AutoAwesome, "Animation studio", tint = Lilac) }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(Panel.copy(alpha = 0.72f))
                ) { Icon(Icons.Rounded.Settings, "Settings", tint = Lilac) }
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier.fillMaxWidth().padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "AYAKA TŌJŌ",
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text("I'm right here.", color = Lilac, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                Text(
                    "BOND  ·  $bondCount",
                    color = Muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Surface(
                color = Panel.copy(alpha = 0.88f),
                shape = RoundedCornerShape(30.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x665E527D)),
                shadowElevation = 12.dp
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onCamera,
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(Panel2)
                    ) { Icon(Icons.Rounded.PhotoCamera, "Camera", tint = Lilac) }
                    IconButton(
                        onClick = { chatOpen = true; onVoiceInput() },
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(Panel2)
                    ) { Icon(Icons.Rounded.Mic, "Speak to Ayaka", tint = Lilac) }
                    Button(
                        onClick = { chatOpen = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)
                    ) {
                        Icon(Icons.Rounded.Forum, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Talk to Ayaka", fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(
                        onClick = onCare,
                        modifier = Modifier.size(46.dp).clip(CircleShape).background(Panel2)
                    ) { Icon(Icons.Rounded.FavoriteBorder, "Care", tint = Lilac) }
                }
            }
        }
    }

    if (chatOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val listState = rememberLazyListState()
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
        }
        ModalBottomSheet(
            onDismissRequest = { chatOpen = false },
            sheetState = sheetState,
            containerColor = Panel,
            contentColor = White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
        ) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.82f).imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("A LITTLE TIME TOGETHER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("Talk with Ayaka", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            messages.lastOrNull { it.role == "assistant" }?.content?.let { spokenText ->
                                onSpeak(spokenText)
                            }
                        },
                        enabled = messages.any { it.role == "assistant" }
                    ) { Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Read latest reply aloud", tint = Lilac) }
                    IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteOutline, "Clear conversation", tint = Muted) }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { message ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (message.role == "user") Color(0xFF49366F) else Panel2,
                                shape = RoundedCornerShape(
                                    topStart = 20.dp, topEnd = 20.dp,
                                    bottomEnd = if (message.role == "user") 7.dp else 20.dp,
                                    bottomStart = if (message.role == "user") 20.dp else 7.dp
                                ),
                                modifier = Modifier.widthIn(max = 320.dp)
                            ) {
                                Text(message.content, Modifier.padding(horizontal = 15.dp, vertical = 12.dp), color = White, fontSize = 14.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                    if (sending) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(14.dp), color = Violet, strokeWidth = 2.dp)
                                Text("Ayaka is thinking…", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onVoiceInput,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Panel2)
                    ) { Icon(Icons.Rounded.Mic, "Voice input", tint = Lilac) }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = onDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Say something to Ayaka…", color = Muted, fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { if (!sending) onSend() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF393750),
                            focusedTextColor = White, unfocusedTextColor = White, cursorColor = Violet
                        )
                    )
                    IconButton(
                        onClick = onSend,
                        enabled = !sending && draft.isNotBlank(),
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(Violet)
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(18.dp), color = Ink, strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Rounded.Send, "Send message", tint = Ink)
                    }
                }
            }
        }
    }

    if (studioOpen) {
        ModalBottomSheet(
            onDismissRequest = { studioOpen = false },
            containerColor = Panel,
            contentColor = White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) }
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("AYAKA ANIMATION STUDIO", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Expressions", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Choose how Ayaka looks. Auto returns expression selection to conversation-based reactions.", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = manualMood == null, onClick = { manualMood = null }, label = { Text("Auto") })
                    FilterChip(selected = manualMood == "neutral", onClick = { manualMood = "neutral" }, label = { Text("Neutral") })
                    FilterChip(selected = manualMood == "happy", onClick = { manualMood = "happy" }, label = { Text("Happy") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = manualMood == "angry", onClick = { manualMood = "angry" }, label = { Text("Tsundere / Angry") })
                    FilterChip(selected = manualMood == "sad", onClick = { manualMood = "sad" }, label = { Text("Sad") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = manualMood == "surprised", onClick = { manualMood = "surprised" }, label = { Text("Surprised") })
                    FilterChip(selected = manualMood == "relaxed", onClick = { manualMood = "relaxed" }, label = { Text("Relaxed") })
                    FilterChip(selected = manualMood == "shy", onClick = { manualMood = "shy" }, label = { Text("Shy") })
                }
                Divider(color = Color(0xFF393750))
                Text("Gestures", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Tap a gesture to play it. Ayaka returns to her idle animation when it finishes.", color = Muted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { playGesture("wave"); studioOpen = false }) {
                        Icon(Icons.Rounded.WavingHand, null); Spacer(Modifier.width(6.dp)); Text("Wave")
                    }
                    Button(onClick = { playGesture("pout"); manualMood = "angry"; studioOpen = false }) {
                        Icon(Icons.Rounded.MoodBad, null); Spacer(Modifier.width(6.dp)); Text("Tsundere pout")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { playGesture("armscrossed"); studioOpen = false }) {
                        Icon(Icons.Rounded.Person, null); Spacer(Modifier.width(6.dp)); Text("Cross arms")
                    }
                    OutlinedButton(onClick = { playGesture("dance"); studioOpen = false }) {
                        Icon(Icons.Rounded.MusicNote, null); Spacer(Modifier.width(6.dp)); Text("Dance")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { playGesture("nod"); studioOpen = false }) { Text("Nod") }
                    OutlinedButton(onClick = { playGesture("headtilt"); studioOpen = false }) { Text("Head tilt") }
                    OutlinedButton(onClick = { playGesture("shrug"); studioOpen = false }) { Text("Shrug") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { playGesture("thinking"); studioOpen = false }) { Text("Thinking") }
                    OutlinedButton(onClick = { playGesture("handonheart"); studioOpen = false }) { Text("Hand on heart") }
                    OutlinedButton(onClick = { playGesture("bow"); studioOpen = false }) { Text("Bow") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { playGesture("laugh"); studioOpen = false }) { Text("Laugh") }
                    OutlinedButton(onClick = { playGesture("shylookaway"); manualMood = "shy"; studioOpen = false }) { Text("Shy") }
                    OutlinedButton(onClick = { playGesture("farewell"); studioOpen = false }) { Text("Farewell") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { avatarGesture = ""; manualMood = null; studioOpen = false }) {
                        Text("Back to idle")
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoriesScreen(store: RebornStore) {
    var note by rememberSaveable { mutableStateOf("") }
    var notes by remember { mutableStateOf(store.memories()) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("YOUR SHARED STORY", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Memories", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Only keep what you choose to remember. You stay in control.", color = Muted, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(note, { note = it }, Modifier.weight(1f), placeholder = { Text("Add a memory…") }, shape = RoundedCornerShape(18.dp))
            IconButton(onClick = { if (note.isNotBlank()) { store.addMemory(note.trim()); notes = store.memories(); note = "" } }) { Icon(Icons.Rounded.AddCircle, null, tint = Violet) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes, key = { it.first }) { item ->
                Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34334E))) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.BookmarkBorder, null, tint = Violet)
                        Text(item.second, Modifier.weight(1f).padding(horizontal = 10.dp), color = White, fontSize = 13.sp)
                        IconButton(onClick = { store.deleteMemory(item.first); notes = store.memories() }) { Icon(Icons.Rounded.DeleteOutline, null, tint = Muted) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareScreen(store: RebornStore) {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable { mutableStateOf(false) }
    var checkInOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var selectedMood by rememberSaveable { mutableStateOf(store.moodForToday() ?: "Okay") }
    var bedtimeLabel by remember { mutableStateOf(BedtimeScheduler.timeLabel(context)) }
    var pendingBedtime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            pendingBedtime?.let {
                BedtimeScheduler.schedule(context, it.first, it.second)
                bedtimeLabel = BedtimeScheduler.timeLabel(context)
            }
        } else {
            Toast.makeText(context, "Allow notifications to receive bedtime reminders.", Toast.LENGTH_SHORT).show()
        }
        pendingBedtime = null
    }
    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft -= 1
            if (secondsLeft <= 0) running = false
        }
    }
    val time = "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("YOUR ROUTINE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Care & connection", color = White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Surface(color = Panel, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF51456F))) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("FOCUS TOGETHER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(time, color = White, fontSize = 48.sp, fontWeight = FontWeight.Light)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 25, 45, 60).forEach { value ->
                        FilterChip(selected = minutes == value, onClick = { minutes = value; secondsLeft = value * 60; running = false }, label = { Text("${value}m") })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { if (secondsLeft == 0) secondsLeft = minutes * 60; running = !running }) {
                        Icon(if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp)); Text(if (running) "Pause" else "Start")
                    }
                    OutlinedButton(onClick = { running = false; secondsLeft = minutes * 60 }) { Text("Reset") }
                }
                Text(if (secondsLeft == 0) "Session complete. Well done." else "A gentle timer. No pressure, just progress.", color = Muted, fontSize = 12.sp)
            }
        }
        CareCard(Icons.Rounded.Favorite, "Bond", "A relationship that grows through real conversations.", "Keep talking")
        Surface(onClick = { checkInOpen = true }, color = Panel, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34334E))) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SelfImprovement, null, tint = Violet)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Daily check-in", color = White, fontWeight = FontWeight.SemiBold)
                    Text("How are you feeling today? Current: $selectedMood", color = Muted, fontSize = 11.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Muted)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34334E))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Nightlight, null, tint = Violet)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Bedtime reminder", color = White, fontWeight = FontWeight.SemiBold)
                        Text(if (bedtimeLabel == "Not set") "No reminder scheduled" else "Every day at $bedtimeLabel", color = Muted, fontSize = 11.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val savedParts = bedtimeLabel.split(":")
                        val initialHour = savedParts.getOrNull(0)?.toIntOrNull() ?: 23
                        val initialMinute = savedParts.getOrNull(1)?.toIntOrNull() ?: 0
                        TimePickerDialog(context, { _, hour, minute ->
                            if (BedtimeScheduler.needsNotificationPermission(context)) {
                                pendingBedtime = hour to minute
                                notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                BedtimeScheduler.schedule(context, hour, minute)
                                bedtimeLabel = BedtimeScheduler.timeLabel(context)
                            }
                        }, initialHour, initialMinute, true).show()
                    }) { Text(if (bedtimeLabel == "Not set") "Set reminder" else "Change time") }
                    if (bedtimeLabel != "Not set") OutlinedButton(onClick = { BedtimeScheduler.cancel(context); bedtimeLabel = "Not set" }) { Text("Cancel") }
                }
                Text("Uses an inexact daily alarm to save battery. Android may delay delivery in power-saving modes.", color = Muted, fontSize = 10.sp)
            }
        }
    }
    if (checkInOpen) {
        AlertDialog(
            onDismissRequest = { checkInOpen = false },
            title = { Text("How are you feeling?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Great", "Good", "Okay", "Tired", "Stressed", "Sad").forEach { mood ->
                        Row(Modifier.fillMaxWidth().clickable { selectedMood = mood; store.saveMood(mood); checkInOpen = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMood == mood, onClick = { selectedMood = mood; store.saveMood(mood); checkInOpen = false })
                            Text(mood, color = White)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { checkInOpen = false }) { Text("Close") } }
        )
    }
}
@Composable
private fun CareCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, action: String) {
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34334E))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Panel2), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Violet)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Text(action, color = Lilac, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsScreen(
    vault: ApiKeyVault,
    voiceEnabled: Boolean,
    voicePitch: Float,
    voiceRate: Float,
    onVoiceEnabled: (Boolean) -> Unit,
    onVoicePitch: (Float) -> Unit,
    onVoiceRate: (Float) -> Unit,
    kokoroInstalled: Boolean,
    kokoroDownloading: Boolean,
    kokoroProgress: Int,
    kokoroStatus: String,
    kokoroVoiceId: String,
    onKokoroVoice: (String) -> Unit,
    onInstallKokoro: () -> Unit
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var revealKey by rememberSaveable { mutableStateOf(false) }
    var configured by remember { mutableStateOf(vault.read() != null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("PERSONAL SPACE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Settings", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("FREE AI CONNECTION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("OpenRouter API key") }, placeholder = { Text("sk-or-…") },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton(onClick = { revealKey = !revealKey }) { Icon(if (revealKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle API key visibility") } }
        )
        Button(onClick = { if (apiKey.isNotBlank()) { vault.save(apiKey); apiKey = ""; configured = true } }, modifier = Modifier.fillMaxWidth()) { Text("Save encrypted API key") }
        Text(if (configured) "API key saved securely on this device." else "Offline mode · add your free OpenRouter key to enable cloud chat.", color = Muted, fontSize = 12.sp)
        TextButton(onClick = { vault.clear(); configured = false; apiKey = "" }) { Text("Remove API key") }
        Text("AYAKA'S VOICE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        SettingToggle("Voice replies", "Automatically speak each new reply. Tap the speaker to hear it again.", voiceEnabled, onVoiceEnabled)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kokoro · offline neural voice", color = White, fontWeight = FontWeight.SemiBold)
                Text("Optional one-time download (~295 MB). After installation, speech is generated locally without an internet connection or subscription. Android's built-in voice remains the fallback.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kokoroVoiceId == "bf_emma", onClick = { onKokoroVoice("bf_emma") }, label = { Text("Emma · British") })
                    FilterChip(selected = kokoroVoiceId == "bf_isabella", onClick = { onKokoroVoice("bf_isabella") }, label = { Text("Isabella · British") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kokoroVoiceId == "af_bella", onClick = { onKokoroVoice("af_bella") }, label = { Text("Bella · American") })
                }
                Text(kokoroStatus, color = if (kokoroInstalled) Violet else Muted, fontSize = 11.sp)
                if (kokoroDownloading) {
                    LinearProgressIndicator(progress = { kokoroProgress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("$kokoroProgress% downloaded", color = Muted, fontSize = 11.sp)
                } else if (!kokoroInstalled) {
                    Button(onClick = onInstallKokoro, modifier = Modifier.fillMaxWidth()) { Text("Download Kokoro · ~295 MB") }
                } else {
                    Text("Kokoro will be used automatically for Ayaka's replies. You can remove the model later in Android app storage.", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pitch · ${"%.2f".format(Locale.US, voicePitch)}", color = White, fontWeight = FontWeight.SemiBold)
                Slider(value = voicePitch, onValueChange = onVoicePitch, valueRange = 0.85f..1.25f)
                Text("Speaking pace · ${"%.2f".format(Locale.US, voiceRate)}", color = White, fontWeight = FontWeight.SemiBold)
                Slider(value = voiceRate, onValueChange = onVoiceRate, valueRange = 0.80f..1.10f)
                Text("These controls tune the Android system-voice fallback. Kokoro uses its own natural voice identity and selectable speaker profiles.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                TextButton(onClick = { onVoicePitch(1.06f); onVoiceRate(0.94f) }) { Text("Reset Ayaka voice") }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Privacy first", color = White, fontWeight = FontWeight.SemiBold)
                Text("Memory stays under your control. Camera capture starts only when you tap Camera.", color = Muted, fontSize = 12.sp)
            }
        }
        Text("Ayaka model credit: epic_noobster · Original VRM creator, license, expression, humanoid and physics metadata is bundled for future native support.", color = Muted, fontSize = 11.sp)
        Text("Model terms: author-only use · personal/non-profit only · credit required. Redistribution and modification are permitted by the embedded VRM metadata.", color = Muted, fontSize = 11.sp)
        Text("YoruHime Reborn · Preview build", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SettingToggle(title: String, detail: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(color = Panel, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
            Switch(checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun NavigationDock(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = Color(0xFF101323), contentColor = Lilac, tonalElevation = 0.dp) {
        val entries = listOf(
            Destination.HOME to Icons.Rounded.Home,
            Destination.MEMORIES to Icons.Rounded.Bookmarks,
            Destination.CARE to Icons.Rounded.FavoriteBorder,
            Destination.SETTINGS to Icons.Rounded.Settings
        )
        entries.forEach { (destination, icon) ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(icon, contentDescription = destination.label) },
                label = { Text(destination.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Ink, selectedTextColor = Lilac,
                    indicatorColor = Violet, unselectedIconColor = Muted, unselectedTextColor = Muted
                )
            )
        }
    }
}
