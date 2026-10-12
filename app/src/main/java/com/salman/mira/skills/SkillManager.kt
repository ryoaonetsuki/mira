package com.salman.mira.skills

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the skill catalog from the Mira manifest URL (Cloudflare Pages),
 * downloads skill packs on demand into local storage.
 */
class SkillManager(context: Context) {
    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "mira/skills").apply { mkdirs() }

    /** Skill catalog served by the Mira Cloudflare Worker (app content only). */
    var manifestUrl: String = "https://mira-skills.salman-dev.workers.dev/skills.json"

    suspend fun fetchCatalog(): Result<List<Skill>> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(manifestUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000; readTimeout = 15000
            }
            if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
            val arr = JSONArray(conn.inputStream.bufferedReader().readText())
            (0 until arr.length()).map { Skill.fromJson(arr.getJSONObject(it)) }
        }
    }

    fun installed(): List<Skill> = dir.listFiles { f -> f.extension == "json" }
        ?.mapNotNull { runCatching { Skill.fromJson(JSONObject(it.readText())) }.getOrNull() }
        ?: emptyList()

    fun isInstalled(id: String): Boolean = File(dir, "$id.json").exists()

    suspend fun install(skill: Skill, onProgress: (Int) -> Unit = {}): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Download the pack (zip) to local storage.
                val packFile = File(dir, "${skill.id}.zip")
                val conn = (URL(skill.downloadUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20000; readTimeout = 60000
                }
                if (conn.responseCode != 200) throw Exception("HTTP ${conn.responseCode}")
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: skill.sizeBytes
                conn.inputStream.use { input ->
                    packFile.outputStream().use { output ->
                        val buf = ByteArray(32 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
                // Record the install.
                File(dir, "${skill.id}.json").writeText(skill.toJson().toString(2))
                onProgress(100)
            }
        }

    fun uninstall(id: String) {
        File(dir, "$id.json").delete()
        File(dir, "$id.zip").delete()
    }

    fun packFile(id: String): File = File(dir, "$id.zip")
}
