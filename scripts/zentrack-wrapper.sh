#!/usr/bin/env bash
case "$1" in
    tray)
        exec /usr/bin/zentrack-server --tray "$@"
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
        pkexec systemctl start zentrack-system@$USER 2>/dev/null
        ;;
    stop)
        pkexec systemctl stop zentrack-system@$USER 2>/dev/null
        pkill -f zentrack-server || true
        ;;
    restart)
        pkexec systemctl restart zentrack-system@$USER 2>/dev/null
        ;;
    logs)
        journalctl -u zentrack-system@$USER -f
        ;;
    *)
        if [ "$#" -eq 0 ]; then
            # 1. Asegurar que el servidor está corriendo en segundo plano
            if ! systemctl is-active --quiet zentrack-system@$USER; then
                pkexec systemctl start zentrack-system@$USER 2>/dev/null || true
                sleep 0.5 # Esperar a que levante el puerto
            fi

            # 2. Asegurar que el icono del system tray está visible
            if ! pgrep -f "zentrack-server --tray" > /dev/null; then
                nohup /usr/bin/zentrack-server --tray >/dev/null 2>&1 &
            fi

            # 3. Mostrar el estado y el QR en la terminal, y devolver el prompt
            exec /usr/bin/zentrack-server --info
        else
            # 4. Si se pasan argumentos como -h, pasarlos directo sin levantar servicio
            exec /usr/bin/zentrack-server "$@"
        fi
        ;;
esac
