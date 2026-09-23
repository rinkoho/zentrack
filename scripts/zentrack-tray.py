#!/usr/bin/env python3
# ==============================================================================
# 🚀 ZenTrack - Bandeja del Sistema / System Tray & Dock Indicator (Linux)
# Compatible con: Polybar, GNOME Shell, KDE Plasma, XFCE, i3, bspwm, etc.
# ==============================================================================

import os
import sys
import json
import time
import subprocess
import urllib.request
import urllib.error
import threading

import gi
gi.require_version('Gtk', '3.0')
try:
    gi.require_version('AyatanaAppIndicator3', '0.1')
    from gi.repository import AyatanaAppIndicator3 as AppIndicator
except (ValueError, ImportError):
    gi.require_version('AppIndicator3', '0.1')
    from gi.repository import AppIndicator3 as AppIndicator

from gi.repository import Gtk, GLib

APP_ID = "zentrack-tray"
SERVER_URL = "http://127.0.0.1:3000"
PROJECT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

# Resolve icon path
DEFAULT_ICON = os.path.join(PROJECT_DIR, "public", "icon-32.png")
if not os.path.exists(DEFAULT_ICON):
    DEFAULT_ICON = os.path.join(PROJECT_DIR, "public", "icon.png")

class ZenTrackTray:
    def __init__(self):
        self.server_process = None
        self.last_device_state = None
        self.last_connected_count = 0
        self.is_server_alive = False

        # Create AppIndicator
        self.indicator = AppIndicator.Indicator.new(
            APP_ID,
            DEFAULT_ICON,
            AppIndicator.IndicatorCategory.APPLICATION_STATUS
        )
        self.indicator.set_status(AppIndicator.IndicatorStatus.ACTIVE)
        self.indicator.set_title("ZenTrack")

        # Build Menu
        self.menu = Gtk.Menu()

        self.item_title = Gtk.MenuItem(label="⚡ ZenTrack Ultra-Low Latency")
        self.item_title.set_sensitive(False)
        self.menu.append(self.item_title)

        self.item_status = Gtk.MenuItem(label="⏳ Verificando servidor...")
        self.item_status.set_sensitive(False)
        self.menu.append(self.item_status)

        self.item_device = Gtk.MenuItem(label="📱 Buscando dispositivos...")
        self.item_device.set_sensitive(False)
        self.menu.append(self.item_device)

        self.menu.append(Gtk.SeparatorMenuItem())

        self.item_open_gui = Gtk.MenuItem(label="🚀 Abrir Centro de Conexión (GUI)")
        self.item_open_gui.connect("activate", self.on_open_gui)
        self.menu.append(self.item_open_gui)

        self.item_usb_reverse = Gtk.MenuItem(label="⚡ Activar Túnel USB ADB")
        self.item_usb_reverse.connect("activate", self.on_usb_reverse)
        self.menu.append(self.item_usb_reverse)

        self.item_copy_url = Gtk.MenuItem(label="📋 Copiar URL de Emparejamiento")
        self.item_copy_url.connect("activate", self.on_copy_url)
        self.menu.append(self.item_copy_url)

        self.menu.append(Gtk.SeparatorMenuItem())

        self.item_restart = Gtk.MenuItem(label="🔄 Reiniciar Servidor")
        self.item_restart.connect("activate", self.on_restart_server)
        self.menu.append(self.item_restart)

        self.item_quit = Gtk.MenuItem(label="❌ Salir de ZenTrack")
        self.item_quit.connect("activate", self.on_quit)
        self.menu.append(self.item_quit)

        self.menu.show_all()
        self.indicator.set_menu(self.menu)

        # Check / ensure server is running
        self.ensure_server_running()

        # Poller every 2.5 seconds
        GLib.timeout_add_seconds(2, self.poll_server)

    def notify(self, title, message):
        try:
            subprocess.Popen(["notify-send", "-a", "ZenTrack", "-i", DEFAULT_ICON, title, message])
        except Exception:
            pass

    def ensure_server_running(self):
        try:
            req = urllib.request.urlopen(f"{SERVER_URL}/status", timeout=1)
            if req.status == 200:
                self.is_server_alive = True
                return
        except Exception:
            pass

        # Server not responding on :3000, attempt to launch binary
        bin_candidates = [
            os.path.join(PROJECT_DIR, "server-rust", "target", "release", "zentrack-server"),
            "/usr/local/bin/zentrack-server"
        ]
        for b in bin_candidates:
            if os.path.exists(b) and os.access(b, os.X_OK):
                try:
                    self.server_process = subprocess.Popen([b], cwd=PROJECT_DIR)
                    self.notify("ZenTrack", "Servidor nativo iniciado en segundo plano (500Hz).")
                    break
                except Exception as e:
                    print(f"[Tray] Error starting server: {e}", file=sys.stderr)

    def poll_server(self):
        # Run network requests in background thread to avoid blocking GTK event loop
        threading.Thread(target=self._fetch_status_worker, daemon=True).start()
        return True

    def _fetch_status_worker(self):
        status_data = None
        health_data = None

        try:
            req = urllib.request.urlopen(f"{SERVER_URL}/status", timeout=1.5)
            status_data = json.loads(req.read().decode("utf-8"))
        except Exception:
            pass

        try:
            req_h = urllib.request.urlopen(f"{SERVER_URL}/api/system-health", timeout=1.5)
            health_data = json.loads(req_h.read().decode("utf-8"))
        except Exception:
            pass

        GLib.idle_add(self._update_ui_state, status_data, health_data)

    def _update_ui_state(self, status_data, health_data):
        if not status_data:
            self.is_server_alive = False
            self.item_status.set_label("🔴 Servidor Inactivo")
            self.item_device.set_label("⚠️ Sin conexión con zentrack-server")
            return

        self.is_server_alive = True
        connected_count = status_data.get("connectedCount", 0)

        if connected_count > 0:
            self.item_status.set_label(f"🟢 Activo • {connected_count} Cliente(s) Conectado(s)")
        else:
            self.item_status.set_label("🟡 Activo • Esperando Conexión (500Hz)")

        if connected_count != self.last_connected_count and connected_count > 0:
            self.notify("ZenTrack Conectado", f"¡Cliente conectado a ultra-baja latencia! (Activos: {connected_count})")
        self.last_connected_count = connected_count

        # Device status from health
        if health_data:
            devices = health_data.get("devices", [])
            auth_device = next((d for d in devices if d.get("is_authorized")), None)
            unauth_device = next((d for d in devices if d.get("state") == "unauthorized"), None)

            if auth_device:
                dev_label = f"⚡ USB: {auth_device.get('model')} (Autorizado)"
                self.item_device.set_label(dev_label)
                if self.last_device_state != auth_device.get("serial"):
                    self.notify("Dispositivo USB Detectado", f"{auth_device.get('model')} listo para operar a 500Hz.")
                    self.last_device_state = auth_device.get("serial")
            elif unauth_device:
                self.item_device.set_label(f"⚠️ USB: {unauth_device.get('model')} (Requiere Aceptar RSA)")
                self.last_device_state = "unauthorized"
            else:
                self.item_device.set_label("📱 Cable USB: Desconectado")
                self.last_device_state = "none"

    def on_open_gui(self, _):
        url = f"{SERVER_URL}/pair"
        try:
            subprocess.Popen(["xdg-open", url])
        except Exception:
            pass

    def on_usb_reverse(self, _):
        def _call_reverse():
            try:
                req = urllib.request.Request(f"{SERVER_URL}/api/usb/reverse", data=b"", method="POST")
                urllib.request.urlopen(req, timeout=2)
                self.notify("ZenTrack USB", "Túnel inverso ADB tcp:3000 activado.")
            except Exception as e:
                self.notify("ZenTrack USB", f"Error al activar túnel: {e}")

        threading.Thread(target=_call_reverse, daemon=True).start()

    def on_copy_url(self, _):
        clipboard = Gtk.Clipboard.get(gi.repository.Gdk.SELECTION_CLIPBOARD)
        clipboard.set_text(f"{SERVER_URL}/pair", -1)
        self.notify("ZenTrack", "URL copiada al portapapeles.")

    def on_restart_server(self, _):
        self.notify("ZenTrack", "Reiniciando servidor...")
        if self.server_process:
            try:
                self.server_process.terminate()
            except Exception:
                pass
            self.server_process = None
        self.ensure_server_running()

    def on_quit(self, _):
        if self.server_process:
            try:
                self.server_process.terminate()
            except Exception:
                pass
        Gtk.main_quit()

def main():
    app = ZenTrackTray()
    Gtk.main()

if __name__ == "__main__":
    main()
