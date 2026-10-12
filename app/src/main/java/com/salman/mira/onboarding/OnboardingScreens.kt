package com.salman.mira.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore("mira_prefs")
val LLM_PROVIDER = stringPreferencesKey("llm_provider")   // gemini | openrouter | custom
val LLM_API_KEY = stringPreferencesKey("llm_api_key")
val LLM_BASE_URL = stringPreferencesKey("llm_base_url")   // custom only
val LLM_MODEL = stringPreferencesKey("llm_model")         // openrouter/custom

const val AI_STUDIO_URL = "https://aistudio.google.com/apikey"
const val OPENROUTER_URL = "https://openrouter.ai/keys"

/** Page 1 — runtime permissions (mic, notifications, …) */
@Composable
fun PermissionsScreen(onDone: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onDone() }
    OnboardingPage(
        title = "Permissions",
        body = "Microphone for voice, notifications to stay alive in background.",
        button = "Continue"
    ) {
        launcher.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        ))
    }
}

/** Page 2 — battery exemption + accessibility service (the automation engine) */
@Composable
fun SystemAccessScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    OnboardingPage(
        title = "System access",
        body = "1. Exempt from battery optimization so the assistant keeps running.\n" +
               "2. Enable the Accessibility service — this is how it taps buttons in other apps for you.",
        button = "Next"
    ) {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${ctx.packageName}")
            })
        }
        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        onDone()
    }
}

/**
 * Page 3 — STRICT BYOK. The app does NOT configure without the user's own key.
 * Providers: Gemini (AI Studio, free) | OpenRouter | Custom OpenAI-compatible base URL.
 * No fallback "our" key — the key never leaves the phone.
 */
@Composable
fun ApiKeyScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var provider by remember { mutableStateOf("gemini") }
    var key by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val providerLabel = when (provider) {
        "openrouter" -> "OpenRouter"
        "custom" -> "Custom"
        else -> "Gemini"
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Your AI key", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bring your own free key — the app talks to the AI directly from your phone. " +
            "Without a key the app cannot be configured."
        )
        Spacer(Modifier.height(16.dp))

        // Provider picker
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = providerLabel, onValueChange = {},
                readOnly = true, label = { Text("Provider") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Gemini (free)") },
                    onClick = { provider = "gemini"; expanded = false })
                DropdownMenuItem(text = { Text("OpenRouter (free models)") },
                    onClick = { provider = "openrouter"; expanded = false })
                DropdownMenuItem(text = { Text("Custom (OpenAI-compatible URL)") },
                    onClick = { provider = "custom"; expanded = false })
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = key, onValueChange = { key = it; error = null },
            label = { Text("API key (required)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        if (provider == "custom") {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = baseUrl, onValueChange = { baseUrl = it },
                label = { Text("Base URL (e.g. https://…/v1)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Model name") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (provider == "openrouter") {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = model, onValueChange = { model = it },
                label = { Text("Model (e.g. meta-llama/llama-3.3-70b-instruct:free)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        TextButton(onClick = {
            val url = if (provider == "openrouter") OPENROUTER_URL else AI_STUDIO_URL
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }) {
            Text(if (provider == "openrouter") "Get a free OpenRouter key" else "Get a free Gemini key")
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                if (key.isBlank()) {
                    error = "API key is required — the app cannot work without it."
                    return@Button
                }
                if (provider == "custom" && baseUrl.isBlank()) {
                    error = "Base URL is required for a custom provider."
                    return@Button
                }
                scope.launch {
                    ctx.dataStore.edit {
                        it[LLM_PROVIDER] = provider
                        it[LLM_API_KEY] = key.trim()
                        it[LLM_BASE_URL] = baseUrl.trim().trimEnd('/')
                        it[LLM_MODEL] = model.trim()
                    }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Next") }
    }
}

/** Sequences the three onboarding pages, then finishes. */
@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    when (page) {
        0 -> PermissionsScreen(onDone = { page = 1 })
        1 -> SystemAccessScreen(onDone = { page = 2 })
        else -> ApiKeyScreen(onDone = onFinish)
    }
}

@Composable
private fun OnboardingPage(
    title: String, body: String, button: String, onButton: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(body)
        Spacer(Modifier.weight(1f))
        Button(onClick = onButton, modifier = Modifier.fillMaxWidth()) {
            Text(button)
        }
    }
}
