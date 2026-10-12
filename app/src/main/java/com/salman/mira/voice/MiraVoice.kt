package com.salman.mira.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Text-to-speech wrapper. Prefers Bangla voice if the device has one,
 * falls back to the default locale otherwise. Free, on-device, no network.
 */
class MiraVoice(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    var enabled = true

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val t = tts ?: return@TextToSpeech
                // Prefer Bangla (Bangladesh), fall back gracefully.
                val bn = Locale("bn", "BD")
                val res = t.setLanguage(bn)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    t.language = Locale.getDefault()
                }
                t.setSpeechRate(1.0f)
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        // Strip markdown-ish noise for cleaner speech.
        val clean = text
            .replace(Regex("[*_`#>]+"), "")
            .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
            .trim()
            .take(1200)
        if (clean.isEmpty()) return
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "mira-${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
