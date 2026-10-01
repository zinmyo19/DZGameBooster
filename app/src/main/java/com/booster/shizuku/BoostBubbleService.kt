package com.booster.shizuku

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import rikka.shizuku.Shizuku

/**
 * Floating boost bubble (chat-head style).
 * Tiny dot when idle -> tap to expand -> one-tap BOOST without leaving the game.
 * Quick boost skips the game relaunch (you're already in it).
 */
class BoostBubbleService : Service() {

    private lateinit var wm: WindowManager
    private var dot: View? = null
    private var dotParams: WindowManager.LayoutParams? = null
    private var panel: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var boosting = false

    companion object {
        const val CHANNEL_ID = "bubble_channel"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.booster.shizuku.STOP_BUBBLE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        // Same process as MainActivity, but re-register anyway in case the
        // process was restarted with only the service alive.
        Shizuku.addBinderReceivedListenerSticky(object : Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() { /* pingBinder() now true */ }
        })
        startForegroundNotif()
        addDot()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removePanel()
        dot?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        dot = null
        Prefs.setBoolean(this, Prefs.KEY_BUBBLE, false)
        super.onDestroy()
    }

    // ---------- notification ----------

    private fun startForegroundNotif() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Boost Bubble", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, BoostBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("◉ Boost Bubble active")
            .setContentText("Tap the floating dot anytime for one-tap boost")
            .setSmallIcon(android.R.drawable.presence_online)
            .setContentIntent(openApp)
            .addAction(0, "STOP", stop)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    // ---------- dot ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun overlayParams(w: Int, h: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams().apply {
            width = w
            height = h
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun addDot() {
        val d = View(this).apply { background = getDrawable(R.drawable.bubble_dot_v2) }
        dotParams = overlayParams(dp(26), dp(26)).apply {
            x = Prefs.getInt(this@BoostBubbleService, Prefs.KEY_BUBBLE_X, dp(8))
            y = Prefs.getInt(this@BoostBubbleService, Prefs.KEY_BUBBLE_Y, dp(320))
        }
        d.setOnTouchListener(DotTouchListener())
        wm.addView(d, dotParams)
        dot = d
    }

    private inner class DotTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = dotParams?.x ?: 0; startY = dotParams?.y ?: 0
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (dx * dx + dy * dy > 100) moved = true
                    dotParams?.let {
                        it.x = startX + dx
                        it.y = startY + dy
                        wm.updateViewLayout(dot, it)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        v.performClick()
                        togglePanel()
                    } else {
                        // persist position
                        Prefs.setInt(this@BoostBubbleService, Prefs.KEY_BUBBLE_X, dotParams?.x ?: 0)
                        Prefs.setInt(this@BoostBubbleService, Prefs.KEY_BUBBLE_Y, dotParams?.y ?: 0)
                    }
                    return true
                }
            }
            return false
        }
    }

    // ---------- panel: circular gaming quick-bar ----------

    private fun circleBtn(
        label: String,
        labelSize: Float,
        bgRes: Int,
        textColor: Int,
        tip: String,
        action: () -> Unit
    ): android.widget.TextView {
        // TextView (not Button) so the circle stays perfectly round —
        // Material buttons force min-width/insets that break the shape.
        return android.widget.TextView(this).apply {
            text = label
            textSize = labelSize
            gravity = Gravity.CENTER
            setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
            setTextColor(textColor)
            background = getDrawable(bgRes)
            tooltipText = tip // long-press hint (API 26+)
            isClickable = true
            isFocusable = false
            setOnClickListener { action() }
        }
    }

    private fun togglePanel() {
        if (panel != null) {
            removePanel()
            return
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = getDrawable(R.drawable.bubble_bar)
            val p = dp(8)
            setPadding(p, p, p, p)
        }
        // Slim gaming-bar sizing: 46dp circles, small fonts so labels fit inside
        val cell = dp(46)
        val lp = LinearLayout.LayoutParams(cell, cell).apply {
            leftMargin = dp(4)
            rightMargin = dp(4)
        }

        bar.addView(
            circleBtn("⚡", 18f, R.drawable.bubble_circle_boost,
                getColor(R.color.white), "Quick boost") { doQuickBoost() }, lp
        )
        bar.addView(
            circleBtn("🧹", 16f, R.drawable.bubble_circle_mem,
                getColor(R.color.white), "Memory clean") { doMemClean() }, lp
        )

        // Script runner: one circle per saved script (max 3 to keep the bar slim).
        // Shows up to 5 chars of the script name; long-press shows the full name.
        val scripts = ScriptStore.getAll(this).take(3)
        for (entry in scripts) {
            val short = entry.name.removeSuffix(".sh").take(5).uppercase().ifBlank { "▸" }
            bar.addView(
                circleBtn(short, 10f, R.drawable.bubble_circle_script,
                    getColor(R.color.accent_mint), entry.name) { runBubbleScript(entry) }, lp
            )
        }

        bar.addView(
            circleBtn("✕", 15f, R.drawable.bubble_circle_hide,
                getColor(R.color.text_secondary), "Hide") { removePanel() }, lp
        )

        val pp = overlayParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        ).apply {
            // appear just under the dot
            x = (dotParams?.x ?: 0)
            y = (dotParams?.y ?: dp(320)) + dp(40)
        }
        wm.addView(bar, pp)
        panel = bar
        // auto-collapse so it never blocks the game
        handler.postDelayed({ removePanel() }, 8000)
    }

    private fun removePanel() {
        handler.removeCallbacksAndMessages(null)
        panel?.let { try { wm.removeView(it) } catch (_: Exception) { } }
        panel = null
    }

    // ---------- boost actions ----------

    private fun doQuickBoost() {
        if (boosting) return
        boosting = true
        removePanel()
        // visual feedback on the dot
        handler.post {
            dot?.animate()?.alpha(0.25f)?.setDuration(150)?.withEndAction {
                dot?.animate()?.alpha(1f)?.setDuration(150)?.start()
            }?.start()
        }
        Thread {
            try {
                if (!ShizukuBooster.isShizukuAvailable() || !ShizukuBooster.hasShizukuPermission()) {
                    toast("Shizuku not ready — open the app to authorize")
                    openApp()
                    return@Thread
                }
                ShizukuBooster.boostDevice(this, {}, launchGame = false)
                toast("⚡ Quick boost done!")
            } finally {
                boosting = false
            }
        }.start()
    }

    private fun doMemClean() {
        if (boosting) return
        boosting = true
        removePanel()
        Thread {
            try {
                if (!ShizukuBooster.isShizukuAvailable() || !ShizukuBooster.hasShizukuPermission()) {
                    toast("Shizuku not ready — open the app to authorize")
                    openApp()
                    return@Thread
                }
                ShizukuBooster.cleanMemory {}
                toast("🧹 Memory cleaned!")
            } finally {
                boosting = false
            }
        }.start()
    }

    private fun runBubbleScript(entry: ScriptEntry) {
        if (boosting) return
        boosting = true
        removePanel()
        toast("▸ Running ${entry.name}...")
        Thread {
            try {
                if (!ShizukuBooster.isShizukuAvailable() || !ShizukuBooster.hasShizukuPermission()) {
                    toast("Shizuku not ready — open the app to authorize")
                    openApp()
                    return@Thread
                }
                val content = ScriptStore.readContent(this, entry)
                if (content.isNullOrBlank()) {
                    toast("Couldn't read ${entry.name} — reopen the app to re-seed scripts")
                    return@Thread
                }
                ShizukuBooster.runScript(content) { }
                toast("✓ ${entry.name} done")
            } finally {
                boosting = false
            }
        }.start()
    }

    private fun toast(msg: String) {
        handler.post { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    private fun openApp() {
        handler.post {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
