package com.booster.shizuku

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Exported receiver that lets DzinLabs Gaming VPN's Game Hub trigger a
 * one-tap quick boost (no game relaunch) right before launching a game.
 *
 * Action: com.booster.shizuku.ACTION_QUICK_BOOST
 * Send as an explicit broadcast with setPackage("com.booster.shizuku").
 *
 * Requires Shizuku server running + authorized; if Shizuku isn't ready the
 * boost simply no-ops (same behavior as the floating bubble).
 */
class QuickBoostReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val appCtx = context.applicationContext
        Thread {
            try {
                ShizukuBooster.boostDevice(appCtx, logCallback = {}, launchGame = false)
            } catch (_: Exception) {
            }
        }.start()
    }

    companion object {
        const val ACTION = "com.booster.shizuku.ACTION_QUICK_BOOST"
    }
}
