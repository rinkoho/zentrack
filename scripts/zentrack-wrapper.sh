#!/usr/bin/env bash
case "$1" in
    tray)
        exec /usr/bin/zentrack-server --tray "$@"
        ;;
    tui)
        exec /usr/bin/zentrack-server --tui "$@"
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
        if [ "$#" -eq 0 ]; then
            if curl -s "http://127.0.0.1:3000/status" &>/dev/null; then
                echo "[ZenTrack] Servidor ya activo. Abriendo Centro de Conexión..."
                xdg-open "http://127.0.0.1:3000/pair" 2>/dev/null || true
            else
                exec /usr/bin/zentrack-server
            fi
        else
            exec /usr/bin/zentrack-server "$@"
        fi
        ;;
esac
