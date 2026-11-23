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
    val engine = remember { AyakaEngine(store, vault) }
    val scope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences("yoruhime_settings", android.content.Context.MODE_PRIVATE) }
    var voiceEnabled by rememberSaveable { mutableStateOf(preferences.getBoolean("voice_replies_enabled", true)) }
    var draft by rememberSaveable { mutableStateOf("") }
    // Voice services are created only after the user asks to use them. Some Android
    // builds have missing or unstable speech providers; that must never block startup.
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var avatarSpeaking by remember { mutableStateOf(false) }
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
                        bondCount = bondCount,
                        draft = draft,
                        onDraft = { draft = it },
                        sending = sending,
                        onSpeak = { text ->
                            if (voiceEnabled) {
                                try {
                                    fun configureAndSpeak(speaker: TextToSpeech) {
                                        speaker.language = Locale.US
                                        speaker.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                            override fun onStart(utteranceId: String?) {
                                                runOnMainThread { avatarSpeaking = true }
                                            }
                                            override fun onDone(utteranceId: String?) {
                                                runOnMainThread { avatarSpeaking = false }
                                            }
                                            @Deprecated("Deprecated in Java")
                                            override fun onError(utteranceId: String?) {
                                                runOnMainThread { avatarSpeaking = false }
                                            }
                                            override fun onError(utteranceId: String?, errorCode: Int) {
                                                runOnMainThread { avatarSpeaking = false }
                                            }
                                        })
                                        val result = speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ayaka-reply")
                                        if (result == TextToSpeech.ERROR) avatarSpeaking = false
                                    }
                                    val existing = tts
                                    if (existing != null) {
                                        configureAndSpeak(existing)
                                    } else {
                                        lateinit var created: TextToSpeech
                                        created = TextToSpeech(context) { status ->
                                            if (status == TextToSpeech.SUCCESS) {
                                                runOnMainThread { configureAndSpeak(created) }
                                            } else {
                                                runOnMainThread {
                                                    avatarSpeaking = false
                                                    Toast.makeText(context, "Speech output is unavailable right now.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                        tts = created
                                    }
                                } catch (_: Throwable) {
                                    avatarSpeaking = false
                                    Toast.makeText(context, "Speech output is unavailable right now.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                avatarSpeaking = false
                            }
                        },
                        avatarSpeaking = avatarSpeaking,
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
                    Destination.SETTINGS -> SettingsScreen(vault, voiceEnabled) { enabled ->
                        voiceEnabled = enabled
                        preferences.edit().putBoolean("voice_replies_enabled", enabled).apply()
                    }
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
    messages: List<ChatLine>, bondCount: Int, draft: String, onDraft: (String) -> Unit,
    sending: Boolean, onSpeak: (String) -> Unit, avatarSpeaking: Boolean, onVoiceInput: () -> Unit,
    onClear: () -> Unit, onCare: () -> Unit, onMemories: () -> Unit,
    onSettings: () -> Unit, onSend: () -> Unit, onCamera: () -> Unit
) {
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    var avatarMood by rememberSaveable { mutableStateOf("neutral") }
    val latestAssistantText = messages.lastOrNull { it.role == "assistant" }?.content.orEmpty()
    LaunchedEffect(sending, messages.lastOrNull()?.role, messages.lastOrNull()?.content) {
        if (sending) avatarMood = "listening"
        else if (messages.lastOrNull()?.role == "assistant") {
            val latestUserText = messages.lastOrNull { it.role == "user" }?.content.orEmpty()
            avatarMood = inferAyakaMood(latestUserText)
        } else avatarMood = "neutral"
    }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF171329), Color(0xFF090B16), Color(0xFF090B16))))
    ) {
        // Ayaka owns the entire home surface. Conversation is a temporary layer,
        // never a permanently allocated half-screen panel.
        AyakaAvatar(Modifier.fillMaxSize(), mood = avatarMood, speaking = avatarSpeaking)

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
private fun SettingsScreen(vault: ApiKeyVault, voiceEnabled: Boolean, onVoiceEnabled: (Boolean) -> Unit) {
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
        SettingToggle("Voice replies", "Allow Ayaka to read the latest reply aloud.", voiceEnabled, onVoiceEnabled)
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
