#!/usr/bin/env python
import sys
import os

try:
    from evdev import UInput, AbsInfo, ecodes as e
except ImportError:
    print("ERROR: python-evdev is not installed. Please run 'sudo pacman -S python-evdev'", file=sys.stderr, flush=True)
    sys.exit(1)

# Register capabilities of Genuine Xbox 360 Controller (Standard evdev)
cap = {
    e.EV_KEY: [
        e.BTN_A, e.BTN_B, e.BTN_X, e.BTN_Y,
        e.BTN_TL, e.BTN_TR, e.BTN_SELECT, e.BTN_START, e.BTN_MODE,
        e.BTN_THUMBL, e.BTN_THUMBR
    ],
    e.EV_ABS: [
        (e.ABS_X, AbsInfo(value=0, min=-32768, max=32767, fuzz=16, flat=128, resolution=0)),
        (e.ABS_Y, AbsInfo(value=0, min=-32768, max=32767, fuzz=16, flat=128, resolution=0)),
        (e.ABS_RX, AbsInfo(value=0, min=-32768, max=32767, fuzz=16, flat=128, resolution=0)),
        (e.ABS_RY, AbsInfo(value=0, min=-32768, max=32767, fuzz=16, flat=128, resolution=0)),
        (e.ABS_Z, AbsInfo(value=0, min=0, max=255, fuzz=0, flat=0, resolution=0)),
        (e.ABS_RZ, AbsInfo(value=0, min=0, max=255, fuzz=0, flat=0, resolution=0)),
        (e.ABS_HAT0X, AbsInfo(value=0, min=-1, max=1, fuzz=0, flat=0, resolution=0)),
        (e.ABS_HAT0Y, AbsInfo(value=0, min=-1, max=1, fuzz=0, flat=0, resolution=0))
    ]
}

try:
    # Use Genuine Microsoft Xbox 360 USB Controller Hardware Identifiers (0x045e:0x028e)
    ui = UInput(
        events=cap,
        name="Microsoft X-Box 360 pad",
        vendor=0x045e,
        product=0x028e,
        version=0x0110,
        bustype=e.BUS_USB
    )
    print("SUCCESS: Virtual Xbox 360 Gamepad created successfully.", flush=True)
except Exception as err:
    print(f"ERROR: Could not create uinput device. {err}", file=sys.stderr, flush=True)
    print("Please ensure uinput module is loaded ('sudo modprobe uinput') and you have write permissions to /dev/uinput.", file=sys.stderr, flush=True)
    sys.exit(1)

# Axis state cache to prevent redundant writes
axis_states = {
    e.ABS_X: 0, e.ABS_Y: 0,
    e.ABS_RX: 0, e.ABS_RY: 0,
    e.ABS_Z: 0, e.ABS_RZ: 0,
    e.ABS_HAT0X: 0, e.ABS_HAT0Y: 0
}

def set_axis(axis, val):
    if axis in (e.ABS_X, e.ABS_Y, e.ABS_RX, e.ABS_RY):
        # Convert 0..255 (center 128) to standard signed 16-bit evdev range (-32768 to 32767)
        val = max(0, min(255, val))
        scaled_val = int(((val - 128) / 128.0) * 32767)
        scaled_val = max(-32768, min(32767, scaled_val))
    elif axis in (e.ABS_HAT0X, e.ABS_HAT0Y):
        # D-Pad HAT values: -1, 0, 1
        scaled_val = max(-1, min(1, val))
    else:
        # Trigger values: 0..255
        val = max(0, min(255, val))
        scaled_val = val

    if axis_states.get(axis) != scaled_val:
        axis_states[axis] = scaled_val
        ui.write(e.EV_ABS, axis, scaled_val)

def set_btn(btn, val):
    ui.write(e.EV_KEY, btn, 1 if val else 0)

# Map string button names to ecodes
BTN_MAP = {
    'A': e.BTN_A, 'B': e.BTN_B, 'X': e.BTN_X, 'Y': e.BTN_Y,
    'L': e.BTN_TL, 'R': e.BTN_TR, 'select': e.BTN_SELECT, 'start': e.BTN_START,
    'mode': e.BTN_MODE, 'thumbl': e.BTN_THUMBL, 'thumbr': e.BTN_THUMBR
}

# Map string axis names to ABS ecodes
AXIS_MAP = {
    'X': e.ABS_X, 'Y': e.ABS_Y,
    'RX': e.ABS_RX, 'RY': e.ABS_RY,
    'Z': e.ABS_Z, 'RZ': e.ABS_RZ,
    'HATX': e.ABS_HAT0X, 'HATY': e.ABS_HAT0Y,
    'HAT0X': e.ABS_HAT0X, 'HAT0Y': e.ABS_HAT0Y
}

# Main loop reading commands from Node.js stdin
try:
    for line in sys.stdin:
        parts = line.strip().split()
        if not parts:
            continue
        
        cmd = parts[0]
        if cmd == 'btn' and len(parts) >= 3:
            name, val = parts[1], int(parts[2])
            btn_code = BTN_MAP.get(name.upper()) if name.upper() in BTN_MAP else BTN_MAP.get(name.lower())
            if btn_code is not None:
                set_btn(btn_code, val)
        elif cmd == 'axis' and len(parts) >= 3:
            name, val = parts[1], int(parts[2])
            axis_code = AXIS_MAP.get(name.upper()) if name.upper() in AXIS_MAP else AXIS_MAP.get(name.lower())
            if axis_code is not None:
                set_axis(axis_code, val)
        
        ui.syn()
except KeyboardInterrupt:
    pass
finally:
    ui.close()
