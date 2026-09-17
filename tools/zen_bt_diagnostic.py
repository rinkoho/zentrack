#!/usr/bin/env python3
"""
ZenTrack Deep Bluetooth Radio & HCI Forensic Diagnostic Tool
Analyzes Android BTSnoop HCI logs & live radio metrics to identify
the exact root cause of mouse jitter, radio stalls, sniff mode freezes,
and keyboard repeat glitches.
"""

import sys
import os
import struct
import datetime
import subprocess
import argparse

BTSNOOP_EPOCH_DELTA = 0x00dcddb30f2f8000

class Color:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    CYAN = '\033[96m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BOLD = '\033[1m'
    UNDERLINE = '\033[4m'
    END = '\033[0m'

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout.strip(), result.stderr.strip(), result.returncode

def extract_phone_log(device_id, output_path):
    print(f"{Color.CYAN}[*] Extrayendo logs de radio HCI desde el celular ({device_id})...{Color.END}")
    cmd = f'adb -s {device_id} shell su -c "cat /data/misc/bluetooth/logs/btsnoop_hci.log.filtered" > "{output_path}"'
    _, err, code = run_cmd(cmd)
    if code != 0:
        print(f"{Color.RED}[!] Error al extraer el archivo btsnoop: {err}{Color.END}")
        return False
    if not os.path.exists(output_path) or os.path.getsize(output_path) < 16:
        print(f"{Color.RED}[!] Archivo btsnoop vacío o no encontrado.{Color.END}")
        return False
    size_kb = os.path.getsize(output_path) / 1024.0
    print(f"{Color.GREEN}[✓] Log extraído exitosamente: {size_kb:.1f} KB ({output_path}){Color.END}")
    return True

def parse_btsnoop(filename):
    with open(filename, 'rb') as f:
        header = f.read(16)
        if len(header) < 16 or not header.startswith(b'btsnoop\0'):
            return None
        
        records = []
        while True:
            rec_hdr = f.read(24)
            if len(rec_hdr) < 24:
                break
            orig_len, incl_len, flags, drops, ts_usec = struct.unpack('>IIIIQ', rec_hdr)
            data = f.read(incl_len)
            is_recv = (flags & 1) == 1
            ts_sec = (ts_usec - BTSNOOP_EPOCH_DELTA) / 1_000_000.0
            records.append((ts_sec, is_recv, flags, data))
    return records

def analyze_records(records, window_seconds=60):
    if not records:
        print(f"{Color.RED}[!] No hay registros para analizar.{Color.END}")
        return

    last_ts = records[-1][0]
    start_ts = last_ts - window_seconds
    window = [r for r in records if r[0] >= start_ts]

    print(f"\n{Color.BOLD}{Color.HEADER}======================================================================{Color.END}")
    print(f"{Color.BOLD}🔬 ZENTRACK: DIAGNÓSTICO FORENSE DE RADIO BLUETOOTH (ÚLTIMOS {window_seconds}s){Color.END}")
    print(f"{Color.BOLD}{Color.HEADER}======================================================================{Color.END}")
    print(f"Total paquetes analizados en ventana: {len(window)}")
    print(f"Inicio: {datetime.datetime.fromtimestamp(start_ts).strftime('%H:%M:%S')} | Fin: {datetime.datetime.fromtimestamp(last_ts).strftime('%H:%M:%S')}\n")

    mouse_packets = []
    key_packets = []
    mode_changes = []
    role_changes = []
    completed_acks = []

    for ts, is_recv, flags, data in window:
        if not data:
            continue
        pkt_type = data[0]
        if pkt_type == 2 and not is_recv: # Sent ACL data
            payload = data[9:]
            if len(payload) >= 7 and payload[0] == 0xa1:
                rep_id = payload[1]
                if rep_id == 2: # Mouse
                    btn, dx, dy, w, p = struct.unpack('b b b b b', payload[2:7])
                    mouse_packets.append((ts, btn, dx, dy, w, p))
                elif rep_id == 1 and len(payload) >= 10: # Keyboard
                    mod = payload[2]
                    keys = list(payload[4:10])
                    key_packets.append((ts, mod, keys))
        elif pkt_type == 4 and is_recv: # HCI Event
            event_code = data[1] if len(data) > 1 else 0
            if event_code == 0x13: # Num Completed Packets
                num_handles = data[3] if len(data) > 3 else 0
                for i in range(num_handles):
                    if 4 + i*4 + 4 <= len(data):
                        h, num = struct.unpack('<HH', data[4+i*4:8+i*4])
                        completed_acks.append((ts, h, num))
            elif event_code == 0x14: # Mode Change
                if len(data) >= 9:
                    st, h, mode, interval = struct.unpack('<BBHH', data[3:9])
                    mode_name = {0: "ACTIVE", 1: "HOLD", 2: "SNIFF"}.get(mode, str(mode))
                    mode_changes.append((ts, st, h, mode_name, interval * 0.625))
            elif event_code == 0x12: # Role Change
                if len(data) >= 11:
                    st, addr, role = data[3], data[4:10].hex(), data[10]
                    role_str = "Master" if role == 0 else "Slave"
                    role_changes.append((ts, st, addr, role_str))

    # 1. ANÁLISIS DE MOUSE (CADENCIA, TIRONES, RÁFAGAS)
    print(f"{Color.BOLD}1. 🖱️ MÉTRICAS DE TRANSMISIÓN DEL MOUSE:{Color.END}")
    moving_packets = [p for p in mouse_packets if p[2] != 0 or p[3] != 0 or p[1] != 0]
    idle_heartbeats = [p for p in mouse_packets if p[2] == 0 and p[3] == 0 and p[1] == 0]

    print(f"  • Total paquetes mouse emitidos: {len(mouse_packets)}")
    print(f"  • Paquetes con movimiento activo: {len(moving_packets)}")
    print(f"  • Heartbeats Anti-Sniff (dx=0, dy=0): {len(idle_heartbeats)}")

    stalls = []
    bursts = []
    prev_ts = None
    intervals = []

    for ts, btn, dx, dy, w, p in moving_packets:
        if prev_ts is not None:
            dt_ms = (ts - prev_ts) * 1000.0
            intervals.append(dt_ms)
            if dt_ms > 30.0:
                stalls.append((ts, dt_ms, dx, dy))
            elif dt_ms < 2.0:
                bursts.append((ts, dt_ms, dx, dy))
        prev_ts = ts

    if intervals:
        avg_interval = sum(intervals) / len(intervals)
        avg_hz = 1000.0 / avg_interval if avg_interval > 0 else 0
        print(f"  • Intervalo medio entre paquetes: {avg_interval:.2f} ms ({avg_hz:.1f} Hz)")
    else:
        print("  • No hubo movimiento activo en esta ventana.")

    # 2. EVALUACIÓN DE TIRONES / CONGELAMIENTOS
    print(f"\n{Color.BOLD}2. ⏱️ ANÁLISIS DE TIRONES (CONGELAMIENTOS > 30ms):{Color.END}")
    if not stalls:
        print(f"  {Color.GREEN}[✓] CERO TIRONES DETECTADOS en la ventana analizada. Transmisión 100% fluida.{Color.END}")
    else:
        print(f"  {Color.RED}[!] Se detectaron {len(stalls)} tirones/pausas durante el movimiento.{Color.END}")
        stalls_sorted = sorted(stalls, key=lambda s: s[1], reverse=True)[:5]
        print(f"  Top 5 tirones más severos:")
        for ts, dt, dx, dy in stalls_sorted:
            time_str = datetime.datetime.fromtimestamp(ts).strftime('%H:%M:%S.%f')[:-3]
            cause = "Pausa de Radio / Sniff Subrating" if dt > 200 else "Buffer Controller o Jitter de SO"
            print(f"    • {time_str} -> Pausa de {Color.RED}{dt:.1f} ms{Color.END} (dx={dx}, dy={dy}) | Causa probable: {cause}")

    # 3. EVALUACIÓN DE RÁFAGAS (CATCH-UP STORMS < 2ms)
    print(f"\n{Color.BOLD}3. ⚡ ANÁLISIS DE RÁFAGAS (CATCH-UP STORM < 2ms):{Color.END}")
    if not bursts:
        print(f"  {Color.GREEN}[✓] CERO RÁFAGAS DETECTADAS. El reloj monotónico respeta la cadencia mínima.{Color.END}")
    else:
        print(f"  {Color.YELLOW}[!] Se detectaron {len(bursts)} paquetes emitidos en ráfaga (<2ms entre sí).{Color.END}")
        print(f"      (Esto ocurre cuando el reloj intenta compensar ticks pasados a máxima velocidad).")

    # 4. EVENTOS DE RADIO (SNIFF MODE Y ROLE CHANGE)
    print(f"\n{Color.BOLD}4. 📡 EVENTOS DE ENLACE DE RADIO (HCI):{Color.END}")
    print(f"  • Cambios de Modo de Energía: {len(mode_changes)}")
    for ts, st, h, mode, interval_ms in mode_changes[-4:]:
        t_str = datetime.datetime.fromtimestamp(ts).strftime('%H:%M:%S')
        color = Color.YELLOW if mode == 'SNIFF' else Color.GREEN
        print(f"    - {t_str}: Handle 0x{h:03x} -> {color}Modo {mode}{Color.END} (Intervalo: {interval_ms:.1f} ms)")

    print(f"  • Intentos de Cambio de Rol (Role Changes): {len(role_changes)}")
    for ts, st, addr, role in role_changes[-4:]:
        t_str = datetime.datetime.fromtimestamp(ts).strftime('%H:%M:%S')
        print(f"    - {t_str}: Peer {addr} -> Rol {role} (Status: {st})")

    # 5. DIAGNÓSTICO DE TECLADO (DWELL TIME Y REPETICIONES)
    print(f"\n{Color.BOLD}5. ⌨️ DIAGNÓSTICO DE TECLADO:{Color.END}")
    if not key_packets:
        print("  • No hubo pulsaciones de teclado en esta ventana.")
    else:
        print(f"  • Total eventos de teclado transmitidos: {len(key_packets)}")
        # Check intervals between non-empty keys and all-zero (key up)
        key_presses = []
        active_down = None
        for ts, mod, keys in key_packets:
            has_keys = any(k != 0 for k in keys) or mod != 0
            if has_keys and active_down is None:
                active_down = (ts, mod, keys)
            elif not has_keys and active_down is not None:
                dwell_ms = (ts - active_down[0]) * 1000.0
                key_presses.append((active_down[0], dwell_ms, active_down[1], active_down[2]))
                active_down = None

        if key_presses:
            print(f"  • Teclas analizadas: {len(key_presses)}")
            long_presses = [kp for kp in key_presses if kp[1] > 250.0]
            if long_presses:
                print(f"  {Color.RED}[!] {len(long_presses)} pulsaciones superaron los 250ms (Riesgo de repetición automática en receptor):{Color.END}")
                for ts, dwell, mod, keys in long_presses:
                    t_str = datetime.datetime.fromtimestamp(ts).strftime('%H:%M:%S.%f')[:-3]
                    print(f"    • {t_str} -> Dwell: {Color.RED}{dwell:.1f} ms{Color.END} (Keys: {keys})")
            else:
                avg_dwell = sum(kp[1] for kp in key_presses) / len(key_presses)
                print(f"  {Color.GREEN}[✓] Todas las pulsaciones < 250ms (Promedio: {avg_dwell:.1f}ms). Cero repeticiones indeseadas.{Color.END}")

    print(f"\n{Color.BOLD}{Color.HEADER}======================================================================{Color.END}\n")

def main():
    parser = argparse.ArgumentParser(description="ZenTrack Bluetooth Radio Forensic Diagnostic Tool")
    parser.add_argument("--device", default="AAJBFIPJWSLJTKCI", help="ADB Device ID")
    parser.add_argument("--file", default="", help="Path to local btsnoop log file instead of pulling from device")
    parser.add_argument("--window", type=int, default=60, help="Analysis window in seconds (default: 60s)")
    args = parser.parse_args()

    log_path = args.file
    if not log_path:
        log_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "btsnoop_diag.log")
        if not extract_phone_log(args.device, log_path):
            sys.exit(1)

    records = parse_btsnoop(log_path)
    if not records:
        print(f"{Color.RED}[!] No se pudo parsear el archivo btsnoop.{Color.END}")
        sys.exit(1)

    analyze_records(records, window_seconds=args.window)

if __name__ == '__main__':
    main()
