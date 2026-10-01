#!/system/bin/sh
# ============================================================
#  CHILL MODE v1.0 — battery-friendly daily profile
#  Run via Shizuku (ADB-level shell). Safe & reversible.
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] Entering CHILL MODE..."

# 1. Battery saver ON
settings put global low_power 1 2>/dev/null
echo "[+] Battery saver: ON"

# 2. Gentle animations (0.5x — smooth but snappy)
for key in window_animation_scale transition_animation_scale animator_duration_scale; do
  settings put global "$key" 0.5 2>/dev/null
done
echo "[+] Animations: 0.5x"

# 3. DND off (normal life)
settings put global zen_mode 0 2>/dev/null
echo "[+] DND: OFF"

# 4. WiFi background scan back on
settings put global wifi_scan_always_enabled 1 2>/dev/null

# 5. Battery status
echo "[*] Battery:"
dumpsys battery 2>/dev/null | grep -E "level|temperature" | head -4

echo "[✓] CHILL MODE active. Battery will thank you."
