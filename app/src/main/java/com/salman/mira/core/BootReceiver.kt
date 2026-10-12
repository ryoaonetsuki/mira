package com.salman.mira.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Auto-start after reboot (verified in Maya's manifest). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.startForegroundService(
                Intent(context, MiraForegroundService::class.java)
            )
        }
    }
}
