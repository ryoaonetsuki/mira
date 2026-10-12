package com.salman.mira.automation

import com.salman.mira.llm.ChatMessage
import com.salman.mira.llm.LlmConfig
import com.salman.mira.llm.LlmProviders
import org.json.JSONArray

private const val PLANNER_PROMPT = """
You generate phone-automation macros as JSON. Reply with ONLY a JSON array, no other text.
Each step: {"action":"<one of tap_text|type_text|open_app|wait_text|back|scroll|delay>","arg":"<text>"}.
Rules:
- tap_text: visible label/button text to tap (e.g. "Search", "Send").
- type_text: text to type into the focused field. Always precede with tap_text on the field when possible.
- open_app: app display name (e.g. "YouTube", "WhatsApp", "Settings").
- wait_text: text expected on the next screen; arg = that text.
- back: arg = "" (goes back).
- scroll: arg = "" (scrolls down).
- delay: arg = milliseconds as string (e.g. "1500").
Keep macros short (3-8 steps). Prefer labels over coordinates.
Example for "open YouTube and search cats":
[{"action":"open_app","arg":"YouTube"},{"action":"wait_text","arg":"Search"},{"action":"tap_text","arg":"Search"},{"action":"type_text","arg":"cats"}]
""".trimIndent()

/** Uses the LLM to turn "make a macro for X" into a verified Macro. */
object MacroPlanner {
    suspend fun plan(
        config: LlmConfig,
        store: MacroStore,
        request: String,
        name: String,
    ): Result<Macro> = runCatching {
        val raw = LlmProviders.create(config).chat(
            listOf(ChatMessage("user", "Task: $request")),
            PLANNER_PROMPT,
        )
        val jsonText = raw.substringAfter('[').substringBeforeLast(']').let { "[$it]" }
        val arr = JSONArray(jsonText)
        val steps = (0 until arr.length()).map { MacroStep.fromJson(arr.getJSONObject(it)) }
        require(steps.isNotEmpty()) { "Planner returned no steps" }
        require(steps.size <= 12) { "Planner returned too many steps" }
        val valid = setOf("tap_text", "type_text", "open_app", "wait_text", "back", "scroll", "delay")
        steps.forEach { require(it.action in valid) { "Unknown action: ${it.action}" } }
        val macro = Macro(
            id = store.newId(),
            name = name,
            triggers = listOf(request.lowercase()),
            steps = steps,
        )
        store.save(macro)
        macro
    }
}
