#!/system/bin/sh
# ============================================================
#  RESTORE STOCK v2.0 — undo everything back to Android defaults
#  Run via Shizuku (ADB-level shell).
#  Covers: beast_mode, network_turbo, cloudflare_dns,
#          touch_wifi_turbo, poco_f5pro_beast + in-app boost
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] Restoring stock settings..."

# Animations back to 1.0x
for key in window_animation_scale transition_animation_scale animator_duration_scale; do
  settings put global "$key" 1.0 2>/dev/null
done
echo "[+] Animations: 1.0x"

# DND off
settings put global zen_mode 0 2>/dev/null
echo "[+] DND: OFF"

# Battery saver off
settings put global low_power 0 2>/dev/null
echo "[+] Battery saver: OFF"

# --- Touch back to stock ---
settings put system pointer_speed 0 2>/dev/null
settings put secure long_press_timeout 500 2>/dev/null
settings put secure multi_press_timeout 400 2>/dev/null
echo "[+] Touch: stock"

# --- Network back to stock ---
settings put global wifi_scan_throttle_enabled 1 2>/dev/null
settings put global wifi_suspend_optimizations_enabled 1 2>/dev/null
settings put global wifi_sleep_policy 0 2>/dev/null
settings put global mobile_data_always_on 0 2>/dev/null
settings put global wifi_scan_always_enabled 1 2>/dev/null
echo "[+] WiFi/network: stock"

# --- Private DNS back to automatic (undoes cloudflare_dns.sh) ---
settings put global private_dns_mode opportunistic 2>/dev/null
settings delete global private_dns_specifier 2>/dev/null
echo "[+] Private DNS: automatic"

# --- POCO beast extras back to stock ---
settings delete system peak_refresh_rate 2>/dev/null
settings put global game_driver_all_apps 0 2>/dev/null
settings put global adaptive_battery_management_enabled 1 2>/dev/null
dumpsys deviceidle enable 2>/dev/null
echo "[+] 120Hz/Doze/battery: stock"

# --- Unfreeze MIUI bloat frozen by poco_f5pro_beast.sh ---
for pkg in com.miui.analytics com.miui.msa.global com.xiaomi.mipicks; do
  if pm enable --user 0 "$pkg" >/dev/null 2>&1; then
    echo "[+] Unfroze: $pkg"
  fi
done

# Remove Doze whitelist
cmd deviceidle whitelist -jp.konami.pesam 2>/dev/null
echo "[+] Doze whitelist cleared"

echo "[✓] All stock. Nothing changed permanently."
