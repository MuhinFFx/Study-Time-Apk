package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.parseColorSafe
import com.example.ui.components.subjectIconForKey
import com.example.ui.theme.BrandViolet

private val subjectColors = listOf("#6C63FF", "#14B8A6", "#F59E0B", "#EC4899", "#3B82F6", "#8B5CF6")
private val subjectIcons = listOf("book", "code", "calculate", "science", "language", "art", "psych", "school")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    subjects: List<Subject>,
    sessions: List<StudySession>,
    planTier: String,
    onAddSubject: (String, String, String, String, Int) -> Unit,
    onUpdateSubject: (String, String, String, String, String, Int) -> Unit,
    onDeleteSubject: (String) -> Unit,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedForAnalytics by remember { mutableStateOf<Subject?>(null) }

    val filtered = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) subjects
        else subjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val isAtFreeCap = planTier != "PREMIUM" && subjects.size >= 5

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subjects (${subjects.size}${if (planTier != "PREMIUM") "/5" else ""})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isAtFreeCap) onOpenPremium() else showCreateDialog = true
                },
                containerColor = BrandViolet,
                contentColor = Color.White,
                modifier = Modifier.testTag("subjects_add_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Subject")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Search subjects...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subjects_search_input"),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (isAtFreeCap) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BrandViolet.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenPremium)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Free plan limit reached (5/5 subjects). Upgrade to Premium for unlimited subjects.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onOpenPremium) {
                                Text("Upgrade")
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No subjects yet",
                        subtitle = "Create subjects to organize your study time, notes, and target hours.",
                        actionText = "Add Subject",
                        onAction = { showCreateDialog = true }
                    )
                }
            } else {
                items(filtered, key = { it.id }) { subject ->
                    val subjColor = parseColorSafe(subject.colorHex)
                    val pct = if (subject.targetMinutes > 0) {
                        (subject.completedMinutes.toFloat() / subject.targetMinutes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedForAnalytics = subject },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(subjColor.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        subjectIconForKey(subject.iconKey),
                                        contentDescription = subject.name,
                                        tint = subjColor
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (subject.description.isNotBlank()) {
                                        Text(
                                            subject.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { editingSubject = subject }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${subject.name}")
                                }
                                IconButton(onClick = { onDeleteSubject(subject.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${subject.name}")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Completed: ${formatDurationMinutes(subject.completedMinutes)}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "${(pct * 100).toInt()}% of ${subject.targetMinutes / 60}h target",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = subjColor
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = subjColor
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingSubject != null) {
        SubjectEditorDialog(
            initial = editingSubject,
            onDismiss = {
                showCreateDialog = false
                editingSubject = null
            },
            onSave = { name, iconKey, colorHex, desc, targetHours ->
                val target = editingSubject
                if (target == null) {
                    onAddSubject(name, iconKey, colorHex, desc, targetHours)
                } else {
                    onUpdateSubject(target.id, name, iconKey, colorHex, desc, targetHours)
                }
                showCreateDialog = false
                editingSubject = null
            }
        )
    }

    selectedForAnalytics?.let { subject ->
        val subjSessions = sessions.filter { it.subjectId == subject.id || it.subjectName == subject.name }
        val totalXp = subjSessions.sumOf { it.xpEarned }
        AlertDialog(
            onDismissRequest = { selectedForAnalytics = null },
            title = { Text("${subject.name} Analytics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Verified Sessions: ${subjSessions.size}")
                    Text("Completed Time: ${formatDurationMinutes(subject.completedMinutes)}")
                    Text("Target Time: ${subject.targetMinutes / 60} hours")
                    Text("Total XP Earned: $totalXp XP")
                }
            },
            confirmButton = {
                Button(onClick = { selectedForAnalytics = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SubjectEditorDialog(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var iconKey by remember { mutableStateOf(initial?.iconKey ?: "book") }
    var colorHex by remember { mutableStateOf(initial?.colorHex ?: "#6C63FF") }
    var targetHours by remember { mutableFloatStateOf(((initial?.targetMinutes ?: 600L) / 60f).coerceIn(1f, 200f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Subject" else "Edit Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Target Hours: ${targetHours.toInt()}h", style = MaterialTheme.typography.labelLarge)
                Slider(value = targetHours, onValueChange = { targetHours = it }, valueRange = 1f..200f)

                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectColors.forEach { hex ->
                        val c = parseColorSafe(hex)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (colorHex == hex) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                Text("Icon", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(subjectIcons) { key ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (iconKey == key) BrandViolet.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { iconKey = key }
                        ) {
                            Icon(
                                subjectIconForKey(key),
                                contentDescription = key,
                                modifier = Modifier.padding(8.dp),
                                tint = if (iconKey == key) BrandViolet else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, iconKey, colorHex, desc, targetHours.toInt()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
