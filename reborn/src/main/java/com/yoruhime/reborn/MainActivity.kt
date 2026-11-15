package com.yoruhime.reborn

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.content.Intent
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    val vault = remember { ApiKeyVault(context) }
    val engine = remember { AyakaEngine(store, vault) }
    val scope = rememberCoroutineScope()
    var voiceEnabled by rememberSaveable { mutableStateOf(true) }
    var draft by rememberSaveable { mutableStateOf("") }
    val tts = remember { TextToSpeech(context) { } }
    val recognizer = remember { if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null }
    val micPermission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        })
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) = Unit
            override fun onResults(results: Bundle?) { draft = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty() }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.destroy() }
    }
    DisposableEffect(tts) { onDispose { tts.stop(); tts.shutdown() } }
    var destination by rememberSaveable { mutableStateOf(Destination.HOME) }
    var messages by remember { mutableStateOf(store.messages().ifEmpty { listOf(ChatLine("assistant", "Good evening. You finally came back. Have you eaten yet?")) }) }
    var sending by remember { mutableStateOf(false) }
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
            bottomBar = { NavigationDock(destination) { destination = it } }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                TopBar(destination)
                when (destination) {
                    Destination.HOME -> HomeScreen(
                        messages = messages,
                        draft = draft,
                        onDraft = { draft = it },
                        sending = sending,
                        onSpeak = { messages.lastOrNull { it.role == "assistant" }?.content?.let { text -> if (voiceEnabled) { tts.language = Locale.US; tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ayaka-reply") } } },
                        onVoiceInput = { micPermission.launch(android.Manifest.permission.RECORD_AUDIO) },
                        onClear = { store.clearMessages(); messages = emptyList() },
                        onSend = {
                            val value = draft.trim()
                            if (value.isNotEmpty() && !sending) {
                                draft = ""
                                messages = messages + ChatLine("user", value)
                                store.addMessage("user", value)
                                sending = true
                                scope.launch {
                                    val answer = engine.reply(value)
                                    store.addMessage("assistant", answer)
                                    messages = messages + ChatLine("assistant", answer)
                                    sending = false
                                }
                            }
                        },
                        onCamera = { camera.launch(null) }
                    )
                    Destination.MEMORIES -> MemoriesScreen()
                    Destination.CARE -> CareScreen()
                    Destination.SETTINGS -> SettingsScreen(vault, voiceEnabled) { voiceEnabled = it }
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
                                showPhoto = false
                                sending = true
                                scope.launch {
                                    val answer = engine.replyAboutImage(question, image)
                                    store.addMessage("assistant", answer)
                                    messages = messages + answer
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

@Composable
private fun HomeScreen(
    messages: List<ChatLine>, draft: String, onDraft: (String) -> Unit,
    sending: Boolean, onSpeak: (String) -> Unit, onVoiceInput: () -> Unit, onClear: () -> Unit, onSend: () -> Unit, onCamera: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Character stage: intentionally a new component boundary; no legacy Live2D/WebView is reused.
        Box(
            Modifier.fillMaxWidth().weight(0.46f).clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF282343), Color(0xFF171A2D), Ink)))
                .border(1.dp, Color(0xFF51456F), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(116.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF554477), Color(0xFF22243D))))
                        .border(1.dp, Violet.copy(alpha = .7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("綾香", color = Lilac, fontSize = 38.sp, fontWeight = FontWeight.Light)
                }
                Text("AYAKA TŌJŌ", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("I'm right here.", color = Lilac, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF80D9B2)))
                    Text("Ready to talk", color = Muted, fontSize = 11.sp)
                }
            }
            Row(Modifier.align(Alignment.TopEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniAction(Icons.Rounded.CameraAlt, "Camera", onCamera)
                MiniAction(Icons.Rounded.Tune, "Mood") { }
            }
            Text("CHARACTER STAGE", Modifier.align(Alignment.BottomStart).padding(16.dp), color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
        }
        Column(
            Modifier.weight(0.54f).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Panel).border(1.dp, Color(0xFF34334E), RoundedCornerShape(28.dp)).padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("YOUR CONVERSATION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Talk with Ayaka", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { messages.lastOrNull()?.let(onSpeak) }, enabled = messages.isNotEmpty()) { Icon(Icons.Rounded.VolumeUp, "Speak latest message", tint = Muted) }
            }
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(messages) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.role == "user") Arrangement.End else Arrangement.Start) {
                        Surface(
                            color = if (message.role == "user") Color(0xFF46366D) else Panel2,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Text(message.content, Modifier.padding(12.dp), color = White, fontSize = 13.sp)
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onCamera, modifier = Modifier.size(44.dp).background(Panel2, CircleShape)) {
                    Icon(Icons.Rounded.PhotoCamera, "Open camera", tint = Lilac)
                }
                IconButton(onClick = onVoiceInput, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Mic, "Speak to Ayaka", tint = Lilac)
                }
                OutlinedTextField(
                    value = draft, onValueChange = onDraft, modifier = Modifier.weight(1f),
                    placeholder = { Text("Say something to Ayaka…", color = Muted, fontSize = 12.sp) },
                    singleLine = true, shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF393750),
                        focusedTextColor = White, unfocusedTextColor = White, cursorColor = Violet
                    )
                )
                IconButton(onClick = onSend, enabled = !sending, modifier = Modifier.size(46.dp).background(Violet, CircleShape)) {
                    if (sending) CircularProgressIndicator(Modifier.size(19.dp), color = Ink, strokeWidth = 2.dp)
                    else Icon(Icons.Rounded.Send, "Send", tint = Ink)
                }
            }
        }
    }
}

@Composable
private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color(0xAA111426), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x665E527D))) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = Lilac, modifier = Modifier.size(15.dp))
            Text(label, color = White, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MemoriesScreen() {
    val context = LocalContext.current
    val store = remember { RebornStore(context) }
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
private fun CareScreen() {
    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(25 * 60) }
    var running by rememberSaveable { mutableStateOf(false) }
    var checkInOpen by remember { mutableStateOf(false) }
    var selectedMood by rememberSaveable { mutableStateOf("Okay") }
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
        CareCard(Icons.Rounded.Nightlight, "Wind down", "Bedtime reminders are not yet enabled in this foundation build.", "Coming soon")
    }
    if (checkInOpen) {
        AlertDialog(
            onDismissRequest = { checkInOpen = false },
            title = { Text("How are you feeling?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Great", "Good", "Okay", "Tired", "Stressed", "Sad").forEach { mood ->
                        Row(Modifier.fillMaxWidth().clickable { selectedMood = mood; checkInOpen = false }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMood == mood, onClick = { selectedMood = mood; checkInOpen = false })
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
    var configured by remember { mutableStateOf(vault.read() != null) }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("PERSONAL SPACE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Settings", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("FREE AI CONNECTION", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("OpenRouter API key") }, placeholder = { Text("sk-or-…") },
            singleLine = true, shape = RoundedCornerShape(16.dp)
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
        Text("YoruHime Reborn · Foundation build", color = Muted, fontSize = 11.sp)
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
