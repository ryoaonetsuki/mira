package com.salman.mira.automation

import org.json.JSONArray
import org.json.JSONObject

/**
 * A macro = named, replayable automation script.
 * Steps are data, not code — safe to store, share, and AI-generate.
 */
data class MacroStep(
    val action: String,           // tap_text | type_text | open_app | wait_text | back | delay | scroll
    val arg: String = "",         // text, app name, millis, etc.
    val timeoutMs: Long = 5000,
) {
    fun toJson() = JSONObject()
        .put("action", action)
        .put("arg", arg)
        .put("timeoutMs", timeoutMs)

    companion object {
        fun fromJson(o: JSONObject) = MacroStep(
            action = o.optString("action"),
            arg = o.optString("arg"),
            timeoutMs = o.optLong("timeoutMs", 5000),
        )
    }
}

data class Macro(
    val id: String,
    val name: String,
    val triggers: List<String> = emptyList(),  // phrases that invoke it by voice
    val steps: List<MacroStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("triggers", JSONArray(triggers))
        .put("steps", JSONArray(steps.map { it.toJson() }))
        .put("createdAt", createdAt)

    companion object {
        fun fromJson(o: JSONObject): Macro {
            val steps = mutableListOf<MacroStep>()
            val arr = o.optJSONArray("steps") ?: JSONArray()
            for (i in 0 until arr.length()) steps.add(MacroStep.fromJson(arr.getJSONObject(i)))
            val trig = mutableListOf<String>()
            val tarr = o.optJSONArray("triggers") ?: JSONArray()
            for (i in 0 until tarr.length()) trig.add(tarr.optString(i))
            return Macro(
                id = o.optString("id"),
                name = o.optString("name"),
                triggers = trig,
                steps = steps,
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
    }
}
