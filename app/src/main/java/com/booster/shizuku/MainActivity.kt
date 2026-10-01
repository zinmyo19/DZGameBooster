package com.booster.shizuku

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.booster.shizuku.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity(), Shizuku.OnRequestPermissionResultListener,
    Shizuku.OnBinderReceivedListener, Shizuku.OnBinderDeadListener {

    private lateinit var binding: ActivityMainBinding
    private val mainHandler = Handler(Looper.getMainLooper())
    private val SHIZUKU_REQUEST_CODE = 404

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(this)
        // Sticky: fires immediately if binder already arrived, or later when it does.
        // The Shizuku server delivers the binder asynchronously after process start.
        Shizuku.addBinderReceivedListenerSticky(this)
        Shizuku.addBinderDeadListener(this)

        // Seed the pre-installed script pack (first run / new versions)
        ScriptStore.seedFromAssets(this)

        updateShizukuStatus()

        binding.btnGrantPermission.setOnClickListener {
            requestShizuku()
        }

        binding.btnBoost.setOnClickListener {
            if (checkOrRequestPermission()) {
                executeBoost()
            }
        }

        binding.btnClearLogs.setOnClickListener {
            binding.tvLogs.text = "> Log cleared_\n"
        }

        binding.tileGames.setOnClickListener {
            startActivity(Intent(this, GamesActivity::class.java))
        }

        binding.tileScripts.setOnClickListener {
            startActivity(Intent(this, ScriptsActivity::class.java))
        }

        binding.tileSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.tileAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.tileRestore.setOnClickListener {
            if (checkOrRequestPermission()) {
                executeRestore()
            }
        }

        binding.tileBubble.setOnClickListener {
            toggleBubble()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh target game + config summary (may have changed in Settings / Games)
        val pkg = Prefs.targetGame(this)
        val label = GameList.labelFor(this, pkg)
        binding.tvTargetGame.text = if (label == pkg) pkg else "$label\n$pkg"
        binding.tvBoostDesc.text = Prefs.summaryLine(this)
        updateBubbleBtn()
        updateShizukuStatus()
    }

    private fun updateBubbleBtn() {
        val on = Prefs.getBoolean(this, Prefs.KEY_BUBBLE, false)
        binding.tvBubbleLabel.text = if (on) "BUBBLE: ON" else "BUBBLE: OFF"
        binding.tvBubbleLabel.setTextColor(
            getColor(if (on) R.color.accent_mint else R.color.text_secondary)
        )
    }

    private fun toggleBubble() {
        if (Prefs.getBoolean(this, Prefs.KEY_BUBBLE, false)) {
            stopService(Intent(this, BoostBubbleService::class.java))
            Prefs.setBoolean(this, Prefs.KEY_BUBBLE, false)
            updateBubbleBtn()
            Toast.makeText(this, "Bubble removed", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow 'Display over other apps', then tap again", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        ContextCompat.startForegroundService(this, Intent(this, BoostBubbleService::class.java))
        Prefs.setBoolean(this, Prefs.KEY_BUBBLE, true)
        updateBubbleBtn()
        Toast.makeText(this, "◉ Bubble live — look for the dot!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
    }

    override fun onBinderReceived() {
        mainHandler.post { updateShizukuStatus() }
    }

    override fun onBinderDead() {
        mainHandler.post { updateShizukuStatus() }
    }

    private fun updateShizukuStatus() {
        val ping = ShizukuBooster.isShizukuAvailable()
        if (!ping) {
            // Binder not delivered yet (server sends it asynchronously after
            // process start). The sticky listener above will refresh this
            // as soon as it arrives.
            binding.tvShizukuStatus.text = "◌ SHIZUKU: LINK PENDING..."
            binding.tvShizukuStatus.setTextColor(getColor(R.color.accent_amber))
            binding.btnGrantPermission.visibility = View.GONE
            return
        }

        val hasPerm = ShizukuBooster.hasShizukuPermission()
        if (hasPerm) {
            binding.tvShizukuStatus.text = "● SHIZUKU: ONLINE // AUTHORIZED"
            binding.tvShizukuStatus.setTextColor(getColor(R.color.accent_mint))
            binding.btnGrantPermission.visibility = View.GONE
        } else {
            binding.tvShizukuStatus.text = "◌ SHIZUKU: AUTH REQUIRED"
            binding.tvShizukuStatus.setTextColor(getColor(R.color.accent_amber))
            binding.btnGrantPermission.visibility = View.VISIBLE
        }
    }

    private fun checkOrRequestPermission(): Boolean {
        if (!ShizukuBooster.isShizukuAvailable()) {
            Toast.makeText(this, "Shizuku not connected yet - make sure Shizuku server is running, then reopen this app", Toast.LENGTH_LONG).show()
            return false
        }
        if (ShizukuBooster.hasShizukuPermission()) {
            return true
        }
        requestShizuku()
        return false
    }

    private fun requestShizuku() {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "Please grant Shizuku permission to apply ADB tweaks", Toast.LENGTH_SHORT).show()
        }
        Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Permission Granted!", Toast.LENGTH_SHORT).show()
                updateShizukuStatus()
            } else {
                Toast.makeText(this, "Permission Denied by user", Toast.LENGTH_SHORT).show()
                updateShizukuStatus()
            }
        }
    }

    private fun appendLog(msg: String) {
        mainHandler.post {
            binding.tvLogs.append(msg + "\n")
            binding.scrollViewMain.post {
                binding.scrollViewMain.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun executeBoost() {
        binding.btnBoost.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        val label = GameList.labelFor(this, Prefs.targetGame(this))
        Toast.makeText(this, "⚡ Boosting $label...", Toast.LENGTH_SHORT).show()
        appendLog("======================================")
        appendLog("[*] INITIATING AGGRESSIVE BOOST...")

        Thread {
            ShizukuBooster.boostDevice(this@MainActivity, logCallback = { log ->
                appendLog(log)
            })
            mainHandler.post {
                binding.btnBoost.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Game Boosted & Launched!", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun executeRestore() {
        binding.tileRestore.isEnabled = false
        appendLog("======================================")
        appendLog("[*] RESTORING DEFAULT SETTINGS...")

        Thread {
            ShizukuBooster.restoreStockSettings { log ->
                appendLog(log)
            }
            mainHandler.post {
                binding.tileRestore.isEnabled = true
                Toast.makeText(this@MainActivity, "Stock Settings Restored!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}
