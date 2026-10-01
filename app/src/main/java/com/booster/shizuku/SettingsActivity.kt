package com.booster.shizuku

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val etTarget = findViewById<EditText>(R.id.etTargetGame)
        val swFb = findViewById<Switch>(R.id.swKillFacebook)
        val swMsg = findViewById<Switch>(R.id.swKillMessenger)
        val swIg = findViewById<Switch>(R.id.swKillInstagram)
        val swTt = findViewById<Switch>(R.id.swKillTiktok)
        val swChrome = findViewById<Switch>(R.id.swKillChrome)
        val swAnim = findViewById<Switch>(R.id.swDisableAnim)
        val swDnd = findViewById<Switch>(R.id.swDnd)
        val swTouch = findViewById<Switch>(R.id.swTouchBoost)
        val swNet = findViewById<Switch>(R.id.swNetStable)
        val swPoco = findViewById<Switch>(R.id.swPocoBeast)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        // Load current values
        etTarget.setText(Prefs.targetGame(this))
        swFb.isChecked = Prefs.getBoolean(this, Prefs.KEY_KILL_FB, true)
        swMsg.isChecked = Prefs.getBoolean(this, Prefs.KEY_KILL_MSG, true)
        swIg.isChecked = Prefs.getBoolean(this, Prefs.KEY_KILL_IG, true)
        swTt.isChecked = Prefs.getBoolean(this, Prefs.KEY_KILL_TT, true)
        swChrome.isChecked = Prefs.getBoolean(this, Prefs.KEY_KILL_CHROME, true)
        swAnim.isChecked = Prefs.disableAnim(this)
        swDnd.isChecked = Prefs.dndEnabled(this)
        swTouch.isChecked = Prefs.touchBoost(this)
        swNet.isChecked = Prefs.netStable(this)
        swPoco.isChecked = Prefs.pocoBeast(this)

        btnSave.setOnClickListener {
            Prefs.setTargetGame(this, etTarget.text.toString())
            Prefs.setBoolean(this, Prefs.KEY_KILL_FB, swFb.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_KILL_MSG, swMsg.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_KILL_IG, swIg.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_KILL_TT, swTt.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_KILL_CHROME, swChrome.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_DISABLE_ANIM, swAnim.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_DND, swDnd.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_TOUCH_BOOST, swTouch.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_NET_STABLE, swNet.isChecked)
            Prefs.setBoolean(this, Prefs.KEY_POCO_BEAST, swPoco.isChecked)
            Toast.makeText(this, "◣ Configuration saved ◢", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
