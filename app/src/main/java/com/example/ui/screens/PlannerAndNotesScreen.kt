package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyNote
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.ui.components.EmptyStateCard
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerAndNotesScreen(
    initialTab: Int = 0,
    subjects: List<Subject>,
    tasks: List<StudyTask>,
    notes: List<StudyNote>,
    onAddTask: (String, String, String, String, String, Long, Boolean, String, Boolean) -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit,
    onSaveNote: (String?, String, String, String, String, Boolean) -> Unit,
    onToggleNotePinned: (String, Boolean) -> Unit,
    onDeleteNote: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var calendarHorizon by remember { mutableStateOf("WEEKLY") }
    var searchNotes by remember { mutableStateOf("") }

    var showTaskDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<StudyNote?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planner & Notes") },
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
                    if (selectedTab == 2) {
                        editingNote = null
                        showNoteDialog = true
                    } else {
                        showTaskDialog = true
                    }
                },
                containerColor = BrandViolet,
                contentColor = Color.White,
                modifier = Modifier.testTag("planner_add_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Calendar & Tasks") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Exams") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Notes") })
            }

            when (selectedTab) {
                0 -> {
                    val horizonMs = when (calendarHorizon) {
                        "DAILY" -> 86_400_000L
                        "WEEKLY" -> 7 * 86_400_000L
                        else -> 30 * 86_400_000L
                    }
                    val cutoff = System.currentTimeMillis() + horizonMs
                    val visibleTasks = tasks.filter { !it.examFlag && (it.dueDateMillis <= cutoff || calendarHorizon == "MONTHLY") }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("DAILY" to "Today", "WEEKLY" to "7 Days", "MONTHLY" to "Month").forEach { (key, label) ->
                                    FilterChip(
                                        selected = calendarHorizon == key,
                                        onClick = { calendarHorizon = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        // Week strip calendar
                        item {
                            WeekCalendarStrip()
                        }

                        if (visibleTasks.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No tasks scheduled",
                                    subtitle = "Tap + to schedule a study block, recurring task, or deadline.",
                                    actionText = "Create Task",
                                    onAction = { showTaskDialog = true }
                                )
                            }
                        } else {
                            items(visibleTasks, key = { it.id }) { task ->
                                TaskItemCard(task, onToggle = { onToggleTask(task.id, it) }, onDelete = { onDeleteTask(task.id) })
                            }
                        }
                    }
                }

                1 -> {
                    val exams = tasks.filter { it.examFlag }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (exams.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No upcoming exams",
                                    subtitle = "Add an exam countdown to track days remaining and prioritize revision.",
                                    actionText = "Add Exam",
                                    onAction = { showTaskDialog = true }
                                )
                            }
                        } else {
                            items(exams, key = { it.id }) { exam ->
                                val daysLeft = ((exam.dueDateMillis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L)
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(checked = exam.completed, onCheckedChange = { onToggleTask(exam.id, it) })
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exam.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("${exam.subjectName.ifBlank { "General" }} • ${exam.description}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Surface(
                                            color = AccentRose.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = if (daysLeft == 0L) "Today" else "${daysLeft}d",
                                                color = AccentRose,
                                                style = MaterialTheme.typography.labelLarge,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                        IconButton(onClick = { onDeleteTask(exam.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete exam")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val filteredNotes = remember(notes, searchNotes) {
                        if (searchNotes.isBlank()) notes
                        else notes.filter {
                            it.title.contains(searchNotes, ignoreCase = true) ||
                                it.content.contains(searchNotes, ignoreCase = true) ||
                                it.subjectName.contains(searchNotes, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchNotes,
                                onValueChange = { searchNotes = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                placeholder = { Text("Search notes by title, content, or subject...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                        if (filteredNotes.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No notes yet",
                                    subtitle = "Capture formulas, summaries, and revision cheat sheets organized by subject.",
                                    actionText = "Write Note",
                                    onAction = {
                                        editingNote = null
                                        showNoteDialog = true
                                    }
                                )
                            }
                        } else {
                            items(filteredNotes, key = { it.id }) { note ->
                                val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
                                val dateStr = note.updatedAt?.toDate()?.let { fmt.format(it) } ?: "Just now"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingNote = note
                                            showNoteDialog = true
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${note.subjectName.ifBlank { "General" }} • $dateStr",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { onToggleNotePinned(note.id, !note.pinned) }) {
                                                Icon(
                                                    if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                    contentDescription = "Pin note",
                                                    tint = if (note.pinned) BrandViolet else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { onDeleteNote(note.id) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                                            }
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = note.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 4
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        CreateTaskDialog(
            subjects = subjects,
            defaultExam = selectedTab == 1,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, subjId, subjName, priority, daysOut, reminder, recurring, isExam ->
                onAddTask(
                    title,
                    desc,
                    subjId,
                    subjName,
                    priority,
                    System.currentTimeMillis() + daysOut * 86_400_000L,
                    reminder,
                    recurring,
                    isExam
                )
                showTaskDialog = false
            }
        )
    }

    if (showNoteDialog) {
        CreateNoteDialog(
            initial = editingNote,
            subjects = subjects,
            onDismiss = { showNoteDialog = false },
            onSave = { title, content, subjId, subjName, pinned ->
                onSaveNote(editingNote?.id, title, content, subjId, subjName, pinned)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun WeekCalendarStrip() {
    val days = remember {
        val cal = Calendar.getInstance()
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val numFmt = SimpleDateFormat("dd", Locale.US)
        (0..6).map { offset ->
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            Triple(dayFmt.format(c.time), numFmt.format(c.time), offset == 0)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { (dow, num, isToday) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isToday) BrandViolet else MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        dow,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        num,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskItemCard(
    task: StudyTask,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val prioColor = when (task.priority) {
        "HIGH" -> AccentRose
        "MEDIUM" -> AccentAmber
        else -> AccentTeal
    }
    val fmt = remember { SimpleDateFormat("MMM dd", Locale.US) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.completed, onCheckedChange = onToggle)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null
                )
                Text(
                    text = "${task.subjectName.ifBlank { "General" }} • Due ${fmt.format(Date(task.dueDateMillis))} • ${task.recurring}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = prioColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    task.priority,
                    color = prioColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete task")
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    subjects: List<Subject>,
    defaultExam: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Int, Boolean, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var recurring by remember { mutableStateOf("NONE") }
    var daysOut by remember { mutableIntStateOf(1) }
    var isExam by remember { mutableStateOf(defaultExam) }
    var reminder by remember { mutableStateOf(true) }
    var selectedSubjId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }
    var selectedSubjName by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isExam) "Add Exam Countdown" else "Add Study Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Details") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects, key = { it.id }) { s ->
                            FilterChip(
                                selected = selectedSubjId == s.id,
                                onClick = {
                                    selectedSubjId = s.id
                                    selectedSubjName = s.name
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Today", 1 to "Tomorrow", 7 to "+7d", 14 to "+14d").forEach { (d, lbl) ->
                        FilterChip(selected = daysOut == d, onClick = { daysOut = d }, label = { Text(lbl) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mark as Exam Countdown")
                    Switch(checked = isExam, onCheckedChange = { isExam = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, desc, selectedSubjId, selectedSubjName, priority, daysOut, reminder, recurring, isExam) },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateNoteDialog(
    initial: StudyNote?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    var selectedSubjId by remember { mutableStateOf(initial?.subjectId ?: subjects.firstOrNull()?.id ?: "") }
    var selectedSubjName by remember { mutableStateOf(initial?.subjectName ?: subjects.firstOrNull()?.name ?: "General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Study Note" else "Edit Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Markdown / Rich Study Notes") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subjects.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects, key = { it.id }) { s ->
                            FilterChip(
                                selected = selectedSubjId == s.id,
                                onClick = {
                                    selectedSubjId = s.id
                                    selectedSubjName = s.name
                                },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pin to top")
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, content, selectedSubjId, selectedSubjName, pinned) },
                enabled = title.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
