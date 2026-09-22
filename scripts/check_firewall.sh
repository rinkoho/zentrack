#!/usr/bin/env bash
# ==============================================================================
# 🛡️ ZenTrack - Diagnóstico y Configuración de Firewall (UFW / Firewalld)
# Puerto TCP 3000: Servidor WebSocket de Alta Velocidad (500Hz)
# Puerto UDP 37020: Radar LAN de Auto-Descubrimiento en red local
# ==============================================================================

set -e

COLOR_RESET="\033[0m"
COLOR_CYAN="\033[1;36m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[1;31m"
COLOR_BOLD="\033[1m"

echo -e "${COLOR_CYAN}========================================================${COLOR_RESET}"
echo -e "${COLOR_CYAN}    🛡️  DIAGNÓSTICO DE FIREWALL - ZENTRACK SERVER 500Hz  ${COLOR_RESET}"
echo -e "${COLOR_CYAN}========================================================${COLOR_RESET}\n"

# 1. Detect active firewall daemon
FIREWALL="none"
if systemctl is-active --quiet ufw 2>/dev/null; then
    FIREWALL="ufw"
elif systemctl is-active --quiet firewalld 2>/dev/null; then
    FIREWALL="firewalld"
elif systemctl is-active --quiet nftables 2>/dev/null; then
    FIREWALL="nftables"
elif systemctl is-active --quiet iptables 2>/dev/null; then
    FIREWALL="iptables"
fi

echo -e "${COLOR_BOLD}1. Servicio de Firewall Activo:${COLOR_RESET} $FIREWALL"

# 2. Inspect local IP and subnet
LOCAL_IP=$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{print $7}' | head -n 1)
SUBNET=$(ip -4 route | grep "proto kernel" | grep "src $LOCAL_IP" | awk '{print $1}' | head -n 1)

echo -e "${COLOR_BOLD}2. Red Local del Equipo:${COLOR_RESET} IP $LOCAL_IP (Subred: $SUBNET)\n"

# 3. Specific analysis for UFW
if [ "$FIREWALL" = "ufw" ]; then
    echo -e "${COLOR_YELLOW}[UFW Detectado Activo]${COLOR_RESET}"
    echo "Inspeccionando reglas actuales para puertos 3000 (TCP) y 37020 (UDP)..."
    
    # Check existing rules in /etc/ufw/user.rules without needing sudo if readable
    TCP_ALLOWED=false
    UDP_ALLOWED=false
    
    if grep -qE "dport 3000|--dport 3000" /etc/ufw/user.rules 2>/dev/null; then
        echo -e "  • Puerto 3000/TCP: ${COLOR_GREEN}Regla existente encontrada.${COLOR_RESET}"
        grep -E "3000" /etc/ufw/user.rules | grep -v "comment" || true
        TCP_ALLOWED=true
    else
        echo -e "  • Puerto 3000/TCP: ${COLOR_RED}NO encontrado en reglas UFW.${COLOR_RESET}"
    fi

    if grep -qE "dport 37020|--dport 37020" /etc/ufw/user.rules 2>/dev/null; then
        echo -e "  • Puerto 37020/UDP (Radar LAN): ${COLOR_GREEN}Regla existente encontrada.${COLOR_RESET}"
        UDP_ALLOWED=true
    else
        echo -e "  • Puerto 37020/UDP (Radar LAN): ${COLOR_RED}NO encontrado en reglas UFW.${COLOR_RESET}"
    fi

    echo ""
    if [ "$TCP_ALLOWED" = true ] && [ "$UDP_ALLOWED" = true ]; then
        echo -e "${COLOR_GREEN}✓ Los puertos de ZenTrack ya están configurados en UFW.${COLOR_RESET}"
    else
        echo -e "${COLOR_YELLOW}⚠️  Para permitir que tu celular se conecte por Wi-Fi y Radar LAN, ejecuta:${COLOR_RESET}\n"
        echo -e "  ${COLOR_CYAN}# Opción A (Recomendada: Solo tu red local $SUBNET):${COLOR_RESET}"
        echo "  sudo ufw allow proto tcp from ${SUBNET:-192.168.1.0/24} to any port 3000 comment 'ZenTrack Wi-Fi 500Hz'"
        echo "  sudo ufw allow proto udp from ${SUBNET:-192.168.1.0/24} to any port 37020 comment 'ZenTrack LAN Radar'"
        echo "  sudo ufw reload"
        echo ""
        echo -e "  ${COLOR_CYAN}# Opción B (Universal: Cualquier red Wi-Fi o punto de acceso móvil):${COLOR_RESET}"
        echo "  sudo ufw allow 3000/tcp comment 'ZenTrack WebSocket Server'"
        echo "  sudo ufw allow 37020/udp comment 'ZenTrack LAN Radar'"
        echo "  sudo ufw reload"
        echo ""
    fi

elif [ "$FIREWALL" = "firewalld" ]; then
    echo -e "${COLOR_YELLOW}[Firewalld Detectado Activo]${COLOR_RESET}"
    echo "Para habilitar los puertos en firewalld ejecuta:"
    echo "  sudo firewall-cmd --add-port=3000/tcp --permanent"
    echo "  sudo firewall-cmd --add-port=37020/udp --permanent"
    echo "  sudo firewall-cmd --reload"

elif [ "$FIREWALL" = "none" ]; then
    echo -e "${COLOR_GREEN}✓ No se detectaron firewalls restrictivos activos. El tráfico de red local está libre.${COLOR_RESET}"
fi

echo -e "\n${COLOR_CYAN}========================================================${COLOR_RESET}"
