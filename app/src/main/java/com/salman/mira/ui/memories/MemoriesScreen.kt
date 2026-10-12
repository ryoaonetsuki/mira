package com.salman.mira.ui.memories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.salman.mira.ui.theme.MiraPrimary
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary

private data class TrainedTask(val name: String, val detail: String)

private val demoTasks = listOf(
    TrainedTask("Morning briefing", "Weather + calendar at 7:00 AM"),
    TrainedTask("WhatsApp to Amma", "Send good-night message at 10 PM"),
)

/** Memories: trained automations + facts. Teaching flow arrives in a later phase. */
@Composable
fun MemoriesScreen() {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {},
                containerColor = MiraPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, "Teach new task")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Memories", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tasks Mira learned by watching you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MiraTextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(demoTasks) { task ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    task.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MiraTextSecondary,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {}) {
                                Icon(Icons.Filled.PlayArrow, "Run", tint = MiraPrimary, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
