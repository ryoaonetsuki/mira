package com.salman.mira.llm

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One chat message. */
data class ChatMessage(val role: String, val text: String) {
    // role: "user" | "model"  (mapped to "assistant" for OpenAI-style APIs)
}

/** Provider config read from DataStore after onboarding. */
data class LlmConfig(
    val provider: String,   // "gemini" | "openrouter" | "custom"
    val apiKey: String,
    val baseUrl: String = "",  // custom only, e.g. https://…/v1
    val model: String = ""     // openrouter/custom; gemini defaults below
)

/** Common chat interface. The key never leaves the phone. */
interface LlmProvider {
    suspend fun chat(messages: List<ChatMessage>, system: String? = null): String
}

object LlmProviders {
    fun create(config: LlmConfig): LlmProvider = when (config.provider) {
        "openrouter" -> OpenAiCompatibleProvider(
            baseUrl = "https://openrouter.ai/api/v1",
            apiKey = config.apiKey,
            model = config.model.ifBlank { "meta-llama/llama-3.3-70b-instruct:free" }
        )
        "custom" -> OpenAiCompatibleProvider(
            baseUrl = config.baseUrl,
            apiKey = config.apiKey,
            model = config.model
        )
        else -> GeminiProvider(config.apiKey)
    }
}

/** Gemini via Google AI Studio key (free tier). */
private class GeminiProvider(apiKey: String) : LlmProvider {
    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = apiKey
    )

    override suspend fun chat(messages: List<ChatMessage>, system: String?): String =
        withContext(Dispatchers.IO) {
            val history = messages.dropLast(1).map {
                com.google.ai.client.generativeai.type.content(
                    if (it.role == "user") "user" else "model"
                ) { text(it.text) }
            }
            val chat = model.startChat(history)
            // System prompt folded into first user turn (simple + robust)
            val last = messages.last()
            val prompt = if (system != null) "$system\n\n${last.text}" else last.text
            chat.sendMessage(prompt).text ?: ""
        }
}

/** Any OpenAI-compatible chat-completions endpoint (OpenRouter, self-hosted, …). */
private class OpenAiCompatibleProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : LlmProvider {

    override suspend fun chat(messages: List<ChatMessage>, system: String?): String =
        withContext(Dispatchers.IO) {
            val arr = JSONArray()
            if (system != null) {
                arr.put(JSONObject().put("role", "system").put("content", system))
            }
            messages.forEach {
                arr.put(JSONObject()
                    .put("role", if (it.role == "user") "user" else "assistant")
                    .put("content", it.text))
            }
            val body = JSONObject()
                .put("model", model)
                .put("messages", arr)
                .toString()

            val conn = URL("$baseUrl/chat/completions").openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.outputStream.use { it.write(body.toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val resp = stream.bufferedReader().readText()
                if (code !in 200..299) error("LLM HTTP $code: ${resp.take(300)}")
                JSONObject(resp)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } finally {
                conn.disconnect()
            }
        }
}
