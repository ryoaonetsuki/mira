package com.salman.mira.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salman.mira.data.MemoryStore
import com.salman.mira.onboarding.ApiKeyScreen
import com.salman.mira.onboarding.LLM_API_KEY
import com.salman.mira.onboarding.LLM_PROVIDER
import com.salman.mira.onboarding.dataStore
import com.salman.mira.ui.theme.MiraError
import com.salman.mira.ui.theme.MiraTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Settings: API key, memory backup/restore, app reset. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onReset: () -> Unit) {
    var page by remember { mutableStateOf("main") }
    if (page == "key") {
        ApiKeyScreen(onDone = { page = "main" })
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val memory = remember { MemoryStore(context) }
    var provider by remember { mutableStateOf("…") }
    var keyMasked by remember { mutableStateOf("…") }
    var status by remember { mutableStateOf<String?>(null) }

    // Load current config once
    LaunchedEffect(Unit) {
        val p = context.dataStore.data.first()
        provider = p[LLM_PROVIDER] ?: "gemini"
        val k = p[LLM_API_KEY].orEmpty()
        keyMasked = if (k.length <= 8) "••••" else k.take(4) + "••••" + k.takeLast(4)
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            status = runCatching { memory.backupTo(uri); "Backup saved." }
                .getOrElse { "Backup failed: ${it.message?.take(120)}" }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            status = runCatching { memory.restoreFrom(uri); "Restore done." }
                .getOrElse { "Restore failed: ${it.message?.take(120)}" }
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))

        Text("AI provider", style = MaterialTheme.typography.titleMedium)
        Text("$provider · $keyMasked", color = MiraTextSecondary)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { page = "key" }, modifier = Modifier.fillMaxWidth()) {
            Text("Change API key")
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Text("Memory (on this phone)", style = MaterialTheme.typography.titleMedium)
        Text(memory.location(), color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { backupLauncher.launch("mira-memory-backup.zip") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Backup memory to file") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { restoreLauncher.launch(arrayOf("application/zip")) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Restore memory from file") }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = MiraError),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset app (clears key + onboarding)") }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MiraTextSecondary)
        }
    }
}
