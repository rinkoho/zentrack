#!/usr/bin/env python
import sys
import os

try:
    from evdev import UInput, ecodes as e
except ImportError:
    print("ERROR: python-evdev is not installed. Please run 'sudo pacman -S python-evdev'", file=sys.stderr, flush=True)
    sys.exit(1)

# Register capabilities of Xbox 360 controller
cap = {
    e.EV_KEY: [
        e.BTN_A, e.BTN_B, e.BTN_X, e.BTN_Y,
        e.BTN_TL, e.BTN_TR, e.BTN_SELECT, e.BTN_START, e.BTN_MODE,
        e.BTN_THUMBL, e.BTN_THUMBR
    ],
    e.EV_ABS: [
        (e.ABS_X, (0, 0, 255, 0, 0)),  # Left Stick X (0 to 255, center 128)
        (e.ABS_Y, (0, 0, 255, 0, 0)),  # Left Stick Y
        (e.ABS_RX, (0, 0, 255, 0, 0)), # Right Stick X
        (e.ABS_RY, (0, 0, 255, 0, 0)), # Right Stick Y
        (e.ABS_Z, (0, 0, 255, 0, 0)),  # L Trigger
        (e.ABS_RZ, (0, 0, 255, 0, 0))  # R Trigger
    ]
}

try:
    ui = UInput(cap, name="ZenGamepad Virtual Xbox 360 Controller", vendor=0x045e, product=0x028e)
    print("SUCCESS: Virtual Gamepad created successfully.", flush=True)
except Exception as err:
    print(f"ERROR: Could not create uinput device. {err}", file=sys.stderr, flush=True)
    print("Please ensure uinput module is loaded ('sudo modprobe uinput') and you have write permissions to /dev/uinput.", file=sys.stderr, flush=True)
    sys.exit(1)

# Axis state cache to prevent redundant writes
axis_states = {
    e.ABS_X: 128, e.ABS_Y: 128,
    e.ABS_RX: 128, e.ABS_RY: 128,
    e.ABS_Z: 0, e.ABS_RZ: 0
}

def set_axis(axis, val):
    val = max(0, min(255, val))
    if axis_states[axis] != val:
        axis_states[axis] = val
        ui.write(e.EV_ABS, axis, val)

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
    'Z': e.ABS_Z, 'RZ': e.ABS_RZ
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
            btn_code = BTN_MAP.get(name.upper()) or BTN_MAP.get(name.lower())
            if btn_code is not None:
                set_btn(btn_code, val)
        elif cmd == 'axis' and len(parts) >= 3:
            name, val = parts[1], int(parts[2])
            axis_code = AXIS_MAP.get(name.upper()) or AXIS_MAP.get(name.lower())
            if axis_code is not None:
                set_axis(axis_code, val)
        
        ui.syn()
except KeyboardInterrupt:
    pass
finally:
    ui.close()
