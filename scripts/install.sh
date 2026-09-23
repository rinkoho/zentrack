#!/usr/bin/env bash
# ==============================================================================
# 🚀 ZenTrack - Instalador Inteligente de Dependencias y Servicio del Sistema
# Plataforma: GNU/Linux (Arch, Ubuntu, Debian, Fedora, openSUSE, etc.)
# ==============================================================================

set -e

COLOR_RESET="\033[0m"
COLOR_CYAN="\033[1;36m"
COLOR_GREEN="\033[1;32m"
COLOR_YELLOW="\033[1;33m"
COLOR_RED="\033[1;31m"
COLOR_GRAY="\033[0;90m"

log_info() {
    echo -e "${COLOR_CYAN}[ZenTrack]${COLOR_RESET} $1"
}

log_success() {
    echo -e "${COLOR_GREEN}[ZenTrack ✓]${COLOR_RESET} $1"
}

log_warn() {
    echo -e "${COLOR_YELLOW}[ZenTrack ⚠️]${COLOR_RESET} $1"
}

log_error() {
    echo -e "${COLOR_RED}[ZenTrack ❌]${COLOR_RESET} $1"
}

echo -e "${COLOR_CYAN}"
echo "========================================================"
echo "    ⚡ ZENTRACK ULTRA-LOW LATENCY SYSTEM INSTALLER ⚡   "
echo "========================================================"
echo -e "${COLOR_RESET}"

# 1. Detect OS
if [ "$(uname)" != "Linux" ]; then
    log_error "Este instalador está diseñado exclusivamente para entornos GNU/Linux."
    exit 1
fi

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CURRENT_USER="${SUDO_USER:-$USER}"

log_info "Directorio del proyecto: $PROJECT_DIR"
log_info "Usuario objetivo: $CURRENT_USER"

# 2. Detect Linux Distribution
DISTRO="generic"
if [ -f /etc/os-release ]; then
    . /etc/os-release
    DISTRO="$ID"
fi
log_info "Distribución detectada: $PRETTY_NAME ($DISTRO)"

# 3. Check / Install ADB dependencies
log_info "Verificando herramientas Android (ADB)..."
if command -v adb &>/dev/null; then
    log_success "ADB ya está instalado en el sistema ($(adb version | head -n 1))."
else
    log_warn "ADB no está instalado. Instalando según tu distribución..."
    case "$DISTRO" in
        arch|manjaro|endeavouros|garuda)
            sudo pacman -S --needed --noconfirm android-tools android-udev
            ;;
        ubuntu|debian|linuxmint|pop)
            sudo apt update && sudo apt install -y adb
            ;;
        fedora|rhel|centos)
            sudo dnf install -y android-tools
            ;;
        opensuse*|suse)
            sudo zypper install -y android-tools
            ;;
        *)
            log_warn "Distribución desconocida. Por favor instala manualmente 'android-tools' o 'adb'."
            ;;
    esac
fi

# 4. Configure /dev/uinput Permissions & udev rules
log_info "Configurando permisos de hardware para /dev/uinput..."

# Add user to input group
if id -nG "$CURRENT_USER" | grep -qw "input"; then
    log_success "El usuario $CURRENT_USER ya pertenece al grupo 'input'."
else
    log_info "Añadiendo a $CURRENT_USER al grupo 'input'..."
    sudo usermod -aG input "$CURRENT_USER"
    log_success "Usuario añadido al grupo 'input'."
fi

# Create udev rule
UDEV_RULE_FILE="/etc/udev/rules.d/99-zentrack.rules"
log_info "Creando regla udev permanente en $UDEV_RULE_FILE..."
echo 'KERNEL=="uinput", MODE="0660", GROUP="input", OPTIONS+="static_node=uinput"' | sudo tee "$UDEV_RULE_FILE" >/dev/null

sudo udevadm control --reload-rules && sudo udevadm trigger
log_success "Reglas udev recargadas correctamente."

# 5. Build and Install Server Binary
log_info "Compilando servidor nativo Rust en modo release..."
cd "$PROJECT_DIR/server-rust"
cargo build --release

BIN_SOURCE="$PROJECT_DIR/server-rust/target/release/zentrack-server"
BIN_DEST="/usr/local/bin/zentrack-server"

log_info "Instalando binario en $BIN_DEST..."
sudo cp "$BIN_SOURCE" "$BIN_DEST"
sudo chmod +x "$BIN_DEST"
log_success "Binario instalado en $BIN_DEST."

# 5.1 Install Tray Companion (Now Built-in Native Rust)
log_info "La bandeja del sistema (System Tray) ahora es nativa en Rust y está integrada en el servidor."

# 5.2 Install System & Desktop Icons
log_info "Instalando iconos del sistema en temas hicolor..."
sudo mkdir -p /usr/share/icons/hicolor/scalable/apps /usr/share/icons/hicolor/48x48/apps /usr/share/icons/hicolor/128x128/apps /usr/share/icons/hicolor/256x256/apps /usr/share/pixmaps 2>/dev/null || true
[ -f "$PROJECT_DIR/public/icon.svg" ] && sudo cp "$PROJECT_DIR/public/icon.svg" /usr/share/icons/hicolor/scalable/apps/zentrack.svg || true
[ -f "$PROJECT_DIR/public/icon.png" ] && sudo cp "$PROJECT_DIR/public/icon.png" /usr/share/icons/hicolor/48x48/apps/zentrack.png || true
[ -f "$PROJECT_DIR/public/icon-128.png" ] && sudo cp "$PROJECT_DIR/public/icon-128.png" /usr/share/icons/hicolor/128x128/apps/zentrack.png || true
[ -f "$PROJECT_DIR/public/icon-256.png" ] && sudo cp "$PROJECT_DIR/public/icon-256.png" /usr/share/icons/hicolor/256x256/apps/zentrack.png || true
[ -f "$PROJECT_DIR/public/icon.png" ] && sudo cp "$PROJECT_DIR/public/icon.png" /usr/share/pixmaps/zentrack.png || true
log_success "Iconos registrados en el sistema."

# 5.3 Install Desktop Application Entry (.desktop)
DESKTOP_SRC="$PROJECT_DIR/scripts/zentrack.desktop"
if [ -f "$DESKTOP_SRC" ]; then
    log_info "Instalando acceso directo en el menú de aplicaciones..."
    sudo cp "$DESKTOP_SRC" /usr/share/applications/zentrack.desktop
    sudo update-desktop-database /usr/share/applications 2>/dev/null || true

    # Autostart in system tray on login
    AUTOSTART_DIR="$HOME/.config/autostart"
    mkdir -p "$AUTOSTART_DIR"
    cp "$DESKTOP_SRC" "$AUTOSTART_DIR/zentrack.desktop"
    log_success "Inicio automático en bandeja de segundo plano configurado en $AUTOSTART_DIR/zentrack.desktop."
fi

# Create wrapper zentrack in /usr/local/bin
WRAPPER_DEST="/usr/local/bin/zentrack"
sudo tee "$WRAPPER_DEST" >/dev/null <<EOF
#!/usr/bin/env bash
case "\$1" in
    tray)
        exec /usr/local/bin/zentrack-server --tray "\$@"
        ;;
    tui)
        exec /usr/local/bin/zentrack-server --tui "\$@"
        ;;
    gui|pair)
        xdg-open "http://127.0.0.1:3000/pair" 2>/dev/null || sensible-browser "http://127.0.0.1:3000/pair"
        ;;
    status)
        curl -s "http://127.0.0.1:3000/status" || echo "Servidor no está en ejecución."
        ;;
    health)
        curl -s "http://127.0.0.1:3000/api/system-health" | jq . 2>/dev/null || curl -s "http://127.0.0.1:3000/api/system-health"
        ;;
    reverse)
        curl -s -X POST "http://127.0.0.1:3000/api/usb/reverse"
        ;;
    start)
        systemctl --user start zentrack
        ;;
    stop)
        systemctl --user stop zentrack
        pkill -f zentrack-server || true
        ;;
    restart)
        systemctl --user restart zentrack
        ;;
    logs)
        journalctl --user -u zentrack -f
        ;;
    *)
        if [ "\$#" -eq 0 ]; then
            # Sin argumentos: si el servidor ya está activo, abre la GUI o bandeja
            if curl -s "http://127.0.0.1:3000/status" &>/dev/null; then
                echo "[ZenTrack] Servidor ya activo en puerto 3000. Abriendo Centro de Conexión..."
                xdg-open "http://127.0.0.1:3000/pair" 2>/dev/null || true
            else
                exec /usr/local/bin/zentrack-server
            fi
        else
            exec /usr/local/bin/zentrack-server "\$@"
        fi
        ;;
esac
EOF
sudo chmod +x "$WRAPPER_DEST"
log_success "CLI del sistema 'zentrack' instalado en $WRAPPER_DEST."

# 6. Install systemd user service for background execution
SERVICE_DIR="$HOME/.config/systemd/user"
mkdir -p "$SERVICE_DIR"
SERVICE_FILE="$SERVICE_DIR/zentrack.service"

log_info "Configurando servicio de inicio en segundo plano en $SERVICE_FILE..."
cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=ZenTrack Ultra-Low Latency Input Server (500Hz)
After=network.target

[Service]
Type=simple
WorkingDirectory=$PROJECT_DIR
ExecStart=/usr/local/bin/zentrack-server
Restart=on-failure
RestartSec=3
Environment="RUST_LOG=info"

[Install]
WantedBy=default.target
EOF

systemctl --user daemon-reload
log_success "Servicio systemd creado."
log_info "Para iniciarlo ahora y activarlo en el arranque de sesión ejecuta:"
echo -e "${COLOR_GREEN}    systemctl --user enable --now zentrack${COLOR_RESET}"

# 7. Check Firewall (UFW)
if command -v ufw &>/dev/null && sudo ufw status 2>/dev/null | grep -qw "active"; then
    log_warn "El firewall UFW está activo. Para permitir Wi-Fi (3000/tcp) y Radar LAN (37020/udp):"
    echo -e "${COLOR_YELLOW}    sudo ufw allow 3000/tcp comment 'ZenTrack WebSocket Server'${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}    sudo ufw allow 37020/udp comment 'ZenTrack LAN Radar'${COLOR_RESET}"
    echo -e "${COLOR_YELLOW}    sudo ufw reload${COLOR_RESET}"
fi

echo ""
log_success "¡Instalación de ZenTrack completada con éxito!"
echo -e "${COLOR_CYAN}Comandos útiles disponibles:${COLOR_RESET}"
echo "  • zentrack           -> Iniciar servidor o abrir panel si ya está activo"
echo "  • zentrack tray      -> Iniciar icono en la bandeja del sistema (Dock/Tray)"
echo "  • zentrack start     -> Iniciar servicio en segundo plano (systemd)"
echo "  • zentrack gui       -> Abrir panel web de emparejamiento en el navegador"
echo "  • zentrack health    -> Ver diagnóstico de salud del sistema y dispositivos"
echo "  • zentrack logs      -> Ver registros en tiempo real"
echo ""
