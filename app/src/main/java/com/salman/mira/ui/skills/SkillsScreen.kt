package com.salman.mira.ui.skills

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.salman.mira.skills.Skill
import com.salman.mira.skills.SkillManager
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary
import kotlinx.coroutines.launch

/** Browse and install skill packs from the Mira catalog. */
@Composable
fun SkillsScreen() {
    val context = LocalContext.current
    val manager = remember { SkillManager(context) }
    val scope = rememberCoroutineScope()
    var catalog by remember { mutableStateOf<List<Skill>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var installedIds by remember { mutableStateOf(manager.installed().map { it.id }.toSet()) }

    LaunchedEffect(Unit) {
        manager.fetchCatalog()
            .onSuccess { catalog = it }
            .onFailure { error = it.message?.take(120) ?: "Couldn't load catalog" }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Skill Store", style = MaterialTheme.typography.headlineSmall)
        Text("Downloadable capability packs. Stored on your phone.",
            color = MiraTextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        when {
            error != null -> Text("Catalog unavailable: $error", color = MiraTextSecondary)
            catalog == null -> CircularProgressIndicator()
            catalog!!.isEmpty() -> Text("No skills published yet.", color = MiraTextSecondary)
            else -> LazyColumn {
                items(catalog!!, key = { it.id }) { skill ->
                    val installed = skill.id in installedIds
                    val busy = busyId == skill.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MiraSurfaceHigh),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(skill.name, style = MaterialTheme.typography.titleMedium)
                                Text("v${skill.version}", color = MiraTextSecondary,
                                    style = MaterialTheme.typography.bodySmall)
                                Text(skill.description, color = MiraTextSecondary,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            } else if (installed) {
                                OutlinedButton(onClick = {
                                    manager.uninstall(skill.id)
                                    installedIds = installedIds - skill.id
                                }) { Text("Remove") }
                            } else {
                                Button(onClick = {
                                    busyId = skill.id
                                    scope.launch {
                                        manager.install(skill)
                                            .onSuccess { installedIds = installedIds + skill.id }
                                            .onFailure { error = it.message?.take(120) }
                                        busyId = null
                                    }
                                }) { Text("Install") }
                            }
                        }
                    }
                }
            }
        }
    }
}
