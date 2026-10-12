package com.salman.mira.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salman.mira.ui.chat.ChatScreen
import com.salman.mira.ui.home.HomeScreen
import com.salman.mira.ui.macros.MacrosScreen
import com.salman.mira.ui.memories.MemoriesScreen
import com.salman.mira.ui.skills.SkillsScreen
import com.salman.mira.ui.scan.ScanScreen
import com.salman.mira.ui.settings.SettingsScreen
import com.salman.mira.ui.theme.MiraPrimary
import com.salman.mira.ui.theme.MiraTextSecondary

private data class Tab(
    val route: String,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
)

private val tabsLeft = listOf(
    Tab("home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab("scan", "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
)
private val tabsRight = listOf(
    Tab("memories", "Memories", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    Tab("chat", "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
)

/**
 * Main app shell: bottom navigation with a center voice FAB.
 * State-based navigation (no nav library) keeps the APK small.
 */
@Composable
fun MiraNav(onMicTap: () -> Unit = {}, onReset: () -> Unit = {}) {
    var currentRoute by remember { mutableStateOf("home") }

    Scaffold(
        bottomBar = {
            MiraBottomBar(
                currentRoute = currentRoute,
                onTabSelected = { currentRoute = it },
                onMicTap = onMicTap,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (currentRoute) {
                "home" -> HomeScreen(
                    onOpenChat = { currentRoute = "chat" },
                    onOpenSettings = { currentRoute = "settings" },
                    onOpenMacros = { currentRoute = "macros" },
                    onOpenSkills = { currentRoute = "skills" },
                )
                "macros" -> MacrosScreen()
                "skills" -> SkillsScreen()
                "scan" -> ScanScreen()
                "memories" -> MemoriesScreen()
                "chat" -> ChatScreen()
                "settings" -> SettingsScreen(
                    onBack = { currentRoute = "home" },
                    onReset = onReset,
                )
            }
        }
    }
}

@Composable
private fun MiraBottomBar(
    currentRoute: String,
    onTabSelected: (String) -> Unit,
    onMicTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabsLeft.forEach { tab ->
                MiraTabItem(tab, selected = currentRoute == tab.route, onClick = { onTabSelected(tab.route) })
            }
        }

        // Center voice button — the signature control.
        Box(
            modifier = Modifier
                .size(60.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MiraPrimary)
                .clickable(onClick = onMicTap),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabsRight.forEach { tab ->
                MiraTabItem(tab, selected = currentRoute == tab.route, onClick = { onTabSelected(tab.route) })
            }
        }
    }
}

@Composable
private fun MiraTabItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MiraPrimary else MiraTextSecondary
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected) tab.filled else tab.outlined,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelLarge.copy(
                color = tint,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
            ),
        )
    }
}
