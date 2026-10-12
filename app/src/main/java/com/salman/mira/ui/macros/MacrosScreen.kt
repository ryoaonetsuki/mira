package com.salman.mira.ui.macros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.salman.mira.automation.Macro
import com.salman.mira.automation.MacroExecutor
import com.salman.mira.automation.MacroPlanner
import com.salman.mira.automation.MacroStore
import com.salman.mira.llm.LlmConfig
import com.salman.mira.onboarding.LLM_API_KEY
import com.salman.mira.onboarding.LLM_BASE_URL
import com.salman.mira.onboarding.LLM_MODEL
import com.salman.mira.onboarding.LLM_PROVIDER
import com.salman.mira.onboarding.dataStore
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** List, run, delete, and AI-generate macros. */
@Composable
fun MacrosScreen() {
    val context = LocalContext.current
    val store = remember { MacroStore(context) }
    val scope = rememberCoroutineScope()
    var macros by remember { mutableStateOf(store.list()) }
    var showDialog by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun refresh() { macros = store.list() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, "New macro")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(20.dp)) {
            Text("Macros", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Automations Mira can replay. Say \"run <name>\" by voice.",
                color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (macros.isEmpty()) {
                Text("No macros yet — tap + to create one.", color = MiraTextSecondary)
            }
            LazyColumn {
                items(macros, key = { it.id }) { m ->
                    MacroRow(
                        macro = m,
                        onRun = {
                            scope.launch {
                                status = "Running ${m.name}…"
                                status = when (val r = MacroExecutor.run(m)) {
                                    is MacroExecutor.Result.Ok -> "${m.name}: done."
                                    is MacroExecutor.Result.Failed ->
                                        "${m.name}: failed at step ${r.stepIndex + 1}."
                                    MacroExecutor.Result.NoService ->
                                        "Enable Mira's accessibility service first."
                                }
                            }
                        },
                        onDelete = { store.delete(m.id); refresh() },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            status?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MiraTextSecondary) }
        }
    }

    if (showDialog) {
        var task by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!busy) showDialog = false },
            title = { Text("New macro (AI-generated)") },
            text = {
                Column {
                    Text("Describe the task, e.g. \"open YouTube and search for cats\".",
                        color = MiraTextSecondary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Macro name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = task, onValueChange = { task = it },
                        label = { Text("What should it do?") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && task.isNotBlank() && name.isNotBlank(),
                    onClick = {
                        busy = true
                        scope.launch {
                            status = runCatching {
                                val cfg = context.dataStore.data.map { p ->
                                    LlmConfig(p[LLM_PROVIDER] ?: "gemini",
                                        p[LLM_API_KEY].orEmpty(),
                                        p[LLM_BASE_URL].orEmpty(),
                                        p[LLM_MODEL].orEmpty())
                                }.first()
                                MacroPlanner.plan(cfg, store, task, name).getOrThrow()
                                "Macro \"$name\" created."
                            }.getOrElse { "Couldn't generate: ${it.message?.take(120)}" }
                            busy = false; showDialog = false; refresh()
                        }
                    }
                ) { Text(if (busy) "Generating…" else "Generate") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MacroRow(macro: Macro, onRun: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onRun),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(macro.name, style = MaterialTheme.typography.titleMedium)
                Text("${macro.steps.size} steps", color = MiraTextSecondary,
                    style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onRun) { Icon(Icons.Filled.PlayArrow, "Run") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }
}
