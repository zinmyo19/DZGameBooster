#!/system/bin/sh
# ============================================================
#  NETWORK TURBO v1.0 — lower latency for online matches
#  Run via Shizuku (ADB-level shell). Safe & reversible.
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] Tuning network stack..."

# 1. Private DNS -> Cloudflare (fast, low-latency resolution; Google DNS lags here)
settings put global private_dns_mode hostname 2>/dev/null
settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com 2>/dev/null
echo "[+] Private DNS: 1dot1dot1dot1.cloudflare-dns.com"

# 2. Keep mobile data always on during WiFi handover gaps
settings put global mobile_data_always_on 1 2>/dev/null
echo "[+] Mobile data always-on: ON"

# 3. Aggressive WiFi roaming off (stable connection > fast roaming)
settings put global wifi_watchdog_on 0 2>/dev/null
echo "[+] WiFi watchdog: OFF"

# 4. Show current DNS / link status
echo "[*] Current DNS mode: $(settings get global private_dns_mode 2>/dev/null)"
echo "[*] Link info:"
dumpsys connectivity 2>/dev/null | grep -i "active network" | head -3

echo "[✓] Network tuned. Test your ping in-game."
