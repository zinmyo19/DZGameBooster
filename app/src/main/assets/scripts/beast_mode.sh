#!/system/bin/sh
# ============================================================
#  BEAST MODE v1.0 — aggressive gaming profile
#  Run via Shizuku (ADB-level shell). Safe & reversible.
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] BEAST MODE engaged..."

# 1. Terminate heavy background apps
for pkg in com.facebook.katana com.facebook.orca com.instagram.android com.zhiliaoapp.musically com.android.chrome com.google.android.youtube; do
  am force-stop "$pkg" 2>/dev/null && echo "[+] Killed $pkg"
done

# 2. Zero-out all animations for max responsiveness
for key in window_animation_scale transition_animation_scale animator_duration_scale; do
  settings put global "$key" 0.0 2>/dev/null
done
echo "[+] Animations: 0.0x"

# 3. Do Not Disturb
settings put global zen_mode 1 2>/dev/null
echo "[+] DND: ON"

# 4. Keep eFootball out of Doze (no background throttling)
cmd deviceidle whitelist +jp.konami.pesam 2>/dev/null && echo "[+] eFootball whitelisted from Doze"

# 5. Reduce background wakeups
settings put global wifi_scan_always_enabled 0 2>/dev/null
echo "[+] WiFi background scan: OFF"

# 6. Show current free RAM
echo "[*] Memory status:"
dumpsys meminfo 2>/dev/null | grep -i "free ram" | head -2

echo "[✓] BEAST MODE active. Go win."
