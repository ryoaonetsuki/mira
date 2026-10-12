package com.salman.mira.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.salman.mira.automation.MacroExecutor
import com.salman.mira.automation.MacroStore
import com.salman.mira.core.FastPath
import com.salman.mira.data.MemoryStore
import com.salman.mira.llm.LlmConfig
import com.salman.mira.llm.LlmProviders
import com.salman.mira.voice.MiraListener
import com.salman.mira.voice.MiraVoice
import com.salman.mira.onboarding.LLM_API_KEY
import com.salman.mira.onboarding.LLM_BASE_URL
import com.salman.mira.onboarding.LLM_MODEL
import com.salman.mira.onboarding.LLM_PROVIDER
import com.salman.mira.onboarding.dataStore
import com.salman.mira.ui.theme.MiraPrimary
import com.salman.mira.ui.theme.MiraSurfaceHigh
import com.salman.mira.ui.theme.MiraTextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.salman.mira.llm.ChatMessage as LlmMessage

private data class UiMessage(val text: String, val fromUser: Boolean)

private const val SYSTEM_PROMPT =
    "You are Mira, a helpful, concise AI assistant inside an Android app. " +
        "Reply in the user's language. Keep answers short unless asked for detail."

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val memory = remember { MemoryStore(context) }
    val messages = remember {
        mutableStateListOf(
            UiMessage("Hi, I'm Mira. Ask me anything.", fromUser = false),
        )
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var voiceOn by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val voice = remember { MiraVoice(context) }
    val listener = remember { MiraListener(context) }
    val macroStore = remember { MacroStore(context) }
    DisposableEffect(Unit) { onDispose { voice.shutdown(); listener.destroy() } }

    suspend fun scrollToEnd() {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun speakReply(text: String) {
        voice.enabled = voiceOn
        if (voiceOn) voice.speak(text)
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || sending) return
        messages.add(UiMessage(clean, fromUser = true))
        input = ""
        sending = true
        scope.launch {
            scrollToEnd()
            // Macro trigger: "run <name>" or a trigger phrase.
            val macro = macroStore.matchTrigger(clean)
                ?: clean.removePrefix("run ").removePrefix("চালাও ").trim().let { q ->
                    macroStore.list().firstOrNull { it.name.equals(q, ignoreCase = true) }
                }
            if (macro != null) {
                messages.add(UiMessage("Running \"${macro.name}\"…", fromUser = false))
                val res = MacroExecutor.run(macro)
                val done = when (res) {
                    is MacroExecutor.Result.Ok -> "\"${macro.name}\" done."
                    is MacroExecutor.Result.Failed -> "Couldn't finish \"${macro.name}\" (step ${res.stepIndex + 1})."
                    MacroExecutor.Result.NoService -> "Enable Mira's accessibility service first (Settings)."
                }
                messages.add(UiMessage(done, fromUser = false))
                speakReply(done)
                sending = false
                scrollToEnd()
                return@launch
            }
            // Fast-path: instant local answer, no LLM.
            val fast = FastPath.answer(clean)
            if (fast != null) {
                messages.add(UiMessage(fast, fromUser = false))
                runCatching {
                    memory.appendHistory("user", clean)
                    memory.appendHistory("model", fast)
                }
                speakReply(fast)
                sending = false
                scrollToEnd()
                return@launch
            }
            try {
                val config = context.dataStore.data.map { p ->
                    LlmConfig(
                        provider = p[LLM_PROVIDER] ?: "gemini",
                        apiKey = p[LLM_API_KEY].orEmpty(),
                        baseUrl = p[LLM_BASE_URL].orEmpty(),
                        model = p[LLM_MODEL].orEmpty(),
                    )
                }.first()
                if (config.apiKey.isBlank()) {
                    messages.add(UiMessage("No API key configured. Please finish setup first.", fromUser = false))
                } else {
                    val history = messages.map {
                        LlmMessage(if (it.fromUser) "user" else "model", it.text)
                    }
                    val reply = LlmProviders.create(config).chat(history, SYSTEM_PROMPT)
                    val finalReply = reply.ifBlank { "(The model returned an empty reply.)" }
                    messages.add(UiMessage(finalReply, fromUser = false))
                    runCatching {
                        memory.appendHistory("user", clean)
                        memory.appendHistory("model", finalReply)
                    }
                    speakReply(finalReply)
                }
            } catch (e: Exception) {
                messages.add(UiMessage("Error: ${e.message?.take(220) ?: "unknown"}", fromUser = false))
            }
            sending = false
            scrollToEnd()
        }
    }

    fun startVoiceInput() {
        if (!listener.isAvailable) {
            messages.add(UiMessage("Voice input isn't available on this device.", fromUser = false))
            return
        }
        listener.onResult = { heard -> send(heard) }
        listener.onError = { err ->
            messages.add(UiMessage(err, fromUser = false))
            scope.launch { scrollToEnd() }
        }
        listener.onListening = { listening = it }
        listener.startListening()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MiraPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Text("M", style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Mira", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        listening -> "listening…"
                        sending -> "typing…"
                        else -> "Online"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = MiraTextSecondary),
                )
            }
            IconButton(onClick = { voiceOn = !voiceOn; if (!voiceOn) voice.stop() }) {
                Icon(
                    imageVector = if (voiceOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = if (voiceOn) "Mute voice" else "Unmute voice",
                    tint = MiraTextSecondary,
                )
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages) { msg -> MessageBubble(msg) }
            if (sending) {
                item { MessageBubble(UiMessage("…", fromUser = false)) }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Filled.AttachFile, "Attach", tint = MiraTextSecondary)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message Mira…", color = MiraTextSecondary) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MiraPrimary,
                    unfocusedBorderColor = MiraSurfaceHigh,
                    cursorColor = MiraPrimary,
                ),
            )
            IconButton(
                onClick = { if (input.isBlank()) startVoiceInput() else send(input) },
                enabled = !sending && !listening,
            ) {
                Icon(
                    imageVector = if (input.isBlank()) Icons.Filled.Mic else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (input.isBlank()) "Voice input" else "Send",
                    tint = MiraPrimary,
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: UiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = msg.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (msg.fromUser) 18.dp else 4.dp,
                        bottomEnd = if (msg.fromUser) 4.dp else 18.dp,
                    ),
                )
                .background(if (msg.fromUser) MiraPrimary else MiraSurfaceHigh)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (msg.fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
