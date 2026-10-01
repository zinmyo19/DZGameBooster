package com.booster.shizuku

import android.content.Context
import android.content.pm.PackageManager

/** A game found on the device: display label + package name. */
data class GameApp(val label: String, val pkg: String)

/**
 * Game auto-detector.
 * Ships a known-games database (assets/gamelist.txt, one package per line),
 * intersects it with installed packages and returns the matches.
 * Replaces manual package-name typing in Settings.
 */
object GameList {
    private var cachedKnown: Set<String>? = null

    fun knownPackages(context: Context): Set<String> {
        cachedKnown?.let { return it }
        val set = HashSet<String>()
        try {
            context.assets.open("gamelist.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val pkg = line.trim()
                    if (pkg.isNotEmpty()) set.add(pkg)
                }
            }
        } catch (_: Exception) {
            // no database -> no detection
        }
        cachedKnown = set
        return set
    }

    /** Installed apps that appear in the known-games database, sorted by label. */
    fun detectInstalled(context: Context): List<GameApp> {
        val known = knownPackages(context)
        if (known.isEmpty()) return emptyList()
        val pm = context.packageManager
        val result = ArrayList<GameApp>()
        try {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in apps) {
                if (known.contains(app.packageName)) {
                    val label = try {
                        pm.getApplicationLabel(app).toString()
                    } catch (_: Exception) {
                        app.packageName
                    }
                    result.add(GameApp(label, app.packageName))
                }
            }
        } catch (_: Exception) {
            // fall through with what we have
        }
        return result.sortedBy { it.label.lowercase() }
    }

    /** Human-readable label for a package; falls back to the package name. */
    fun labelFor(context: Context, pkg: String): String {
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) {
            pkg
        }
    }
}
