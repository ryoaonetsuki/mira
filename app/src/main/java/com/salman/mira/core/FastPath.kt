package com.salman.mira.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fast-path intents: instant local answers for common commands,
 * no LLM call, no network. Bangla + English.
 */
object FastPath {
    fun answer(input: String): String? {
        val t = input.trim().lowercase(Locale.US)
        if (t.isEmpty()) return null

        // --- time ---
        if (t.contains("time") || t.contains("সময়") || t.contains("কয়টা") || t.contains("kotota")) {
            val time = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            return if (isBangla(t)) "এখন সময় $time" else "It's $time"
        }
        // --- date ---
        if (t.contains("date") || t.contains("তারিখ") || t.contains("আজকে") && t.contains("কত")) {
            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            return if (isBangla(t)) "আজ $date" else "Today is $date"
        }
        // --- greetings ---
        if (t in setOf("hi", "hello", "hey", "হাই", "হ্যালো", "আসসালামু আলাইকুম", "সালাম")) {
            return if (isBangla(t)) "হ্যালো! আমি মিরা, কীভাবে সাহায্য করতে পারি?" else "Hello! I'm Mira, how can I help?"
        }
        // --- who are you ---
        if (t.contains("who are you") || t.contains("তুমি কে") || t.contains("তোমার নাম")) {
            return if (isBangla(t)) "আমি মিরা, তোমার AI অ্যাসিস্ট্যান্ট।"
            else "I'm Mira, your AI assistant."
        }
        return null
    }

    /** "remember X is Y" / "মনে রাখো X হলো Y" → Pair(key, value), or null. */
    fun parseRemember(input: String): Pair<String, String>? {
        val t = input.trim()
        val en = Regex("(?i)^remember\\s+(.+?)\\s+is\\s+(.+)$").find(t)
        if (en != null) return en.groupValues[1].trim() to en.groupValues[2].trim()
        val bn = Regex("^মনে রাখো\\s+(.+?)\\s+(?:হলো|হচ্ছে)\\s+(.+)$").find(t)
        if (bn != null) return bn.groupValues[1].trim() to bn.groupValues[2].trim()
        return null
    }

    private fun isBangla(t: String): Boolean =
        t.any { it in '\u0980'..'\u09FF' }
}
