package com.salman.mira.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Local-first memory. Everything lives in the phone's app folder:
 *   <filesDir>/memory/
 *     facts.json      — long-term facts ("user's birthday is …")
 *     prefs.json      — preferences
 *     history.jsonl   — conversation log (append-only)
 *
 * No account, no server. Backup/restore from inside the app via
 * Storage Access Framework (user picks the file location).
 */
class MemoryStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "memory").apply { mkdirs() }

    private fun readJson(name: String): JSONObject {
        val f = File(dir, name)
        return if (f.exists()) JSONObject(f.readText()) else JSONObject()
    }

    private fun writeJson(name: String, obj: JSONObject) {
        File(dir, name).writeText(obj.toString(2))
    }

    // ---- Facts ----
    fun putFact(key: String, value: String) {
        val o = readJson("facts.json").put(key, value)
        writeJson("facts.json", o)
    }

    fun getFact(key: String): String? =
        readJson("facts.json").optString(key, null)

    fun allFacts(): Map<String, String> {
        val o = readJson("facts.json")
        return o.keys().asSequence().associateWith { o.getString(it) }
    }

    // ---- Preferences ----
    fun putPref(key: String, value: String) {
        val o = readJson("prefs.json").put(key, value)
        writeJson("prefs.json", o)
    }

    fun getPref(key: String): String? =
        readJson("prefs.json").optString(key, null)

    // ---- History (append-only JSONL) ----
    fun appendHistory(role: String, text: String) {
        val line = JSONObject()
            .put("ts", System.currentTimeMillis())
            .put("role", role)
            .put("text", text)
            .toString()
        File(dir, "history.jsonl").appendText(line + "\n")
    }

    fun recentHistory(limit: Int = 50): List<JSONObject> {
        val f = File(dir, "history.jsonl")
        if (!f.exists()) return emptyList()
        return f.readLines().takeLast(limit).mapNotNull {
            runCatching { JSONObject(it) }.getOrNull()
        }
    }

    // ---- Backup: zip the whole memory dir to a user-chosen location ----
    suspend fun backupTo(uri: Uri) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            ZipOutputStream(out).use { zip ->
                dir.walkTopDown().filter { it.isFile }.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.relativeTo(dir).path))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Cannot open output")
    }

    // ---- Restore: replace memory dir from a backup zip ----
    suspend fun restoreFrom(uri: Uri) = withContext(Dispatchers.IO) {
        // Safety: keep a pre-restore copy
        val safety = File(context.filesDir, "memory.bak.${System.currentTimeMillis()}")
        if (dir.exists()) dir.copyRecursively(safety, overwrite = true)
        try {
            context.contentResolver.openInputStream(uri)?.use { inp ->
                ZipInputStream(inp).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        // Zip-slip guard
                        val target = File(dir, entry.name).canonicalFile
                        require(target.path.startsWith(dir.canonicalPath)) { "Bad zip entry" }
                        if (!entry.isDirectory) {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error("Cannot open input")
        } catch (t: Throwable) {
            // Roll back on failure
            dir.deleteRecursively()
            safety.copyRecursively(dir, overwrite = true)
            throw t
        }
    }

    /** Where the memory lives, to show the user in Settings. */
    fun location(): String = dir.absolutePath
}
