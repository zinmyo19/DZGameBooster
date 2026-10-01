package com.booster.shizuku

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            findViewById<TextView>(R.id.tvVersion).text = "v${info.versionName}"
        } catch (_: Exception) { }

        openOnTap(R.id.tvContactTelegram, "https://t.me/Dominic_aiBot")
        openOnTap(R.id.tvContactGithub, "https://github.com/zinmyo19")
        openOnTap(R.id.tvContactWeb, "https://dzinlabs-site.zynelabs.workers.dev/")
    }

    private fun openOnTap(viewId: Int, url: String) {
        findViewById<TextView>(viewId).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) { }
        }
    }
}
