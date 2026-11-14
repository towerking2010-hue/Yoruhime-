package com.aistudio.yoruhime.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.yoruhime.data.local.MemoryNoteEntity
import com.aistudio.yoruhime.data.local.UserBondEntity
import com.aistudio.yoruhime.ui.pomodoro.PomodoroSession
import com.aistudio.yoruhime.ui.theme.*

enum class WorkspaceDestination(val label: String) {
    COMPANION("Companion"), MEMORIES("Memories"), CARE("Care"), SETTINGS("Settings")
}

@Composable
fun YoruHimeBottomBar(selected: WorkspaceDestination, onSelect: (WorkspaceDestination) -> Unit) {
    NavigationBar(containerColor = MidnightNavy, tonalElevation = 0.dp) {
        val destinations = listOf(
            WorkspaceDestination.COMPANION to Icons.Default.Home,
            WorkspaceDestination.MEMORIES to Icons.AutoMirrored.Filled.MenuBook,
            WorkspaceDestination.CARE to Icons.Default.Favorite,
            WorkspaceDestination.SETTINGS to Icons.Default.Settings
        )
        destinations.forEach { (destination, icon) ->
            NavigationBarItem(
                selected = selected == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(icon, contentDescription = destination.label) },
                label = { Text(destination.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextWhite,
                    selectedTextColor = LavenderPrimary,
                    indicatorColor = VioletAccent.copy(alpha = 0.32f),
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Composable
fun YoruHimeWorkspacePage(
    destination: WorkspaceDestination,
    memories: List<MemoryNoteEntity>,
    bond: UserBondEntity?,
    isOnlineMode: Boolean,
    hasApiKey: Boolean,
    isAudioMuted: Boolean,
    isPomodoroVisible: Boolean,
    onForgetMemory: (String) -> Unit,
    onClearMemories: () -> Unit,
    onToggleCloud: () -> Unit,
    onConfigureCloud: () -> Unit,
    onToggleAudio: () -> Unit,
    onTogglePomodoro: () -> Unit,
    onConfigureBedtime: () -> Unit,
    onStudyCompleted: (Int) -> Unit,
    onStudyInterrupted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(MidnightBlack).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("YORUHIME  /  " + destination.label.uppercase(), color = LavenderPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
        Spacer(Modifier.height(12.dp))
        when (destination) {
            WorkspaceDestination.COMPANION -> Unit
            WorkspaceDestination.MEMORIES -> {
                Text("What Ayaka remembers", color = TextWhite, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Only facts you explicitly ask her to remember are stored here on this device.", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                if (memories.isEmpty()) {
                    WorkspaceCard {
                        Text("Nothing saved yet", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Text("In chat, say “Remember that I prefer…” and it will appear here.", color = TextMuted, fontSize = 13.sp)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(memories.size.toString() + " saved note(s)", color = TextSilver, fontSize = 12.sp)
                        TextButton(onClick = onClearMemories) { Text("Clear all", color = BlushPink) }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        items(memories, key = { it.id }) { memory ->
                            WorkspaceCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(memory.content, color = TextWhite, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onForgetMemory(memory.content) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Forget memory", tint = BlushPink)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            WorkspaceDestination.CARE -> {
                Text("A little structure, not pressure.", color = TextWhite, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Ayaka helps you stay consistent while remembering to rest.", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = BlushPink)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Your bond", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Level " + (bond?.bondLevel ?: 1) + " · " + (bond?.getTierTitle() ?: "Getting acquainted"), color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Focused study: " + (bond?.totalStudyMinutes ?: 0) + " minutes", color = TextSilver, fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = LavenderPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Focus session", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Work in a focused block, then take a real break.", color = TextMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    if (isPomodoroVisible) PomodoroSession(onSessionCompleted = onStudyCompleted, onAppSwitchedAway = onStudyInterrupted)
                    else AccentAction("Start a focus session", onTogglePomodoro)
                }
                Spacer(Modifier.height(12.dp))
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = SoftGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Rest & bedtime", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Daily reminder: " + formatBedtime(bond?.bedtimeHour ?: 23, bond?.bedtimeMinute ?: 0), color = TextSilver, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    AccentAction("Adjust bedtime reminder", onConfigureBedtime)
                }
            }
            WorkspaceDestination.SETTINGS -> {
                Text("Make YoruHime yours.", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Clear controls. No surprise spending. Your settings stay in your hands.", color = TextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(16.dp))
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isOnlineMode) Icons.Default.Cloud else Icons.Default.CloudOff, contentDescription = null, tint = LavenderPrimary)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI engine", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text(if (isOnlineMode && hasApiKey) "Cloud selected · key saved" else if (isOnlineMode) "Cloud selected · setup required" else "Offline companion", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentAction(if (isOnlineMode) "Use offline" else "Try cloud", onToggleCloud, Modifier.weight(1f))
                        AccentAction("API setup", onConfigureCloud, Modifier.weight(1f), secondary = true)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Cloud requests use the provider and key you configure. Offline replies are lightweight scripted responses. YoruHime does not make purchases or top up credits.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                WorkspaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isAudioMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = null, tint = LavenderPrimary)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Ayaka's voice", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text(if (isAudioMuted) "Muted" else "Voice enabled", color = TextMuted, fontSize = 12.sp)
                        }
                        TextButton(onClick = onToggleAudio) { Text(if (isAudioMuted) "Unmute" else "Mute", color = LavenderPrimary) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                WorkspaceCard {
                    Text("Privacy & control", color = TextWhite, fontWeight = FontWeight.SemiBold)
                    Text("Saved memories stay in the local database. You can review or delete them at any time.", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WorkspaceCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = SlateDark, border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun AccentAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (secondary) SlateSurface else VioletAccent, contentColor = Color.White),
        border = if (secondary) BorderStroke(1.dp, SlateBorder) else null
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

private fun formatBedtime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return h.toString() + ":" + minute.toString().padStart(2, '0') + if (hour < 12) " AM" else " PM"
}
