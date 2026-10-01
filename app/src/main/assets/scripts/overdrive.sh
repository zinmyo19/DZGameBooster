#!/system/bin/sh
# ============================================================
#  OVERDRIVE v1.0 — the full arsenal in ONE tap.
#  For when you're losing and need MAXIMUM everything. 😤
#  Runs: animations off, DND, touch max, WiFi full-power,
#        Cloudflare DNS, POCO beast tuning, RAM nuke.
#  100% LEGIT: stock Android settings only. No macros,
#  no packet tricks, no game files touched.
#  Undo everything: run restore_stock.sh
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] OVERDRIVE ENGAGED..."

# 1. Animations off — zero UI lag
for key in window_animation_scale transition_animation_scale animator_duration_scale; do
  settings put global "$key" 0.0 2>/dev/null
done
echo "[+] Animations: 0.0x"

# 2. Do Not Disturb — nothing interrupts the comeback
settings put global zen_mode 1 2>/dev/null
echo "[+] DND: ON"

# 3. Touch maxed
settings put system pointer_speed 7 2>/dev/null
settings put secure long_press_timeout 300 2>/dev/null
settings put secure multi_press_timeout 300 2>/dev/null
echo "[+] Touch: MAX"

# 4. Cloudflare DNS (DoT)
settings put global private_dns_mode hostname 2>/dev/null
settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com 2>/dev/null
echo "[+] DNS: Cloudflare"

# 5. WiFi full power
settings put global wifi_scan_throttle_enabled 0 2>/dev/null
settings put global wifi_suspend_optimizations_enabled 0 2>/dev/null
settings put global wifi_sleep_policy 2 2>/dev/null
settings put global mobile_data_always_on 1 2>/dev/null
echo "[+] WiFi: FULL POWER"

# 6. POCO F5 Pro beast
settings put system peak_refresh_rate 120.0 2>/dev/null
settings put global game_driver_all_apps 1 2>/dev/null
settings put global adaptive_battery_management_enabled 0 2>/dev/null
dumpsys deviceidle disable 2>/dev/null
cmd deviceidle whitelist +jp.konami.pesam 2>/dev/null
echo "[+] F5P beast: ON"

# 7. RAM nuke — everything cached dies, game untouched
am kill-all 2>/dev/null
for pkg in com.facebook.katana com.facebook.orca com.instagram.android com.zhiliaoapp.musically com.android.chrome com.google.android.youtube; do
  am force-stop "$pkg" >/dev/null 2>&1
done
echo "[+] RAM: NUKED"

echo "[✓] OVERDRIVE ACTIVE. Now go turn this match around."
echo "[i] Battery drains fast in OVERDRIVE — run restore_stock.sh after the match."
