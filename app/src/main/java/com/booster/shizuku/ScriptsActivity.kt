package com.booster.shizuku

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ScriptsActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var adapter: ScriptAdapter
    private val scripts = mutableListOf<ScriptEntry>()

    private val pickScript = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                // Persist access across reboots — no storage permission needed (SAF)
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            val name = queryDisplayName(uri) ?: "script.sh"
            ScriptStore.add(this, name, uri)
            refreshList()
            Toast.makeText(this, "◣ Imported: $name ◢", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scripts)

        val rv = findViewById<RecyclerView>(R.id.rvScripts)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ScriptAdapter()
        rv.adapter = adapter

        findViewById<Button>(R.id.btnImportScript).setOnClickListener {
            // System file picker — user selects .sh files, no storage perm needed
            pickScript.launch(arrayOf("application/x-sh", "text/x-shellscript", "text/plain", "*/*"))
        }

        refreshList()
    }

    private fun refreshList() {
        scripts.clear()
        scripts.addAll(ScriptStore.getAll(this))
        adapter.notifyDataSetChanged()
        findViewById<TextView>(R.id.tvScriptsEmpty).visibility =
            if (scripts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex("_display_name")
                    if (idx >= 0) c.getString(idx) else null
                } else null
            }
        } catch (_: Exception) { null }
    }

    private fun runScript(entry: ScriptEntry, statusView: TextView) {
        if (!ShizukuBooster.isShizukuAvailable() || !ShizukuBooster.hasShizukuPermission()) {
            Toast.makeText(this, "Shizuku not authorized — grant it on the main screen first", Toast.LENGTH_LONG).show()
            return
        }
        val content = ScriptStore.readContent(this, entry)
        if (content.isNullOrBlank()) {
            Toast.makeText(this, "Cannot read script (file missing?) — bundled scripts re-seed on app update", Toast.LENGTH_LONG).show()
            return
        }

        // Live log dialog
        val logView = TextView(this).apply {
            setTextColor(getColor(R.color.accent_mint))
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(16, 16, 16, 16)
            text = "> Executing ${entry.name}...\n"
        }
        val scroll = ScrollView(this).apply { addView(logView) }
        val dialog = AlertDialog.Builder(this)
            .setTitle("◈ ${entry.name}")
            .setView(scroll)
            .setPositiveButton("CLOSE", null)
            .create()
        dialog.show()

        statusView.text = "◉ running..."
        statusView.setTextColor(getColor(R.color.accent_amber))

        Thread {
            ShizukuBooster.runScript(content) { log ->
                mainHandler.post {
                    logView.append(log + "\n")
                    scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
                }
            }
            mainHandler.post {
                statusView.text = "○ ready"
                statusView.setTextColor(getColor(R.color.text_dim))
            }
        }.start()
    }

    inner class ScriptAdapter : RecyclerView.Adapter<ScriptAdapter.Holder>() {
        inner class Holder(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvScriptName)
            val status: TextView = v.findViewById(R.id.tvScriptStatus)
            val btnRun: Button = v.findViewById(R.id.btnRunScript)
            val btnDel: Button = v.findViewById(R.id.btnDeleteScript)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_script, parent, false)
            return Holder(v)
        }

        override fun getItemCount(): Int = scripts.size

        override fun onBindViewHolder(h: Holder, pos: Int) {
            val entry = scripts[pos]
            h.name.text = entry.name
            h.status.text = "○ ready"
            h.btnRun.setOnClickListener { runScript(entry, h.status) }
            h.btnDel.setOnClickListener {
                ScriptStore.remove(this@ScriptsActivity, entry)
                refreshList()
                Toast.makeText(this@ScriptsActivity, "Removed: ${entry.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
