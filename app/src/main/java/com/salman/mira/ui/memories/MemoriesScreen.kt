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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salman.mira.automation.MacroExecutor
import com.salman.mira.automation.MacroStore
import com.salman.mira.data.MemoryStore
import com.salman.mira.ui.theme.MiraPrimary
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary
import kotlinx.coroutines.launch

/** Memories: real macros + facts Mira has learned, all stored on this phone. */
@Composable
fun MemoriesScreen() {
    val context = LocalContext.current
    val macroStore = remember { MacroStore(context) }
    val memory = remember { MemoryStore(context) }
    val scope = rememberCoroutineScope()
    var macros by remember { mutableStateOf(macroStore.list()) }
    var facts by remember { mutableStateOf(memory.allFacts()) }
    var status by remember { mutableStateOf<String?>(null) }

    // Refresh when returning to this tab.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        macros = macroStore.list()
        facts = memory.allFacts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Memories", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "What Mira learned. All stored on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MiraTextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        Text("Automations", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (macros.isEmpty()) {
            Text("No macros yet — create one from the Macros tab.",
                color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(macros, key = { it.id }) { macro ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(macro.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("${macro.steps.size} steps",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MiraTextSecondary)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = {
                            scope.launch {
                                status = when (MacroExecutor.run(macro)) {
                                    is MacroExecutor.Result.Ok -> "\"${macro.name}\" done."
                                    is MacroExecutor.Result.Failed -> "Couldn't finish \"${macro.name}\"."
                                    MacroExecutor.Result.NoService -> "Enable accessibility service first."
                                }
                            }
                        }) {
                            Icon(Icons.Filled.PlayArrow, "Run", tint = MiraPrimary,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Facts", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (facts.isEmpty()) {
            Text("Nothing remembered yet. Tell Mira things like \"remember my birthday is…\".",
                color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            facts.forEach { (k, v) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(k, style = MaterialTheme.typography.titleSmall)
                        Text(v, color = MiraTextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        status?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MiraTextSecondary) }
        Spacer(Modifier.height(16.dp))
    }
}
