package com.salman.mira.automation

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.UUID

/** Local macro persistence: <files>/mira/macros/<id>.json */
class MacroStore(context: Context) {
    private val dir = File(context.filesDir, "mira/macros").apply { mkdirs() }

    fun list(): List<Macro> = dir.listFiles { f -> f.extension == "json" }
        ?.mapNotNull { runCatching { Macro.fromJson(org.json.JSONObject(it.readText())) }.getOrNull() }
        ?.sortedByDescending { it.createdAt }
        ?: emptyList()

    fun save(macro: Macro) {
        File(dir, "${macro.id}.json").writeText(macro.toJson().toString(2))
    }

    fun delete(id: String) {
        File(dir, "$id.json").delete()
    }

    fun newId(): String = UUID.randomUUID().toString().take(8)

    /** Find a macro whose trigger phrase matches the voice command. */
    fun matchTrigger(command: String): Macro? {
        val c = command.lowercase()
        return list().firstOrNull { m -> m.triggers.any { c.contains(it.lowercase()) } }
    }
}
