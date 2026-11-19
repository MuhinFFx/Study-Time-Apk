package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.BrandViolet

fun parseColorSafe(hex: String, fallback: Color = BrandViolet): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun subjectIconForKey(key: String): ImageVector = when (key.lowercase()) {
    "code" -> Icons.Filled.Code
    "calculate" -> Icons.Filled.Calculate
    "science" -> Icons.Filled.Biotech
    "language" -> Icons.Filled.Language
    "art" -> Icons.Filled.Palette
    "psych" -> Icons.Filled.Psychology
    "school" -> Icons.Filled.School
    else -> Icons.Filled.Book
}

@Composable
fun AvatarBadge(
    emoji: String,
    colorHex: String,
    size: Dp = 46.dp,
    onlineStatus: String? = null
) {
    val bgColor = parseColorSafe(colorHex)
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(bgColor, bgColor.copy(alpha = 0.72f))
                    )
                )
                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji.ifBlank { "🦉" },
                fontSize = (size.value * 0.45f).sp
            )
        }
        if (onlineStatus != null && onlineStatus != "OFFLINE") {
            val dotColor = if (onlineStatus == "FOCUSING") Color(0xFFF59E0B) else Color(0xFF10B981)
            Box(
                modifier = Modifier
                    .size((size.value * 0.28f).dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun GradientBannerCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandViolet, AccentLavender, Color(0xFF4F46E5))
                    )
                )
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_state),
                contentDescription = title,
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.FilledTonalButton(onClick = onAction) {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
fun XpProgressBar(
    level: Long,
    currentInLevel: Long,
    neededInLevel: Long,
    totalXp: Long
) {
    val progress = if (neededInLevel > 0) (currentInLevel.toFloat() / neededInLevel.toFloat()).coerceIn(0f, 1f) else 1f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Level $level Scholar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currentInLevel / $neededInLevel XP ($totalXp total)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            strokeCap = StrokeCap.Round
        )
    }
}

fun formatDurationMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun formatTimerSeconds(totalSec: Int): String {
    val m = (totalSec / 60).coerceAtLeast(0)
    val s = (totalSec % 60).coerceAtLeast(0)
    return "%02d:%02d".format(m, s)
}

@Composable
fun AppBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scrimColor = if (isDark) {
        MaterialTheme.colorScheme.background.copy(alpha = 0.78f)
    } else {
        Color.White.copy(alpha = 0.14f)
    }
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.img_app_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
        )
        content()
    }
}

