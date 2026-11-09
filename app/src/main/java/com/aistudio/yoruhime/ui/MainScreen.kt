package com.aistudio.yoruhime.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.yoruhime.data.local.ChatMessageEntity
import com.aistudio.yoruhime.ui.camera.ARCameraBackground
import com.aistudio.yoruhime.ui.camera.CameraPreviewDock
import com.aistudio.yoruhime.ui.live2d.Live2DController
import com.aistudio.yoruhime.ui.live2d.Live2DStage
import com.aistudio.yoruhime.ui.pomodoro.PomodoroSession
import com.aistudio.yoruhime.ui.theme.BlushPink
import com.aistudio.yoruhime.ui.theme.GlassBorder
import com.aistudio.yoruhime.ui.theme.GlassDark
import com.aistudio.yoruhime.ui.theme.GlassSurface
import com.aistudio.yoruhime.ui.theme.LavenderPrimary
import com.aistudio.yoruhime.ui.theme.MidnightBlack
import com.aistudio.yoruhime.ui.theme.SlateBorder
import com.aistudio.yoruhime.ui.theme.SlateDark
import com.aistudio.yoruhime.ui.theme.SlateSurface
import com.aistudio.yoruhime.ui.theme.SoftGold
import com.aistudio.yoruhime.ui.theme.TextMuted
import com.aistudio.yoruhime.ui.theme.TextSilver
import com.aistudio.yoruhime.ui.theme.TextWhite
import com.aistudio.yoruhime.ui.theme.VioletAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentEmotion by viewModel.currentEmotion.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val userBond by viewModel.userBond.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val isOnlineMode by viewModel.isOnlineMode.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isPomodoroVisible by viewModel.isPomodoroVisible.collectAsState()
    val isArCameraActive by viewModel.isArCameraActive.collectAsState()
    val isCameraDockVisible by viewModel.isCameraDockVisible.collectAsState()
    val isFrontCamera by viewModel.isFrontCamera.collectAsState()
    val isAudioMuted by viewModel.isAudioMuted.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var attachedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var tempApiKey by remember { mutableStateOf(apiKey) }
    var showBedtimeDialog by remember { mutableStateOf(false) }
    var tempBedtimePhrase by remember {
        mutableStateOf(
            userBond?.customBedtimePhrase
                ?: "It's 11:00 AM curfew! Put your phone down right now and take a rest, you hopeless slacker!"
        )
    }
    var selectedHour by remember { mutableStateOf(userBond?.bedtimeHour ?: 11) }
    var selectedMinute by remember { mutableStateOf(userBond?.bedtimeMinute ?: 0) }

    val live2DController = remember { Live2DController() }
    val listState = rememberLazyListState()

    LaunchedEffect(apiKey) {
        if (!showApiKeyDialog) tempApiKey = apiKey
    }

    // Speech-to-text recording launcher for bedtime trigger phrase
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                tempBedtimePhrase = spokenText
                Toast.makeText(context, "Captured voice trigger phrase!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Voice input for chat using the device's installed speech-recognition service.
    val chatSpeechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) inputText = spokenText
        }
    }

    // Zero-permission Android Photo Picker for chat attachments
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                attachedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Scroll to bottom on new message and trigger Live2D speech bubble
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            val latest = messages.last()
            if (!latest.isFromUser) {
                live2DController.showSpeech(latest.text, latest.emotionTag)
            }
        }
    }

    // Animated dynamic mood aura border
    val auraColor by animateColorAsState(
        targetValue = when (currentEmotion) {
            "blush" -> BlushPink.copy(alpha = 0.55f)
            "angry" -> Color(0xFFEF4444).copy(alpha = 0.5f)
            "happy" -> SoftGold.copy(alpha = 0.5f)
            "smile" -> VioletAccent.copy(alpha = 0.45f)
            "sad"   -> Color(0xFF38BDF8).copy(alpha = 0.45f)
            else    -> LavenderPrimary.copy(alpha = 0.3f)
        },
        animationSpec = tween(600),
        label = "aura_color"
    )

    // Root Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // 1. BOTTOM-MOST LAYER: CameraX live preview (AR mode) or Midnight wallpaper
        ARCameraBackground(
            isCameraActive = isArCameraActive && !isCameraDockVisible,
            isFrontCamera = isFrontCamera,
            modifier = Modifier.fillMaxSize()
        )

        // 2. FOREGROUND CONTENT: Live2D Stage (Top 60%) + Frosted Glass Chat Overlay (Bottom 40%)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // TOP 60%: Live2D Transparent AR Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.60f)
                    .testTag("live2d_stage_section")
            ) {
                // Live2D WebView Canvas directly floating over camera feed
                Live2DStage(
                    currentEmotion = currentEmotion,
                    controller = live2DController,
                    onModelTapped = { zone ->
                        viewModel.onModelTapped(zone)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Explicit, user-triggered camera inspection. The dock owns its preview,
                // front/back switch, and still-image capture; closing it releases CameraX.
                CameraPreviewDock(
                    isVisible = isCameraDockVisible,
                    onClose = { viewModel.toggleCameraDock(false) },
                    onCaptureForInspection = { bitmap, fromFrontCamera ->
                        viewModel.toggleCameraDock(false)
                        val cameraLabel = if (fromFrontCamera) "front selfie camera" else "rear world camera"
                        viewModel.sendMessage(
                            "Ayaka, please inspect this snapshot from the $cameraLabel. Describe visible details and answer my question based only on the image.",
                            bitmap,
                            isFrontCamera = fromFrontCamera
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // Top Header Overlay: Bond Status & Mode Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bond Level Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GlassDark.copy(alpha = 0.75f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, auraColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = BlushPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Lv.${userBond?.bondLevel ?: 1} • ${userBond?.getTierTitle() ?: "Slacker"}",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Top Action Controls: Dedicated AR Camera World-Feed Button, Online Pill & API Key
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Dedicated Floating Camera World-Feed Toggle
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isArCameraActive) VioletAccent.copy(alpha = 0.9f) else GlassDark.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isArCameraActive) LavenderPrimary else GlassBorder
                            ),
                            modifier = Modifier
                                .clickable { viewModel.toggleArCamera() }
                                .testTag("toggle_ar_camera_feed_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isArCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle live camera background",
                                    tint = if (isArCameraActive) TextWhite else LavenderPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isArCameraActive) "Camera On" else "Camera View",
                                    color = if (isArCameraActive) TextWhite else TextSilver,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Flip Camera Front/Back button (when AR is active)
                        if (isArCameraActive) {
                            IconButton(
                                onClick = { viewModel.toggleCameraFacing() },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(GlassDark.copy(alpha = 0.85f))
                                    .border(1.dp, GlassBorder, CircleShape)
                                    .testTag("flip_camera_facing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera Facing",
                                    tint = LavenderPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        // Open the camera inspection dock for a one-shot visual question.
                        IconButton(
                            onClick = { viewModel.toggleCameraDock(true) },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GlassDark.copy(alpha = 0.85f))
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("open_camera_inspection_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Open Ayaka camera inspection",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Online/Offline Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GlassDark.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.clickable {
                                viewModel.toggleOnlineMode(!isOnlineMode)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOnlineMode) Icons.Default.Cloud else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isOnlineMode) LavenderPrimary else SoftGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOnlineMode) "Cloud" else "Guard",
                                    color = TextSilver,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // API Key dialog button
                        IconButton(
                            onClick = {
                                tempApiKey = apiKey
                                showApiKeyDialog = true
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GlassDark.copy(alpha = 0.75f))
                                .border(1.dp, GlassBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Minimalist Master Speaker Voice Toggle
                        IconButton(
                            onClick = { viewModel.toggleAudioMute() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) VioletAccent.copy(alpha = 0.85f) else GlassDark.copy(alpha = 0.75f))
                                .border(
                                    1.dp,
                                    if (isSpeaking) LavenderPrimary else if (!isAudioMuted) GlassBorder else SlateBorder,
                                    CircleShape
                                )
                                .testTag("master_audio_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = if (isAudioMuted) "Unmute Ayaka Voice" else "Mute Ayaka Voice",
                                tint = if (!isAudioMuted) LavenderPrimary else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Quick Emotion Selector Chips along the right edge
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val emotions = listOf(
                        "blush" to "😳",
                        "smile" to "😊",
                        "angry" to "😠",
                        "happy" to "✨",
                        "sad" to "🥺",
                        "normal" to "😐"
                    )
                    emotions.forEach { (emo, emoji) ->
                        Surface(
                            shape = CircleShape,
                            color = if (currentEmotion == emo) VioletAccent else GlassDark.copy(alpha = 0.75f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentEmotion == emo) LavenderPrimary else GlassBorder
                            ),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable { viewModel.setEmotion(emo) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Pomodoro Session Floating Dock Overlaid on top of stage if active
                if (isPomodoroVisible) {
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        PomodoroSession(
                            onSessionCompleted = { mins ->
                                viewModel.onStudyCompleted(mins)
                            },
                            onAppSwitchedAway = {
                                viewModel.onAppSwitchedAwayDuringStudy()
                            }
                        )
                    }
                }
            }

            // 3. BOTTOM 40%: Frosted-Glass Semi-Transparent Chat Overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.40f)
                    .testTag("chat_section_surface"),
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = GlassDark.copy(alpha = 0.60f), // Frosted semi-transparent overlay
                border = androidx.compose.foundation.BorderStroke(1.dp, auraColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    // Toolbar: Pomodoro Toggle, Bedtime Scold Test, Clear Chat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Study / Pomodoro Toggle
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isPomodoroVisible) VioletAccent else GlassSurface.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clickable { viewModel.togglePomodoro(!isPomodoroVisible) }
                                    .testTag("toggle_pomodoro_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isPomodoroVisible) TextWhite else LavenderPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Study Session",
                                        fontSize = 11.sp,
                                        color = if (isPomodoroVisible) TextWhite else TextSilver
                                    )
                                }
                            }

                            // 11:00 AM Curfew & Bedtime Scold Settings Dialog Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GlassSurface.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                                modifier = Modifier
                                    .clickable {
                                        tempBedtimePhrase = userBond?.customBedtimePhrase
                                            ?: "It's 11:00 AM curfew! Put your phone down right now and take a rest, you hopeless slacker!"
                                        selectedHour = userBond?.bedtimeHour ?: 11
                                        selectedMinute = userBond?.bedtimeMinute ?: 0
                                        showBedtimeDialog = true
                                    }
                                    .testTag("curfew_settings_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val hour = userBond?.bedtimeHour ?: 11
                                    val min = (userBond?.bedtimeMinute ?: 0).toString().padStart(2, '0')
                                    val amPm = if (hour < 12) "AM" else "PM"
                                    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                                    Text(
                                        "Curfew ($displayHour:$min $amPm)",
                                        fontSize = 11.sp,
                                        color = TextSilver
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit Curfew Settings",
                                        tint = LavenderPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            // Quick Bedtime Alert Test
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GlassSurface.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .clickable { viewModel.triggerTestMidnightAlert(context) }
                                    .testTag("test_midnight_alert_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = SoftGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Test Alert", fontSize = 11.sp, color = TextSilver)
                                }
                            }
                        }

                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.size(28.dp).testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Chat",
                                tint = TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Quick Prompt Suggestions Row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val prompts = listOf(
                            "👉 Poke Ayaka" to "Hey, Ayaka! *pokes cheeks*",
                            "🌸 You look pretty today" to "Ayaka, you look really pretty today.",
                            "🥱 I'm so exhausted..." to "Ayaka, I'm so exhausted and sleepy...",
                            "🍱 Did you eat lunch?" to "Ayaka, did you eat proper lunch today?",
                            "📖 Check my study goal" to "Ayaka, can you keep an eye on my study goal?"
                        )
                        items(prompts) { (label, prompt) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SlateDark.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable { viewModel.sendMessage(prompt) }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = TextSilver,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Chat Messages List (Frosted glass bubbles)
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        if (messages.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Say something to Ayaka, attach a photo, or poke her above!",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        items(messages, key = { it.id }) { msg ->
                            Box(modifier = Modifier.animateItem()) {
                                ChatMessageBubble(
                                    message = msg,
                                    onReplayAudio = { text, emotion ->
                                        viewModel.speakMessage(text, emotion, isCritical = true)
                                    }
                                )
                            }
                        }

                        if (isLoading) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        color = LavenderPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Ayaka is preparing her reply...",
                                        color = LavenderPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Selected Attachment Preview Card (Photos)
                    AnimatedVisibility(
                        visible = attachedBitmap != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        attachedBitmap?.let { bmp ->
                            Surface(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = GlassDark.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Attached media",
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Attached for Ayaka's inspection",
                                        fontSize = 11.sp,
                                        color = TextSilver,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { attachedBitmap = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove attachment",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Message Input Bar with Distinct Attachment Media Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Distinct Attachment Icon for uploading Photos & Videos
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassSurface.copy(alpha = 0.75f))
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("chat_attach_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Upload Photo",
                                tint = if (attachedBitmap != null) LavenderPrimary else TextSilver,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    "Talk to Ayaka… or face her scolding",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_field"),
                            shape = RoundedCornerShape(22.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Send
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank() || attachedBitmap != null) {
                                        val text = if (inputText.isNotBlank()) inputText else "Ayaka, inspect this photo!"
                                        val bmp = attachedBitmap
                                        inputText = ""
                                        attachedBitmap = null
                                        viewModel.sendMessage(text, bmp)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LavenderPrimary,
                                unfocusedBorderColor = SlateBorder.copy(alpha = 0.5f),
                                focusedContainerColor = SlateDark.copy(alpha = 0.6f),
                                unfocusedContainerColor = SlateDark.copy(alpha = 0.6f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            singleLine = true,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(
                                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                        )
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to YoruHime")
                                    }
                                    chatSpeechRecognizerLauncher.launch(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Speech recognition is not available on this device",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GlassSurface.copy(alpha = 0.75f))
                                .border(1.dp, GlassBorder, CircleShape)
                                .testTag("chat_voice_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak a chat message",
                                tint = LavenderPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() || attachedBitmap != null) {
                                    val text = if (inputText.isNotBlank()) inputText else "Ayaka, inspect this photo!"
                                    val bmp = attachedBitmap
                                    inputText = ""
                                    attachedBitmap = null
                                    viewModel.sendMessage(text, bmp)
                                }
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(VioletAccent)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = TextWhite,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("OpenRouter Cloud Setup", color = TextWhite) },
            text = {
                Column {
                    Text(
                        "Enter your OpenRouter API key for cloud chat. YoruHime uses OpenRouter's automatic model routing. If the key is blank or the request fails, it falls back to the offline dialogue engine.",
                        fontSize = 12.sp,
                        color = TextSilver
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("sk-or-v1-...", color = TextMuted) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Memory tip: say “Remember that I prefer…” to save a note on this phone. Say “Forget that…” to remove that exact note.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    TextButton(onClick = {
                        showApiKeyDialog = false
                        showMemoryDialog = true
                    }) {
                        Text("Manage saved memories (" + memories.size + ")", color = LavenderPrimary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateApiKey(tempApiKey.trim())
                        showApiKeyDialog = false
                    }
                ) {
                    Text("Save & Connect", color = LavenderPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SlateDark
        )
    }

    // Local memory manager: review and remove notes saved at the user's request.
    if (showMemoryDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryDialog = false },
            title = { Text("YoruHime's Memories", color = TextWhite) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "These notes are stored in this app's local database. When cloud chat is enabled, relevant notes are included in requests to OpenRouter.",
                        fontSize = 11.sp,
                        color = TextSilver
                    )
                    if (memories.isEmpty()) {
                        Text("No saved memories yet. Tell YoruHime: Remember that ...", color = TextMuted)
                    } else {
                        memories.forEach { memory ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = memory.content,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.forgetMemory(memory.content) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete saved memory",
                                        tint = BlushPink
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearMemories()
                    showMemoryDialog = false
                }) {
                    Text("Clear all", color = BlushPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemoryDialog = false }) {
                    Text("Done", color = LavenderPrimary)
                }
            },
            containerColor = SlateDark
        )
    }

    // Bedtime & Curfew Settings Dialog (11:00 AM Curfew / Midnight / Custom Phrase)
    if (showBedtimeDialog) {
        val scrollState = rememberScrollState()
        val presetPhrases = listOf(
            "It's 11:00 AM curfew! Put your phone down right now and take a rest, you hopeless slacker!",
            "Hey! Look at the clock, it's 11:00 AM! Stop staring at this screen and stretch, or I'm taking your phone!",
            "Are you really still slacking off at 11:00 AM?! I didn't remind you because I care... I just hate seeing you ruin your health!",
            "It's bedtime! Put your phone down and go to sleep right this second, you stubborn idiot! If you get sick, I'm NOT nursing you!"
        )

        val timeOptions = listOf(
            Triple("11:00 AM (Curfew)", 11, 0),
            Triple("12:00 AM (Midnight)", 0, 0),
            Triple("11:00 PM (Night)", 23, 0),
            Triple("10:30 PM", 22, 30)
        )

        AlertDialog(
            onDismissRequest = { showBedtimeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = SoftGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Curfew & Bedtime Scold Settings",
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ayaka will enforce your daily rest schedule with a push notification and strict audio scolding in her mommy-tsundere voice.",
                        fontSize = 12.sp,
                        color = TextSilver,
                        lineHeight = 16.sp
                    )

                    // 1. Curfew Time Selector
                    Text(
                        text = "1. Curfew Alert Time:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeOptions.take(2).forEach { (label, hour, min) ->
                            val isSelected = selectedHour == hour && selectedMinute == min
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VioletAccent else SlateSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LavenderPrimary else SlateBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedHour = hour
                                        selectedMinute = min
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextWhite else TextSilver
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timeOptions.drop(2).forEach { (label, hour, min) ->
                            val isSelected = selectedHour == hour && selectedMinute == min
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) VioletAccent else SlateSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) LavenderPrimary else SlateBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedHour = hour
                                        selectedMinute = min
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TextWhite else TextSilver
                                    )
                                }
                            }
                        }
                    }

                    // 2. Select In-Character Trigger Phrase
                    Text(
                        text = "2. Select In-Character Scold Phrase:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderPrimary
                    )

                    presetPhrases.forEachIndexed { _, phrase ->
                        val isCurrent = tempBedtimePhrase.trim() == phrase.trim()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) VioletAccent.copy(alpha = 0.45f) else SlateSurface.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrent) LavenderPrimary else SlateBorder.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempBedtimePhrase = phrase }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.Check else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrent) LavenderPrimary else TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = phrase,
                                    fontSize = 11.sp,
                                    color = if (isCurrent) TextWhite else TextSilver,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // 3. Record or Custom Edit
                    Text(
                        text = "3. Or Record / Type Your Own:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LavenderPrimary
                    )

                    // Speech-to-Text Voice Recording Button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(
                                        RecognizerIntent.EXTRA_PROMPT,
                                        "Speak Ayaka's curfew trigger phrase..."
                                    )
                                }
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    "Speech recognition not available on this device",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassSurface,
                            contentColor = TextWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LavenderPrimary.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_voice_trigger_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Trigger Phrase",
                                tint = BlushPink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Record Custom Voice Phrase",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextWhite
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tempBedtimePhrase,
                        onValueChange = { tempBedtimePhrase = it },
                        placeholder = { Text("Enter custom scold phrase...", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = LavenderPrimary,
                            unfocusedBorderColor = SlateBorder
                        )
                    )

                    // 4. Voice Preview Action
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SlateDark,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.previewCustomPhrase(tempBedtimePhrase)
                            }
                            .testTag("preview_curfew_voice_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Preview Ayaka Voice Scold",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftGold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phrase = tempBedtimePhrase.trim().ifBlank {
                            "It's 11:00 AM curfew! Put your phone down right now and take a rest, you hopeless slacker!"
                        }
                        viewModel.updateBedtimeConfig(
                            context = context,
                            phrase = phrase,
                            hour = selectedHour,
                            minute = selectedMinute
                        )
                        val displayHour = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
                        val displayMin = selectedMinute.toString().padStart(2, '0')
                        val amPm = if (selectedHour < 12) "AM" else "PM"
                        Toast.makeText(
                            context,
                            "Curfew set for $displayHour:$displayMin $amPm!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showBedtimeDialog = false
                    },
                    modifier = Modifier.testTag("save_curfew_config_button")
                ) {
                    Text("Save Curfew", color = LavenderPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBedtimeDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SlateDark
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    onReplayAudio: (String, String) -> Unit = { _, _ -> }
) {
    val isUser = message.isFromUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) VioletAccent.copy(alpha = 0.85f) else SlateDark.copy(alpha = 0.65f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) VioletAccent.copy(alpha = 0.85f) else SlateBorder.copy(alpha = 0.5f)
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Ayaka Tōjou",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LavenderPrimary
                            )
                            if (message.isOffline) {
                                Text(
                                    text = "• Offline",
                                    fontSize = 9.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // On-Demand Voice Line Replay Button
                        IconButton(
                            onClick = { onReplayAudio(message.text, message.emotionTag) },
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(SlateSurface.copy(alpha = 0.5f))
                                .testTag("replay_voice_button_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Replay Ayaka voice line",
                                tint = LavenderPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = if (isUser) TextWhite else TextSilver
                )
            }
        }
    }
}
