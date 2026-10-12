package com.salman.mira

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.salman.mira.onboarding.OnboardingFlow
import com.salman.mira.onboarding.dataStore
import com.salman.mira.ui.nav.MiraNav
import com.salman.mira.ui.theme.MiraTheme
import kotlinx.coroutines.launch

/**
 * Entry point.
 * Onboarding (verified Maya-style flow, our own UI):
 *  1. Runtime permissions → Continue
 *  2. Battery exemption + Accessibility service → Next
 *  3. Paste own Gemini API key (Google AI Studio, free) → Next → app opens
 * Then the main shell: Home | Scan | mic | Memories | Chat.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val prefs = remember {
                        context.getSharedPreferences("mira_prefs", MODE_PRIVATE)
                    }
                    var onboarded by remember {
                        mutableStateOf(prefs.getBoolean("onboarded", false))
                    }
                    if (onboarded) {
                        val scope = rememberCoroutineScope()
                        MiraNav(
                            onReset = {
                                scope.launch {
                                    context.dataStore.edit { it.clear() }
                                    prefs.edit().putBoolean("onboarded", false).apply()
                                    onboarded = false
                                }
                            },
                        )
                    } else {
                        OnboardingFlow(
                            onFinish = {
                                prefs.edit().putBoolean("onboarded", true).apply()
                                onboarded = true
                            },
                        )
                    }
                }
            }
        }
    }
}
