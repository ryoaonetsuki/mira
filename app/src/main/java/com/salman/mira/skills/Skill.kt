package com.salman.mira.skills

import org.json.JSONObject

/**
 * A skill = downloadable capability pack.
 * Manifest lives on Cloudflare Pages; packs download on demand.
 * No user data ever touches the backend.
 */
data class Skill(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val downloadUrl: String,
    val sizeBytes: Long = 0,
    val minAppVersion: String = "0.1.0",
) {
    fun toJson() = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("version", version)
        .put("description", description)
        .put("downloadUrl", downloadUrl)
        .put("sizeBytes", sizeBytes)
        .put("minAppVersion", minAppVersion)

    companion object {
        fun fromJson(o: JSONObject) = Skill(
            id = o.optString("id"),
            name = o.optString("name"),
            version = o.optString("version"),
            description = o.optString("description"),
            downloadUrl = o.optString("downloadUrl"),
            sizeBytes = o.optLong("sizeBytes"),
            minAppVersion = o.optString("minAppVersion", "0.1.0"),
        )
    }
}
