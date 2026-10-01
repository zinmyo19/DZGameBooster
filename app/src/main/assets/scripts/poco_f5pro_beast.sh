#!/system/bin/sh
# ============================================================
#  POCO F5 PRO BEAST v1.0 — near-root tuning WITHOUT root
#  Run via Shizuku (ADB-level shell). Aggressive but reversible.
#  100% LEGIT: stock Android/MIUI settings only. No overclock,
#  no thermal-engine hacks, no game files touched.
#  Undo everything: run restore_stock.sh
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] POCO F5 Pro BEAST MODE engaged..."

# 1. Lock 120Hz — buttery animations & gameplay
settings put system peak_refresh_rate 120.0 2>/dev/null
echo "[+] Refresh rate: 120Hz locked"

# 2. Force updated Game Driver path on all apps (GPU)
settings put global game_driver_all_apps 1 2>/dev/null
echo "[+] Game Driver: all apps"

# 3. Stop the system throttling the game to "save battery"
settings put global adaptive_battery_management_enabled 0 2>/dev/null
echo "[+] Adaptive battery management: OFF"

# 4. Doze OFF — no background CPU throttling mid-match
dumpsys deviceidle disable 2>/dev/null
echo "[+] Doze: DISABLED"

# 5. Keep eFootball out of Doze regardless
cmd deviceidle whitelist +jp.konami.pesam 2>/dev/null
echo "[+] eFootball whitelisted from Doze"

# 6. Freeze MIUI bloat that wakes the CPU (safe list — re-enable anytime)
for pkg in com.miui.analytics com.miui.msa.global com.xiaomi.mipicks; do
  if pm disable-user --user 0 "$pkg" >/dev/null 2>&1; then
    echo "[+] Froze bloat: $pkg"
  fi
done

# 7. RAM purge — cached apps out, game untouched
am kill-all 2>/dev/null
for pkg in com.facebook.katana com.facebook.orca com.instagram.android com.zhiliaoapp.musically com.android.chrome; do
  am kill "$pkg" >/dev/null 2>&1
done
echo "[+] Memory purged"

echo "[✓] BEAST MODE active. Go win."
echo "[i] Battery will drain faster at 120Hz + Doze off — run restore_stock.sh after playing."
