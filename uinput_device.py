#!/usr/bin/env python3
import sys
import json
import evdev
from evdev import UInput, ecodes as e

# Complete Physical Keyboard & Touchpad Scancode Map
KEY_MAP = {
    "a": e.KEY_A, "b": e.KEY_B, "c": e.KEY_C, "d": e.KEY_D, "e": e.KEY_E,
    "f": e.KEY_F, "g": e.KEY_G, "h": e.KEY_H, "i": e.KEY_I, "j": e.KEY_J,
    "k": e.KEY_K, "l": e.KEY_L, "m": e.KEY_M, "n": e.KEY_N, "o": e.KEY_O,
    "p": e.KEY_P, "q": e.KEY_Q, "r": e.KEY_R, "s": e.KEY_S, "t": e.KEY_T,
    "u": e.KEY_U, "v": e.KEY_V, "w": e.KEY_W, "x": e.KEY_X, "y": e.KEY_Y,
    "z": e.KEY_Z,
    "A": e.KEY_A, "B": e.KEY_B, "C": e.KEY_C, "D": e.KEY_D, "E": e.KEY_E,
    "F": e.KEY_F, "G": e.KEY_G, "H": e.KEY_H, "I": e.KEY_I, "J": e.KEY_J,
    "K": e.KEY_K, "L": e.KEY_L, "M": e.KEY_M, "N": e.KEY_N, "O": e.KEY_O,
    "P": e.KEY_P, "Q": e.KEY_Q, "R": e.KEY_R, "S": e.KEY_S, "T": e.KEY_T,
    "U": e.KEY_U, "V": e.KEY_V, "W": e.KEY_W, "X": e.KEY_X, "Y": e.KEY_Y,
    "Z": e.KEY_Z,
    "1": e.KEY_1, "2": e.KEY_2, "3": e.KEY_3, "4": e.KEY_4, "5": e.KEY_5,
    "6": e.KEY_6, "7": e.KEY_7, "8": e.KEY_8, "9": e.KEY_9, "0": e.KEY_0,
    "space": e.KEY_SPACE,
    "Return": e.KEY_ENTER,
    "BackSpace": e.KEY_BACKSPACE,
    "Tab": e.KEY_TAB,
    "Escape": e.KEY_ESC,
    "Caps_Lock": e.KEY_CAPSLOCK,
    "CapsLock": e.KEY_CAPSLOCK,
    "Caps": e.KEY_CAPSLOCK,
    "Control_L": e.KEY_LEFTCTRL,
    "Control_R": e.KEY_RIGHTCTRL,
    "Shift_L": e.KEY_LEFTSHIFT,
    "Shift_R": e.KEY_RIGHTSHIFT,
    "Alt_L": e.KEY_LEFTALT,
    "Alt_R": e.KEY_RIGHTALT,
    "Super_L": e.KEY_LEFTMETA,
    "Super_R": e.KEY_RIGHTMETA,
    "Left": e.KEY_LEFT,
    "Right": e.KEY_RIGHT,
    "Up": e.KEY_UP,
    "Down": e.KEY_DOWN,
    "Home": e.KEY_HOME,
    "End": e.KEY_END,
    "Page_Up": e.KEY_PAGEUP,
    "Page_Down": e.KEY_PAGEDOWN,
    "Delete": e.KEY_DELETE,
    "Insert": e.KEY_INSERT,
    "grave": e.KEY_GRAVE,
    "minus": e.KEY_MINUS,
    "equal": e.KEY_EQUAL,
    "bracketleft": e.KEY_LEFTBRACE,
    "bracketright": e.KEY_RIGHTBRACE,
    "backslash": e.KEY_BACKSLASH,
    "semicolon": e.KEY_SEMICOLON,
    "apostrophe": e.KEY_APOSTROPHE,
    "comma": e.KEY_COMMA,
    "period": e.KEY_DOT,
    "slash": e.KEY_SLASH,
    "F1": e.KEY_F1, "F2": e.KEY_F2, "F3": e.KEY_F3, "F4": e.KEY_F4,
    "F5": e.KEY_F5, "F6": e.KEY_F6, "F7": e.KEY_F7, "F8": e.KEY_F8,
    "F9": e.KEY_F9, "F10": e.KEY_F10, "F11": e.KEY_F11, "F12": e.KEY_F12
}

all_keys = list(set(KEY_MAP.values())) + [e.BTN_LEFT, e.BTN_RIGHT, e.BTN_MIDDLE]

cap = {
    e.EV_REL: [
        e.REL_X,
        e.REL_Y,
        e.REL_WHEEL,
        e.REL_HWHEEL,
        e.REL_WHEEL_HI_RES,
        e.REL_HWHEEL_HI_RES
    ],
    e.EV_KEY: all_keys
}

try:
    ui = UInput(cap, name="ZenTrack Physical Keyboard & Touchpad", version=0x1)
    print("ZEN_UINPUT_READY", flush=True)
except Exception as err:
    print(f"ZEN_UINPUT_ERROR: {err}", flush=True)
    sys.exit(1)

accum_y = 0
accum_x = 0

for line in sys.stdin:
    line = line.strip()
    if not line:
        continue
    try:
        data = json.loads(line)
        cmd_type = data.get("type")

        if cmd_type == "keydown":
            key_name = data.get("key")
            code = KEY_MAP.get(key_name)
            if code is not None:
                ui.write(e.EV_KEY, code, 1)
                ui.syn()

        elif cmd_type == "keyup":
            key_name = data.get("key")
            code = KEY_MAP.get(key_name)
            if code is not None:
                ui.write(e.EV_KEY, code, 0)
                ui.syn()

        elif cmd_type == "move":
            dx = int(data.get("dx", 0))
            dy = int(data.get("dy", 0))
            if dx or dy:
                if dx: ui.write(e.EV_REL, e.REL_X, dx)
                if dy: ui.write(e.EV_REL, e.REL_Y, dy)
                ui.syn()

        elif cmd_type == "click":
            btn = data.get("button", 1)
            btn_code = e.BTN_LEFT if btn == 1 else (e.BTN_RIGHT if btn == 3 else e.BTN_MIDDLE)
            ui.write(e.EV_KEY, btn_code, 1)
            ui.syn()
            ui.write(e.EV_KEY, btn_code, 0)
            ui.syn()

        elif cmd_type == "mousedown":
            btn = data.get("button", 1)
            btn_code = e.BTN_LEFT if btn == 1 else (e.BTN_RIGHT if btn == 3 else e.BTN_MIDDLE)
            ui.write(e.EV_KEY, btn_code, 1)
            ui.syn()

        elif cmd_type == "mouseup":
            btn = data.get("button", 1)
            btn_code = e.BTN_LEFT if btn == 1 else (e.BTN_RIGHT if btn == 3 else e.BTN_MIDDLE)
            ui.write(e.EV_KEY, btn_code, 0)
            ui.syn()

        elif cmd_type == "smooth_scroll":
            dx = float(data.get("dx", 0))
            dy = float(data.get("dy", 0))
            hi_res_y = int(-dy * 14)
            hi_res_x = int(dx * 14)

            if hi_res_y:
                ui.write(e.EV_REL, e.REL_WHEEL_HI_RES, hi_res_y)
                accum_y += hi_res_y
                if abs(accum_y) >= 120:
                    notches = int(accum_y / 120)
                    ui.write(e.EV_REL, e.REL_WHEEL, notches)
                    accum_y -= notches * 120

            if hi_res_x:
                ui.write(e.EV_REL, e.REL_HWHEEL_HI_RES, hi_res_x)
                accum_x += hi_res_x
                if abs(accum_x) >= 120:
                    notches = int(accum_x / 120)
                    ui.write(e.EV_REL, e.REL_HWHEEL, notches)
                    accum_x -= notches * 120

            ui.syn()

    except Exception:
        pass
