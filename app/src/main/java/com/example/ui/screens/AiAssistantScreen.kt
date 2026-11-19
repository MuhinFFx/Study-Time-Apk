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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import com.example.ui.viewmodel.AiUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    aiState: AiUiState,
    planTier: String,
    usedToday: Int,
    onAsk: (String) -> Unit,
    onGeneratePlan: () -> Unit,
    onAddTaskToPlanner: (String, String, String) -> Unit,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var prompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Study Coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (planTier == "PREMIUM") "Unlimited Gemini Coaching"
                            else "Free tier: $usedToday / 5 prompts today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (planTier != "PREMIUM") {
                        TextButton(onClick = onOpenPremium) { Text("Upgrade") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = onGeneratePlan,
                        enabled = !aiState.planLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_generate_plan_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (aiState.planLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Crafting Smart Study Plan...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandViolet)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Structured Today's Study Plan")
                        }
                    }
                }

                aiState.latestPlan?.let { plan ->
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Recommended Plan (${plan.recommendedMinutes}m)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(plan.summary, style = MaterialTheme.typography.bodyMedium)
                                plan.tasks.forEach { task ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(task.title, style = MaterialTheme.typography.labelLarge)
                                                Text("${task.subject} • ${task.priority}", style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(onClick = { onAddTaskToPlanner(task.title, task.subject, task.priority) }) {
                                                Icon(Icons.Filled.AddTask, contentDescription = "Add to planner", tint = AccentTeal)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                items(aiState.messages, key = { it.id }) { bubble ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (bubble.fromUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (bubble.fromUser) BrandViolet else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Text(
                                text = bubble.text,
                                modifier = Modifier.padding(14.dp),
                                color = if (bubble.fromUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (aiState.errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = aiState.errorMessage,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickPrompts = listOf(
                    "Build a 2-hour Pomodoro revision sprint",
                    "Quiz me with 3 active-recall questions",
                    "How do I improve my focus streak?"
                )
                items(quickPrompts) { q ->
                    AssistChip(
                        onClick = { onAsk(q) },
                        label = { Text(q) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Ask AI Study Coach...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (prompt.isNotBlank() && !aiState.streaming) {
                            onAsk(prompt)
                            prompt = ""
                        }
                    },
                    enabled = !aiState.streaming,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandViolet)
                        .testTag("ai_send_btn")
                ) {
                    if (aiState.streaming) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask AI", tint = Color.White)
                    }
                }
            }
        }
    }
}
