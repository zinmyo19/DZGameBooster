package com.booster.shizuku

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Game detector screen: lists installed games found in the known-games
 * database. Tap a game to set it as the boost target.
 */
class GamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_games)

        val tvHeader = findViewById<TextView>(R.id.tvGamesHeader)
        val listView = findViewById<ListView>(R.id.lvGames)
        val tvEmpty = findViewById<TextView>(R.id.tvGamesEmpty)

        tvHeader.text = "// SCANNING INSTALLED APPS..."

        Thread {
            val games = GameList.detectInstalled(this)
            runOnUiThread {
                if (games.isEmpty()) {
                    tvHeader.text = "// NO KNOWN GAMES FOUND"
                    tvEmpty.visibility = View.VISIBLE
                    listView.visibility = View.GONE
                } else {
                    tvHeader.text = "// ${games.size} GAME(S) DETECTED — TAP TO SET TARGET"
                    val adapter = object : ArrayAdapter<GameApp>(
                        this,
                        android.R.layout.simple_list_item_2,
                        android.R.id.text1,
                        games
                    ) {
                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {
                            val v = super.getView(position, convertView, parent)
                            val g = getItem(position)!!
                            val tv1 = v.findViewById<TextView>(android.R.id.text1)
                            val tv2 = v.findViewById<TextView>(android.R.id.text2)
                            tv1.text = "🎮 ${g.label}"
                            tv1.setTextColor(
                                ContextCompat.getColor(
                                    this@GamesActivity,
                                    R.color.text_primary
                                )
                            )
                            tv1.textSize = 15f
                            tv2.text = g.pkg
                            tv2.setTextColor(
                                ContextCompat.getColor(
                                    this@GamesActivity,
                                    R.color.text_dim
                                )
                            )
                            tv2.textSize = 11f
                            return v
                        }
                    }
                    listView.adapter = adapter
                    listView.onItemClickListener =
                        AdapterView.OnItemClickListener { _, _, pos, _ ->
                            val g = games[pos]
                            Prefs.setTargetGame(this, g.pkg)
                            Toast.makeText(
                                this,
                                "🎯 Target set: ${g.label}",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                }
            }
        }.start()
    }
}
