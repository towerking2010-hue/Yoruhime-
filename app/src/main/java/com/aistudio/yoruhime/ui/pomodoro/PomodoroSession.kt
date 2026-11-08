package com.aistudio.yoruhime.ui.pomodoro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aistudio.yoruhime.ui.theme.BlushPink
import com.aistudio.yoruhime.ui.theme.GlassBorder
import com.aistudio.yoruhime.ui.theme.GlassDark
import com.aistudio.yoruhime.ui.theme.LavenderPrimary
import com.aistudio.yoruhime.ui.theme.SlateBorder
import com.aistudio.yoruhime.ui.theme.SlateSurface
import com.aistudio.yoruhime.ui.theme.TextMuted
import com.aistudio.yoruhime.ui.theme.TextSilver
import com.aistudio.yoruhime.ui.theme.TextWhite
import com.aistudio.yoruhime.ui.theme.VioletAccent
import kotlinx.coroutines.delay

enum class PomodoroState {
    IDLE, FOCUSING, PAUSED, BREAK
}

@Composable
fun PomodoroSession(
    onSessionCompleted: (Int) -> Unit,
    onAppSwitchedAway: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by remember { mutableIntStateOf(25) }
    var totalSeconds by remember { mutableIntStateOf(25 * 60) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    var state by remember { mutableStateOf(PomodoroState.IDLE) }
    var isBreak by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Detect app switching away during active focus session and scold on return
    DisposableEffect(lifecycleOwner, state) {
        var wasSwitchedAway = false
        val observer = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && state == PomodoroState.FOCUSING) {
                wasSwitchedAway = true
                state = PomodoroState.PAUSED
            } else if (event == Lifecycle.Event.ON_RESUME && wasSwitchedAway) {
                wasSwitchedAway = false
                onAppSwitchedAway()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Timer Loop
    LaunchedEffect(state) {
        if (state == PomodoroState.FOCUSING || state == PomodoroState.BREAK) {
            while (secondsRemaining > 0 && (state == PomodoroState.FOCUSING || state == PomodoroState.BREAK)) {
                delay(1000L)
                secondsRemaining--
            }
            if (secondsRemaining <= 0) {
                if (state == PomodoroState.FOCUSING) {
                    onSessionCompleted(selectedMinutes)
                    // Auto switch to 5 min break
                    isBreak = true
                    state = PomodoroState.BREAK
                    totalSeconds = 5 * 60
                    secondsRemaining = 5 * 60
                } else {
                    // Break ended
                    isBreak = false
                    state = PomodoroState.IDLE
                    totalSeconds = selectedMinutes * 60
                    secondsRemaining = totalSeconds
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("pomodoro_session_surface"),
        shape = RoundedCornerShape(20.dp),
        color = GlassDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = LavenderPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBreak) "Ayaka's Approved Break" else "Ayaka's Strict Study Session",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (state == PomodoroState.FOCUSING) VioletAccent.copy(alpha = 0.3f) else SlateSurface
                ) {
                    Text(
                        text = when (state) {
                            PomodoroState.IDLE -> "Standby"
                            PomodoroState.FOCUSING -> "Locked In"
                            PomodoroState.PAUSED -> "Paused (Caught Slacking?)"
                            PomodoroState.BREAK -> "Break Time"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = if (state == PomodoroState.FOCUSING) LavenderPrimary else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Time Selector chips when Idle
            AnimatedVisibility(visible = state == PomodoroState.IDLE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = selectedMinutes == mins,
                            onClick = {
                                selectedMinutes = mins
                                totalSeconds = mins * 60
                                secondsRemaining = totalSeconds
                            },
                            label = { Text("${mins}m", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletAccent,
                                selectedLabelColor = TextWhite,
                                containerColor = SlateSurface,
                                labelColor = TextSilver
                            )
                        )
                    }
                }
            }

            // Timer Display Circle with Animated Focus Glow
            val isFocusing = state == PomodoroState.FOCUSING
            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse_glow")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.96f,
                targetValue = 1.06f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.55f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "pulse_alpha"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(116.dp)
            ) {
                if (isFocusing) {
                    Box(
                        modifier = Modifier
                            .size(110.dp * pulseScale)
                            .background(
                                color = VioletAccent.copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                }

                val progress = if (totalSeconds > 0) (secondsRemaining.toFloat() / totalSeconds.toFloat()) else 0f
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(105.dp),
                    color = if (isBreak) BlushPink else LavenderPrimary,
                    trackColor = SlateBorder,
                    strokeWidth = 6.dp
                )

                val displayMins = secondsRemaining / 60
                val displaySecs = secondsRemaining % 60
                val timeString = String.format("%02d:%02d", displayMins, displaySecs)

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 24.sp
                    )
                    Text(
                        text = if (isBreak) "Rest Your Eyes" else "Ayaka is Watching",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (state) {
                    PomodoroState.IDLE -> {
                        Button(
                            onClick = {
                                totalSeconds = selectedMinutes * 60
                                secondsRemaining = totalSeconds
                                isBreak = false
                                state = PomodoroState.FOCUSING
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pomodoro_start_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Focus Block", fontSize = 12.sp)
                        }
                    }
                    PomodoroState.FOCUSING -> {
                        Button(
                            onClick = { state = PomodoroState.PAUSED },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("pomodoro_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause Session", fontSize = 12.sp)
                        }
                    }
                    PomodoroState.PAUSED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { state = PomodoroState.FOCUSING },
                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pomodoro_resume_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    state = PomodoroState.IDLE
                                    totalSeconds = selectedMinutes * 60
                                    secondsRemaining = totalSeconds
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("pomodoro_reset_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", fontSize = 12.sp)
                            }
                        }
                    }
                    PomodoroState.BREAK -> {
                        Button(
                            onClick = {
                                state = PomodoroState.IDLE
                                isBreak = false
                                totalSeconds = selectedMinutes * 60
                                secondsRemaining = totalSeconds
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BlushPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Skip Break & Ready Next", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
