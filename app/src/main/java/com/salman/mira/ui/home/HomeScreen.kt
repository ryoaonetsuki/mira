package com.salman.mira.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salman.mira.ui.theme.MiraPrimary
import com.salman.mira.ui.theme.MiraSecondary
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary
import java.util.Calendar

private data class QuickAction(
    val label: String,
    val icon: ImageVector,
)

private val quickActions = listOf(
    QuickAction("Message", Icons.Filled.Message),
    QuickAction("Reminder", Icons.Filled.Alarm),
    QuickAction("Music", Icons.Filled.MusicNote),
    QuickAction("Camera", Icons.Filled.CameraAlt),
    QuickAction("Voice mode", Icons.Filled.GraphicEq),
)

/** Home: greeting, assistant orb, quick actions. Professional, no emojis. */
@Composable
fun HomeScreen(onOpenChat: () -> Unit, onOpenSettings: () -> Unit = {}) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mira",
                style = MaterialTheme.typography.headlineSmall,
            )
            Row {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Notifications, "Notifications", tint = MiraTextSecondary)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, "Settings", tint = MiraTextSecondary)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(text = greeting + ",", style = MaterialTheme.typography.bodyLarge.copy(color = MiraTextSecondary))
        Text(text = "What can I do for you?", style = MaterialTheme.typography.displaySmall)

        Spacer(Modifier.height(28.dp))

        // Assistant orb — soft breathing animation.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            MiraOrb()
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tap the mic and just speak",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(28.dp))

        Text(text = "Quick actions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(quickActions) { action ->
                QuickActionCard(action, onClick = onOpenChat)
            }
        }
    }
}

@Composable
private fun MiraOrb() {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )
    Box(
        modifier = Modifier
            .size(168.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(MiraPrimary.copy(alpha = 0.9f), MiraSecondary.copy(alpha = 0.55f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = MiraPrimary,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

@Composable
private fun QuickActionCard(action: QuickAction, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(108.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(action.icon, action.label, tint = MiraPrimary, modifier = Modifier.size(26.dp))
            Text(text = action.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
