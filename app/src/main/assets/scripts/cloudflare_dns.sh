#!/system/bin/sh
# ============================================================
#  CLOUDFLARE DNS v1.0 — Private DNS over TLS
#  Run via Shizuku (ADB-level shell). Safe & reversible.
#  100% LEGIT: this only changes Android's standard Private DNS
#  setting. No packet tricks, no macros, no game files touched —
#  a stable ping is NOT cheating.
#  Author: Dominic's Game Booster Lab
# ============================================================

echo "[*] Switching Private DNS to Cloudflare (DoT)..."

settings put global private_dns_mode hostname 2>/dev/null
settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com 2>/dev/null

echo "[+] Private DNS: 1dot1dot1dot1.cloudflare-dns.com"
echo "[*] Mode: $(settings get global private_dns_mode 2>/dev/null)"
echo "[*] Host: $(settings get global private_dns_specifier 2>/dev/null)"

# Quick resolution sanity check
echo "[*] Resolving test..."
nslookup google.com 2>/dev/null | head -4 || getprop net.dns1 2>/dev/null

echo "[✓] Cloudflare DNS active. Lower DNS latency = faster matchmaking, steadier ping."
echo "[i] Undo: run restore_stock.sh"
