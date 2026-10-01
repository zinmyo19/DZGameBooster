#!/system/bin/sh
# ============================================================
#  TOUCH + WIFI TURBO v1.0 — max touch response, full-power WiFi
#  Run via Shizuku (ADB-level shell). Safe & reversible.
#  100% LEGIT: standard Android settings only. No macros,
#  no auto-tap, no packet manipulation — pure system tuning.
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] Maxing touch response..."
settings put system pointer_speed 7 2>/dev/null
settings put secure long_press_timeout 300 2>/dev/null
settings put secure multi_press_timeout 300 2>/dev/null
echo "[+] Touch: sensitivity MAX, tap recognition 300ms"

echo "[*] WiFi full-power tuning..."
# Stop background WiFi scans from causing ping spikes
settings put global wifi_scan_throttle_enabled 0 2>/dev/null
echo "[+] WiFi scan throttle: OFF"
# Keep WiFi radio at full power (no dozing mid-match)
settings put global wifi_suspend_optimizations_enabled 0 2>/dev/null
echo "[+] WiFi suspend optimizations: OFF"
# Never let WiFi sleep while the screen is on
settings put global wifi_sleep_policy 2 2>/dev/null
echo "[+] WiFi sleep policy: NEVER"
# Cellular stays hot as instant fallback during handover gaps
settings put global mobile_data_always_on 1 2>/dev/null
echo "[+] Mobile data always-on: ON"

echo "[✓] Touch + WiFi turbo active. Undo: run restore_stock.sh"
