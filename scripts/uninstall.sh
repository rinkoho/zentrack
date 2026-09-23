#!/usr/bin/env bash
# Desinstalador de ZenTrack Ultra-Low Latency

# Colores para salida
COLOR_RED='\033[0;31m'
COLOR_GREEN='\033[0;32m'
COLOR_YELLOW='\033[1;33m'
COLOR_RESET='\033[0m'

log_info() { echo -e "[-] $1"; }
log_success() { echo -e "${COLOR_GREEN}[✓] $1${COLOR_RESET}"; }
log_warn() { echo -e "${COLOR_YELLOW}[⚠️] $1${COLOR_RESET}"; }
log_error() { echo -e "${COLOR_RED}[✗] $1${COLOR_RESET}"; }

echo -e "${COLOR_RED}========================================================${COLOR_RESET}"
echo -e "${COLOR_RED}           ZENTRACK SYSTEM UNINSTALLER                  ${COLOR_RESET}"
echo -e "${COLOR_RED}========================================================${COLOR_RESET}"
echo ""

# 1. Stop background services
log_info "Deteniendo servicios en segundo plano..."
systemctl --user disable --now zentrack 2>/dev/null || true
pkill -f zentrack-server 2>/dev/null || true
log_success "Servicios detenidos."

# 2. Ask for sudo up front
log_info "Se requieren permisos de administrador para eliminar binarios y reglas udev."
sudo -v

# 3. Remove Systemd Service
log_info "Eliminando servicio systemd..."
rm -f "$HOME/.config/systemd/user/zentrack.service"
systemctl --user daemon-reload
log_success "Servicio systemd eliminado."

# 4. Remove binaries
log_info "Eliminando binarios principales..."
sudo rm -f /usr/local/bin/zentrack
sudo rm -f /usr/local/bin/zentrack-server
sudo rm -f /usr/local/bin/zentrack-tray # Legacy
log_success "Binarios eliminados de /usr/local/bin."

# 5. Remove udev rules
log_info "Eliminando reglas udev..."
sudo rm -f /etc/udev/rules.d/99-zentrack.rules
sudo udevadm control --reload-rules && sudo udevadm trigger
log_success "Reglas udev de /dev/uinput eliminadas."

# 6. Remove Desktop entries and icons
log_info "Limpiando accesos directos e iconos del sistema..."
sudo rm -f /usr/share/applications/zentrack.desktop
rm -f "$HOME/.config/autostart/zentrack.desktop"

sudo rm -f /usr/share/icons/hicolor/scalable/apps/zentrack.svg
sudo rm -f /usr/share/icons/hicolor/48x48/apps/zentrack.png
sudo rm -f /usr/share/icons/hicolor/128x128/apps/zentrack.png
sudo rm -f /usr/share/icons/hicolor/256x256/apps/zentrack.png
sudo rm -f /usr/share/pixmaps/zentrack.png
sudo update-desktop-database /usr/share/applications 2>/dev/null || true
log_success "Accesos directos e iconos limpios."

echo ""
log_success "¡ZenTrack ha sido desinstalado completamente de tu sistema!"
echo -e "Nota: La carpeta del proyecto en $PWD no ha sido borrada."
echo -e "Si deseas eliminarla, simplemente borra este directorio manualmente."
