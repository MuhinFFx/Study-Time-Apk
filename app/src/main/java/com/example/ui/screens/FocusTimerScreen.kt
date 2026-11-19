package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.AmbientSound
import com.example.core.LocalAppPreferences
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatTimerSeconds
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusTimerScreen(
    timerState: com.example.ui.viewmodel.PomodoroTimerState,
    preferences: LocalAppPreferences,
    subjects: List<Subject>,
    sessions: List<StudySession>,
    onSelectPreset: (String, Int, Int) -> Unit,
    onSelectSubject: (String, String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onSwitchPhase: (com.example.ui.viewmodel.TimerPhase) -> Unit,
    onSelectSound: (AmbientSound) -> Unit,
    onToggleChime: (Boolean) -> Unit,
    onDismissReward: () -> Unit
) {
    var showCustomDialog by remember { mutableStateOf(false) }

    val progress = if (timerState.totalSeconds > 0) {
        timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerArc")

    val sessionLocked = timerState.running || timerState.startedAtWallMillis > 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Focus Studio", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Verified Pomodoro tracking • Duration locked once active",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Reward Celebration Banner
        item {
            AnimatedVisibility(visible = timerState.completedRewardBanner != null) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = AccentTeal)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = timerState.completedRewardBanner.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(onClick = onDismissReward) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
        }

        // Mode Presets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = timerState.mode == "POMODORO_25",
                    onClick = { onSelectPreset("POMODORO_25", 25, 5) },
                    enabled = !sessionLocked,
                    label = { Text("25 / 5") },
                    modifier = Modifier.testTag("preset_25_5")
                )
                FilterChip(
                    selected = timerState.mode == "DEEP_50",
                    onClick = { onSelectPreset("DEEP_50", 50, 10) },
                    enabled = !sessionLocked,
                    label = { Text("50 / 10") },
                    modifier = Modifier.testTag("preset_50_10")
                )
                FilterChip(
                    selected = timerState.mode == "CUSTOM",
                    onClick = { showCustomDialog = true },
                    enabled = !sessionLocked,
                    label = { Text("Custom (${timerState.focusMinutes}m)") },
                    modifier = Modifier.testTag("preset_custom")
                )
            }
        }

        // Phase Selector + Circular Countdown Dial
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.example.ui.viewmodel.TimerPhase.entries.forEach { phase ->
                            FilterChip(
                                selected = timerState.phase == phase,
                                onClick = { onSwitchPhase(phase) },
                                enabled = !timerState.running,
                                label = {
                                    Text(
                                        when (phase) {
                                            com.example.ui.viewmodel.TimerPhase.FOCUS -> "Focus"
                                            com.example.ui.viewmodel.TimerPhase.SHORT_BREAK -> "Short Break"
                                            com.example.ui.viewmodel.TimerPhase.LONG_BREAK -> "Long Break"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    val arcColor = if (timerState.phase == com.example.ui.viewmodel.TimerPhase.FOCUS) BrandViolet else AccentTeal
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    Box(
                        modifier = Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 14.dp.toPx()
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = arcColor,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formatTimerSeconds(timerState.remainingSeconds),
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.testTag("timer_display")
                            )
                            Text(
                                text = timerState.selectedSubjectName,
                                style = MaterialTheme.typography.labelLarge,
                                color = arcColor
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { if (timerState.running) onPause() else onStartOrResume() },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .testTag("timer_start_pause_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = arcColor)
                        ) {
                            Icon(
                                if (timerState.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    timerState.running -> "Pause"
                                    timerState.startedAtWallMillis > 0L -> "Resume"
                                    else -> "Start Focus"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("timer_reset_btn"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reset Timer")
                        }
                    }
                }
            }
        }

        // Subject Selection
        item {
            SectionHeader("Active Subject")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = timerState.selectedSubjectId == "general",
                        onClick = { onSelectSubject("general", "General Study") },
                        label = { Text("General Study") }
                    )
                }
                items(subjects, key = { it.id }) { subject ->
                    FilterChip(
                        selected = timerState.selectedSubjectId == subject.id,
                        onClick = { onSelectSubject(subject.id, subject.name) },
                        label = { Text(subject.name) }
                    )
                }
            }
        }

        // Session Notes
        item {
            OutlinedTextField(
                value = timerState.sessionNotes,
                onValueChange = onUpdateNotes,
                label = { Text("Session Goal / Notes") },
                placeholder = { Text("What will you accomplish this session?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_notes_input"),
                maxLines = 3
            )
        }

        // Ambient Focus Sounds & Chime Settings
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = BrandViolet)
                        Spacer(Modifier.width(8.dp))
                        Text("Ambient Focus Sound", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AmbientSound.entries) { sound ->
                            FilterChip(
                                selected = preferences.ambientSound == sound,
                                onClick = { onSelectSound(sound) },
                                label = { Text(sound.label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = AccentTeal)
                            Spacer(Modifier.width(8.dp))
                            Text("Session Completion Chime", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = preferences.chimeEnabled, onCheckedChange = onToggleChime)
                    }
                }
            }
        }

        // Verified Session History
        item {
            SectionHeader("Verified Session History")
            if (sessions.isEmpty()) {
                Text(
                    "No verified sessions recorded yet. Finish a focus block to earn XP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
                sessions.take(12).forEach { sess ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sess.subjectName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${fmt.format(Date(sess.verifiedEndMillis))} • ${sess.durationMinutes}m",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (sess.notes.isNotBlank()) {
                                    Text(sess.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            Surface(
                                color = BrandViolet.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "+${sess.xpEarned} XP",
                                    color = BrandViolet,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var customMins by remember { mutableFloatStateOf(timerState.focusMinutes.toFloat()) }
        var customBreak by remember { mutableFloatStateOf(timerState.breakMinutes.toFloat()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Pomodoro Duration") },
            text = {
                Column {
                    Text("Focus: ${customMins.toInt()} minutes")
                    Slider(
                        value = customMins,
                        onValueChange = { customMins = it },
                        valueRange = 1f..120f
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Break: ${customBreak.toInt()} minutes")
                    Slider(
                        value = customBreak,
                        onValueChange = { customBreak = it },
                        valueRange = 1f..30f
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSelectPreset("CUSTOM", customMins.toInt(), customBreak.toInt())
                    showCustomDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}
