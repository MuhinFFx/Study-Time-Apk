package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ExportHelper
import com.example.data.model.PublicProfile
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.parseColorSafe
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    profile: PublicProfile?,
    subjects: List<Subject>,
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    planTier: String,
    onOpenPremium: () -> Unit,
    onExportStatus: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var range by remember { mutableStateOf("WEEK") }

    val cutoffMillis = remember(range) {
        val now = System.currentTimeMillis()
        when (range) {
            "DAY" -> now - TimeUnit.DAYS.toMillis(1)
            "WEEK" -> now - TimeUnit.DAYS.toMillis(7)
            "MONTH" -> now - TimeUnit.DAYS.toMillis(30)
            else -> now - TimeUnit.DAYS.toMillis(365)
        }
    }

    val scopedSessions = remember(sessions, cutoffMillis) {
        sessions.filter { it.verifiedEndMillis >= cutoffMillis }
    }
    val totalMinutes = scopedSessions.sumOf { it.durationMinutes }
    val totalXp = scopedSessions.sumOf { it.xpEarned }
    val completedTasksCount = tasks.count { it.completed }
    val completionRate = if (tasks.isNotEmpty()) (completedTasksCount * 100 / tasks.size) else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DAY" to "Daily", "WEEK" to "Weekly", "MONTH" to "Monthly", "YEAR" to "Yearly").forEach { (k, lbl) ->
                        FilterChip(selected = range == k, onClick = { range = k }, label = { Text(lbl) })
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox("Study Time", formatDurationMinutes(totalMinutes), Modifier.weight(1f))
                    StatBox("Focus Blocks", "${scopedSessions.size}", Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox("XP Earned", "$totalXp XP", Modifier.weight(1f))
                    StatBox("Task Rate", "$completionRate%", Modifier.weight(1f))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionHeader("Subject Breakdown")
                        Spacer(Modifier.height(8.dp))
                        if (subjects.isEmpty()) {
                            Text("Add subjects to view subject-wise distribution.")
                        } else {
                            val maxSubjMins = subjects.maxOf { it.completedMinutes }.coerceAtLeast(30L)
                            subjects.forEach { s ->
                                val c = parseColorSafe(s.colorHex)
                                val ratio = (s.completedMinutes.toFloat() / maxSubjMins.toFloat()).coerceIn(0f, 1f)
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(s.name, style = MaterialTheme.typography.labelLarge)
                                        Text(formatDurationMinutes(s.completedMinutes), style = MaterialTheme.typography.labelMedium, color = c)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        color = c
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Streak History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Current Streak: ${profile?.currentStreakDays ?: 0} days")
                        Text("Longest Streak: ${profile?.longestStreakDays ?: 0} days")
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Export Reports (CSV & PDF)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (planTier == "PREMIUM") "Generate real CSV or PDF study reports."
                            else "PDF & CSV report exports are a Premium feature. You can preview Premium plans below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (planTier != "PREMIUM") {
                                        onOpenPremium()
                                    } else {
                                        val csv = ExportHelper.buildSessionsCsv(profile, subjects, sessions)
                                        val file = ExportHelper.writeCsvFile(context, csv)
                                        onExportStatus("CSV saved: ${file.name}")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_csv_btn")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export CSV")
                            }
                            Button(
                                onClick = {
                                    if (planTier != "PREMIUM") {
                                        onOpenPremium()
                                    } else {
                                        val file = ExportHelper.writePdfReport(context, profile, subjects, sessions)
                                        onExportStatus("PDF saved: ${file.name}")
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_pdf_btn")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export PDF")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}
