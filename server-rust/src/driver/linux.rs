use super::InputDriver;
use evdev::uinput::{VirtualDevice, VirtualDeviceBuilder};
use evdev::{
    AbsInfo, AbsoluteAxisType, AttributeSet, EventType, InputEvent, Key, RelativeAxisType,
    UinputAbsSetup,
};
use std::collections::HashSet;

pub struct LinuxDriver {
    touchpad: Option<VirtualDevice>,
    gamepad: Option<VirtualDevice>,
    active_modifiers: HashSet<String>,
    accum_scroll_y: i32,
    accum_scroll_x: i32,
}

impl LinuxDriver {
    pub fn new() -> Result<Self, String> {
        let touchpad = Self::create_touchpad()?;
        Ok(Self {
            touchpad: Some(touchpad),
            gamepad: None,
            active_modifiers: HashSet::new(),
            accum_scroll_y: 0,
            accum_scroll_x: 0,
        })
    }

    fn create_touchpad() -> Result<VirtualDevice, String> {
        let mut keys = AttributeSet::<Key>::new();
        // Mouse buttons
        keys.insert(Key::BTN_LEFT);
        keys.insert(Key::BTN_RIGHT);
        keys.insert(Key::BTN_MIDDLE);

        // Standard Keyboard Keys
        let key_list = [
            Key::KEY_A, Key::KEY_B, Key::KEY_C, Key::KEY_D, Key::KEY_E, Key::KEY_F,
            Key::KEY_G, Key::KEY_H, Key::KEY_I, Key::KEY_J, Key::KEY_K, Key::KEY_L,
            Key::KEY_M, Key::KEY_N, Key::KEY_O, Key::KEY_P, Key::KEY_Q, Key::KEY_R,
            Key::KEY_S, Key::KEY_T, Key::KEY_U, Key::KEY_V, Key::KEY_W, Key::KEY_X,
            Key::KEY_Y, Key::KEY_Z, Key::KEY_0, Key::KEY_1, Key::KEY_2, Key::KEY_3,
            Key::KEY_4, Key::KEY_5, Key::KEY_6, Key::KEY_7, Key::KEY_8, Key::KEY_9,
            Key::KEY_SPACE, Key::KEY_ENTER, Key::KEY_BACKSPACE, Key::KEY_TAB,
            Key::KEY_ESC, Key::KEY_CAPSLOCK, Key::KEY_LEFTCTRL, Key::KEY_RIGHTCTRL,
            Key::KEY_LEFTSHIFT, Key::KEY_RIGHTSHIFT, Key::KEY_LEFTALT, Key::KEY_RIGHTALT,
            Key::KEY_LEFTMETA, Key::KEY_RIGHTMETA, Key::KEY_LEFT, Key::KEY_RIGHT,
            Key::KEY_UP, Key::KEY_DOWN, Key::KEY_HOME, Key::KEY_END, Key::KEY_PAGEUP,
            Key::KEY_PAGEDOWN, Key::KEY_DELETE, Key::KEY_INSERT, Key::KEY_GRAVE,
            Key::KEY_MINUS, Key::KEY_EQUAL, Key::KEY_LEFTBRACE, Key::KEY_RIGHTBRACE,
            Key::KEY_BACKSLASH, Key::KEY_SEMICOLON, Key::KEY_APOSTROPHE, Key::KEY_COMMA,
            Key::KEY_DOT, Key::KEY_SLASH, Key::KEY_F1, Key::KEY_F2, Key::KEY_F3,
            Key::KEY_F4, Key::KEY_F5, Key::KEY_F6, Key::KEY_F7, Key::KEY_F8,
            Key::KEY_F9, Key::KEY_F10, Key::KEY_F11, Key::KEY_F12,
        ];
        for k in key_list {
            keys.insert(k);
        }

        let mut rel_axes = AttributeSet::<RelativeAxisType>::new();
        rel_axes.insert(RelativeAxisType::REL_X);
        rel_axes.insert(RelativeAxisType::REL_Y);
        rel_axes.insert(RelativeAxisType::REL_WHEEL);
        rel_axes.insert(RelativeAxisType::REL_HWHEEL);
        rel_axes.insert(RelativeAxisType::REL_WHEEL_HI_RES);
        rel_axes.insert(RelativeAxisType::REL_HWHEEL_HI_RES);

        VirtualDeviceBuilder::new()
            .map_err(|e| format!("Failed to initialize uinput: {:?}", e))?
            .name("ZenTrack Native Physical Touchpad & Keyboard")
            .with_keys(&keys)
            .map_err(|e| format!("Failed to register keys: {:?}", e))?
            .with_relative_axes(&rel_axes)
            .map_err(|e| format!("Failed to register relative axes: {:?}", e))?
            .build()
            .map_err(|e| format!("Failed to build VirtualDevice: {:?}", e))
    }

    fn create_gamepad() -> Result<VirtualDevice, String> {
        let mut keys = AttributeSet::<Key>::new();
        keys.insert(Key::BTN_SOUTH);
        keys.insert(Key::BTN_EAST);
        keys.insert(Key::BTN_NORTH);
        keys.insert(Key::BTN_WEST);
        keys.insert(Key::BTN_TL);
        keys.insert(Key::BTN_TR);
        keys.insert(Key::BTN_SELECT);
        keys.insert(Key::BTN_START);
        keys.insert(Key::BTN_MODE);
        keys.insert(Key::BTN_THUMBL);
        keys.insert(Key::BTN_THUMBR);

        let abs_stick = AbsInfo::new(0, -32768, 32767, 16, 128, 0);
        let abs_trigger = AbsInfo::new(0, 0, 255, 0, 0, 0);
        let abs_hat = AbsInfo::new(0, -1, 1, 0, 0, 0);

        VirtualDeviceBuilder::new()
            .map_err(|e| format!("Failed to initialize uinput gamepad: {:?}", e))?
            .name("Microsoft X-Box 360 pad")
            .with_keys(&keys)
            .map_err(|e| format!("Failed to set gamepad keys: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_X, abs_stick))
            .map_err(|e| format!("Failed axis ABS_X: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_Y, abs_stick))
            .map_err(|e| format!("Failed axis ABS_Y: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RX, abs_stick))
            .map_err(|e| format!("Failed axis ABS_RX: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RY, abs_stick))
            .map_err(|e| format!("Failed axis ABS_RY: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_Z, abs_trigger))
            .map_err(|e| format!("Failed axis ABS_Z: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_RZ, abs_trigger))
            .map_err(|e| format!("Failed axis ABS_RZ: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_HAT0X, abs_hat))
            .map_err(|e| format!("Failed axis ABS_HAT0X: {:?}", e))?
            .with_absolute_axis(&UinputAbsSetup::new(AbsoluteAxisType::ABS_HAT0Y, abs_hat))
            .map_err(|e| format!("Failed axis ABS_HAT0Y: {:?}", e))?
            .build()
            .map_err(|e| format!("Failed to build gamepad VirtualDevice: {:?}", e))
    }
}

impl InputDriver for LinuxDriver {
    fn mouse_move(&mut self, dx: i32, dy: i32) {
        if let Some(dev) = self.touchpad.as_mut() {
            let mut evs = Vec::with_capacity(2);
            if dx != 0 {
                evs.push(InputEvent::new(EventType::RELATIVE, RelativeAxisType::REL_X.0, dx));
            }
            if dy != 0 {
                evs.push(InputEvent::new(EventType::RELATIVE, RelativeAxisType::REL_Y.0, dy));
            }
            if !evs.is_empty() {
                let _ = dev.emit(&evs);
            }
        }
    }

    fn mouse_down(&mut self, button: u8) {
        if let Some(dev) = self.touchpad.as_mut() {
            let key = match button {
                1 => Key::BTN_LEFT,
                2 => Key::BTN_MIDDLE,
                3 => Key::BTN_RIGHT,
                _ => return,
            };
            let ev = InputEvent::new(EventType::KEY, key.0, 1);
            let _ = dev.emit(&[ev]);
        }
    }

    fn mouse_up(&mut self, button: u8) {
        if let Some(dev) = self.touchpad.as_mut() {
            let key = match button {
                1 => Key::BTN_LEFT,
                2 => Key::BTN_MIDDLE,
                3 => Key::BTN_RIGHT,
                _ => return,
            };
            let ev = InputEvent::new(EventType::KEY, key.0, 0);
            let _ = dev.emit(&[ev]);
        }
    }

    fn mouse_click(&mut self, button: u8, double: bool) {
        self.mouse_down(button);
        self.mouse_up(button);
        if double {
            std::thread::sleep(std::time::Duration::from_millis(50));
            self.mouse_down(button);
            self.mouse_up(button);
        }
    }

    fn smooth_scroll(&mut self, dx: f64, dy: f64) {
        if let Some(dev) = self.touchpad.as_mut() {
            let mut evs = Vec::new();

            let hi_res_y = (-dy * 14.0) as i32;
            let hi_res_x = (dx * 14.0) as i32;

            if hi_res_y != 0 {
                evs.push(InputEvent::new(
                    EventType::RELATIVE,
                    RelativeAxisType::REL_WHEEL_HI_RES.0,
                    hi_res_y,
                ));
                self.accum_scroll_y += hi_res_y;
                if self.accum_scroll_y.abs() >= 120 {
                    let notches = self.accum_scroll_y / 120;
                    self.accum_scroll_y %= 120;
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_WHEEL.0,
                        notches,
                    ));
                }
            }

            if hi_res_x != 0 {
                evs.push(InputEvent::new(
                    EventType::RELATIVE,
                    RelativeAxisType::REL_HWHEEL_HI_RES.0,
                    hi_res_x,
                ));
                self.accum_scroll_x += hi_res_x;
                if self.accum_scroll_x.abs() >= 120 {
                    let notches = self.accum_scroll_x / 120;
                    self.accum_scroll_x %= 120;
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_HWHEEL.0,
                        notches,
                    ));
                }
            }

            if !evs.is_empty() {
                let _ = dev.emit(&evs);
            }
        }
    }

    fn scroll_discrete(&mut self, direction: &str, steps: i32) {
        if let Some(dev) = self.touchpad.as_mut() {
            let mut evs = Vec::new();
            match direction {
                "up" => {
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_WHEEL.0,
                        steps,
                    ));
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_WHEEL_HI_RES.0,
                        steps * 120,
                    ));
                }
                "down" => {
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_WHEEL.0,
                        -steps,
                    ));
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_WHEEL_HI_RES.0,
                        -steps * 120,
                    ));
                }
                "left" => {
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_HWHEEL.0,
                        -steps,
                    ));
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_HWHEEL_HI_RES.0,
                        -steps * 120,
                    ));
                }
                "right" => {
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_HWHEEL.0,
                        steps,
                    ));
                    evs.push(InputEvent::new(
                        EventType::RELATIVE,
                        RelativeAxisType::REL_HWHEEL_HI_RES.0,
                        steps * 120,
                    ));
                }
                _ => {}
            }
            if !evs.is_empty() {
                let _ = dev.emit(&evs);
            }
        }
    }

    fn key_down(&mut self, key: &str) {
        if let Some(dev) = self.touchpad.as_mut() {
            if let Some(k) = map_evdev_key(key) {
                self.active_modifiers.insert(key.to_string());
                let ev = InputEvent::new(EventType::KEY, k.0, 1);
                let _ = dev.emit(&[ev]);
            }
        }
    }

    fn key_up(&mut self, key: &str) {
        if let Some(dev) = self.touchpad.as_mut() {
            if let Some(k) = map_evdev_key(key) {
                self.active_modifiers.remove(key);
                let ev = InputEvent::new(EventType::KEY, k.0, 0);
                let _ = dev.emit(&[ev]);
            }
        }
    }

    fn key_click(&mut self, key: &str) {
        self.key_down(key);
        self.key_up(key);
    }

    fn release_all_modifiers(&mut self) {
        let keys_to_release: Vec<String> = self.active_modifiers.drain().collect();
        for k in keys_to_release {
            self.key_up(&k);
        }
    }

    fn set_gamepad_mode(&mut self, enabled: bool) {
        if enabled {
            if self.gamepad.is_none() {
                match Self::create_gamepad() {
                    Ok(pad) => {
                        println!("[LinuxDriver] Virtual Xbox 360 Gamepad initialized");
                        self.gamepad = Some(pad);
                    }
                    Err(e) => eprintln!("[LinuxDriver] Failed to create gamepad: {}", e),
                }
            }
        } else {
            self.gamepad = None;
        }
    }

    fn gamepad_btn(&mut self, name: &str, value: i32) {
        if let Some(pad) = self.gamepad.as_mut() {
            let key = match name.to_uppercase().as_str() {
                "A" => Key::BTN_SOUTH,
                "B" => Key::BTN_EAST,
                "X" => Key::BTN_NORTH,
                "Y" => Key::BTN_WEST,
                "L" => Key::BTN_TL,
                "R" => Key::BTN_TR,
                "SELECT" => Key::BTN_SELECT,
                "START" => Key::BTN_START,
                "MODE" => Key::BTN_MODE,
                "THUMBL" => Key::BTN_THUMBL,
                "THUMBR" => Key::BTN_THUMBR,
                _ => return,
            };
            let ev = InputEvent::new(EventType::KEY, key.0, if value != 0 { 1 } else { 0 });
            let _ = pad.emit(&[ev]);
        }
    }

    fn gamepad_axis(&mut self, name: &str, val: i32) {
        if let Some(pad) = self.gamepad.as_mut() {
            let (axis, scaled) = match name.to_uppercase().as_str() {
                "X" => (AbsoluteAxisType::ABS_X, scale_stick(val)),
                "Y" => (AbsoluteAxisType::ABS_Y, scale_stick(val)),
                "RX" => (AbsoluteAxisType::ABS_RX, scale_stick(val)),
                "RY" => (AbsoluteAxisType::ABS_RY, scale_stick(val)),
                "Z" => (AbsoluteAxisType::ABS_Z, val.clamp(0, 255)),
                "RZ" => (AbsoluteAxisType::ABS_RZ, val.clamp(0, 255)),
                "HATX" | "HAT0X" => (AbsoluteAxisType::ABS_HAT0X, val.clamp(-1, 1)),
                "HATY" | "HAT0Y" => (AbsoluteAxisType::ABS_HAT0Y, val.clamp(-1, 1)),
                _ => return,
            };
            let ev = InputEvent::new(EventType::ABSOLUTE, axis.0, scaled);
            let _ = pad.emit(&[ev]);
        }
    }
}

fn scale_stick(val: i32) -> i32 {
    let clamped = val.clamp(0, 255);
    let scaled = ((clamped - 128) as f64 / 128.0) * 32767.0;
    (scaled as i32).clamp(-32768, 32767)
}

fn map_evdev_key(key: &str) -> Option<Key> {
    match key {
        "a" | "A" => Some(Key::KEY_A),
        "b" | "B" => Some(Key::KEY_B),
        "c" | "C" => Some(Key::KEY_C),
        "d" | "D" => Some(Key::KEY_D),
        "e" | "E" => Some(Key::KEY_E),
        "f" | "F" => Some(Key::KEY_F),
        "g" | "G" => Some(Key::KEY_G),
        "h" | "H" => Some(Key::KEY_H),
        "i" | "I" => Some(Key::KEY_I),
        "j" | "J" => Some(Key::KEY_J),
        "k" | "K" => Some(Key::KEY_K),
        "l" | "L" => Some(Key::KEY_L),
        "m" | "M" => Some(Key::KEY_M),
        "n" | "N" => Some(Key::KEY_N),
        "o" | "O" => Some(Key::KEY_O),
        "p" | "P" => Some(Key::KEY_P),
        "q" | "Q" => Some(Key::KEY_Q),
        "r" | "R" => Some(Key::KEY_R),
        "s" | "S" => Some(Key::KEY_S),
        "t" | "T" => Some(Key::KEY_T),
        "u" | "U" => Some(Key::KEY_U),
        "v" | "V" => Some(Key::KEY_V),
        "w" | "W" => Some(Key::KEY_W),
        "x" | "X" => Some(Key::KEY_X),
        "y" | "Y" => Some(Key::KEY_Y),
        "z" | "Z" => Some(Key::KEY_Z),
        "0" => Some(Key::KEY_0),
        "1" => Some(Key::KEY_1),
        "2" => Some(Key::KEY_2),
        "3" => Some(Key::KEY_3),
        "4" => Some(Key::KEY_4),
        "5" => Some(Key::KEY_5),
        "6" => Some(Key::KEY_6),
        "7" => Some(Key::KEY_7),
        "8" => Some(Key::KEY_8),
        "9" => Some(Key::KEY_9),
        "space" => Some(Key::KEY_SPACE),
        "Return" => Some(Key::KEY_ENTER),
        "BackSpace" => Some(Key::KEY_BACKSPACE),
        "Tab" => Some(Key::KEY_TAB),
        "Escape" => Some(Key::KEY_ESC),
        "Caps_Lock" | "CapsLock" | "Caps" => Some(Key::KEY_CAPSLOCK),
        "Control_L" => Some(Key::KEY_LEFTCTRL),
        "Control_R" => Some(Key::KEY_RIGHTCTRL),
        "Shift_L" => Some(Key::KEY_LEFTSHIFT),
        "Shift_R" => Some(Key::KEY_RIGHTSHIFT),
        "Alt_L" => Some(Key::KEY_LEFTALT),
        "Alt_R" => Some(Key::KEY_RIGHTALT),
        "Super_L" => Some(Key::KEY_LEFTMETA),
        "Super_R" => Some(Key::KEY_RIGHTMETA),
        "Left" => Some(Key::KEY_LEFT),
        "Right" => Some(Key::KEY_RIGHT),
        "Up" => Some(Key::KEY_UP),
        "Down" => Some(Key::KEY_DOWN),
        "Home" => Some(Key::KEY_HOME),
        "End" => Some(Key::KEY_END),
        "Page_Up" => Some(Key::KEY_PAGEUP),
        "Page_Down" => Some(Key::KEY_PAGEDOWN),
        "Delete" => Some(Key::KEY_DELETE),
        "Insert" => Some(Key::KEY_INSERT),
        "grave" => Some(Key::KEY_GRAVE),
        "minus" => Some(Key::KEY_MINUS),
        "equal" => Some(Key::KEY_EQUAL),
        "bracketleft" => Some(Key::KEY_LEFTBRACE),
        "bracketright" => Some(Key::KEY_RIGHTBRACE),
        "backslash" => Some(Key::KEY_BACKSLASH),
        "semicolon" => Some(Key::KEY_SEMICOLON),
        "apostrophe" => Some(Key::KEY_APOSTROPHE),
        "comma" => Some(Key::KEY_COMMA),
        "period" => Some(Key::KEY_DOT),
        "slash" => Some(Key::KEY_SLASH),
        "F1" => Some(Key::KEY_F1),
        "F2" => Some(Key::KEY_F2),
        "F3" => Some(Key::KEY_F3),
        "F4" => Some(Key::KEY_F4),
        "F5" => Some(Key::KEY_F5),
        "F6" => Some(Key::KEY_F6),
        "F7" => Some(Key::KEY_F7),
        "F8" => Some(Key::KEY_F8),
        "F9" => Some(Key::KEY_F9),
        "F10" => Some(Key::KEY_F10),
        "F11" => Some(Key::KEY_F11),
        "F12" => Some(Key::KEY_F12),
        _ => None,
    }
}
