use super::InputDriver;

#[cfg(target_os = "windows")]
use windows_sys::Win32::UI::Input::KeyboardAndMouse::*;

#[cfg(target_os = "windows")]
use vigem_client::{Client, TargetId, Xbox360Wired, XGamepad, XButtons};

pub struct WindowsDriver {
    #[cfg(target_os = "windows")]
    accum_scroll_y: f64,
    #[cfg(target_os = "windows")]
    accum_scroll_x: f64,
    #[cfg(target_os = "windows")]
    repeat_cancel: Option<tokio::sync::oneshot::Sender<()>>,
    #[cfg(target_os = "windows")]
    gamepad: Option<Xbox360Wired<Client>>,
    #[cfg(target_os = "windows")]
    gamepad_state: XGamepad,
}

unsafe impl Send for WindowsDriver {}
unsafe impl Sync for WindowsDriver {}

impl WindowsDriver {
    pub fn new() -> Result<Self, String> {
        Ok(Self {
            #[cfg(target_os = "windows")]
            accum_scroll_y: 0.0,
            #[cfg(target_os = "windows")]
            accum_scroll_x: 0.0,
            #[cfg(target_os = "windows")]
            repeat_cancel: None,
            #[cfg(target_os = "windows")]
            gamepad: None,
            #[cfg(target_os = "windows")]
            gamepad_state: XGamepad::default(),
        })
    }
}

#[cfg(target_os = "windows")]
impl Drop for WindowsDriver {
    fn drop(&mut self) {
        if let Some(mut pad) = self.gamepad.take() {
            let _ = pad.unplug();
        }
    }
}

#[cfg(target_os = "windows")]
#[inline]
unsafe fn send_input_with_fallback(inputs: &[INPUT]) {
    let count = inputs.len() as u32;
    let size = std::mem::size_of::<INPUT>() as i32;
    let res = SendInput(count, inputs.as_ptr(), size);
    if res == 0 {
        // If SendInput failed (e.g. desktop switched to lockscreen / UAC secure desktop),
        // try switching thread desktop to the active input desktop and retry.
        use windows_sys::Win32::System::StationsAndDesktops::{OpenInputDesktop, SetThreadDesktop, CloseDesktop};
        use windows_sys::Win32::Foundation::GENERIC_ALL;
        let h_desk = OpenInputDesktop(0, 0, GENERIC_ALL);
        if h_desk != 0 {
            SetThreadDesktop(h_desk);
            SendInput(count, inputs.as_ptr(), size);
            CloseDesktop(h_desk);
        }
    }
}

#[cfg(target_os = "windows")]
impl InputDriver for WindowsDriver {
    fn mouse_move(&mut self, dx: i32, dy: i32) {
        unsafe {
            let mut input: INPUT = std::mem::zeroed();
            input.r#type = INPUT_MOUSE;
            input.Anonymous.mi = MOUSEINPUT {
                dx,
                dy,
                mouseData: 0,
                dwFlags: MOUSEEVENTF_MOVE,
                time: 0,
                dwExtraInfo: 0,
            };
            send_input_with_fallback(&[input]);
        }
    }

    fn mouse_down(&mut self, button: u8) {
        let flag = match button {
            1 => MOUSEEVENTF_LEFTDOWN,
            2 => MOUSEEVENTF_MIDDLEDOWN,
            3 => MOUSEEVENTF_RIGHTDOWN,
            _ => return,
        };
        unsafe {
            let mut input: INPUT = std::mem::zeroed();
            input.r#type = INPUT_MOUSE;
            input.Anonymous.mi.dwFlags = flag;
            send_input_with_fallback(&[input]);
        }
    }

    fn mouse_up(&mut self, button: u8) {
        let flag = match button {
            1 => MOUSEEVENTF_LEFTUP,
            2 => MOUSEEVENTF_MIDDLEUP,
            3 => MOUSEEVENTF_RIGHTUP,
            _ => return,
        };
        unsafe {
            let mut input: INPUT = std::mem::zeroed();
            input.r#type = INPUT_MOUSE;
            input.Anonymous.mi.dwFlags = flag;
            send_input_with_fallback(&[input]);
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
        // High-resolution Windows wheel input
        self.accum_scroll_y += -dy * 12.0;
        self.accum_scroll_x += dx * 12.0;

        let delta_y = self.accum_scroll_y as i32;
        if delta_y != 0 {
            self.accum_scroll_y -= delta_y as f64;
            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_MOUSE;
                input.Anonymous.mi = MOUSEINPUT {
                    dx: 0,
                    dy: 0,
                    mouseData: delta_y as u32,
                    dwFlags: MOUSEEVENTF_WHEEL,
                    time: 0,
                    dwExtraInfo: 0,
                };
                send_input_with_fallback(&[input]);
            }
        }

        let delta_x = self.accum_scroll_x as i32;
        if delta_x != 0 {
            self.accum_scroll_x -= delta_x as f64;
            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_MOUSE;
                input.Anonymous.mi = MOUSEINPUT {
                    dx: 0,
                    dy: 0,
                    mouseData: delta_x as u32,
                    dwFlags: MOUSEEVENTF_HWHEEL,
                    time: 0,
                    dwExtraInfo: 0,
                };
                send_input_with_fallback(&[input]);
            }
        }
    }

    fn scroll_discrete(&mut self, direction: &str, steps: i32) {
        let clicks = steps * 120;
        match direction {
            "up" => self.smooth_scroll(0.0, -clicks as f64 / 12.0),
            "down" => self.smooth_scroll(0.0, clicks as f64 / 12.0),
            "left" => self.smooth_scroll(-clicks as f64 / 12.0, 0.0),
            "right" => self.smooth_scroll(clicks as f64 / 12.0, 0.0),
            _ => {}
        }
    }

    fn key_down(&mut self, key: &str) {
        if let Some(cancel) = self.repeat_cancel.take() {
            let _ = cancel.send(());
        }

        if let Some(vk) = map_windows_key(key) {
            let scan = unsafe { MapVirtualKeyW(vk as u32, 0) as u16 };
            let is_extended = is_extended_key(vk);
            let mut flags = if is_extended { KEYEVENTF_EXTENDEDKEY } else { 0 };
            
            // Windows 11 DirectX / Raw Input compatibility
            if scan != 0 {
                flags |= KEYEVENTF_SCANCODE;
            }

            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_KEYBOARD;
                input.Anonymous.ki = KEYBDINPUT {
                    wVk: vk,
                    wScan: scan,
                    dwFlags: flags,
                    time: 0,
                    dwExtraInfo: 0,
                };
                send_input_with_fallback(&[input]);
            }

            if is_repeatable_key(key) {
                let (tx, mut rx) = tokio::sync::oneshot::channel::<()>();
                self.repeat_cancel = Some(tx);

                tokio::spawn(async move {
                    tokio::select! {
                        _ = &mut rx => return,
                        _ = tokio::time::sleep(std::time::Duration::from_millis(300)) => {}
                    }

                    let mut interval = tokio::time::interval(std::time::Duration::from_millis(33));
                    interval.tick().await;

                    loop {
                        tokio::select! {
                            _ = &mut rx => break,
                            _ = interval.tick() => {
                                unsafe {
                                    let mut input: INPUT = std::mem::zeroed();
                                    input.r#type = INPUT_KEYBOARD;
                                    input.Anonymous.ki = KEYBDINPUT {
                                        wVk: vk,
                                        wScan: scan,
                                        dwFlags: flags,
                                        time: 0,
                                        dwExtraInfo: 0,
                                    };
                                    send_input_with_fallback(&[input]);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    fn key_up(&mut self, key: &str) {
        if let Some(cancel) = self.repeat_cancel.take() {
            let _ = cancel.send(());
        }

        if let Some(vk) = map_windows_key(key) {
            let scan = unsafe { MapVirtualKeyW(vk as u32, 0) as u16 };
            let is_extended = is_extended_key(vk);
            let mut flags = KEYEVENTF_KEYUP | if is_extended { KEYEVENTF_EXTENDEDKEY } else { 0 };
            
            // Windows 11 DirectX / Raw Input compatibility
            if scan != 0 {
                flags |= KEYEVENTF_SCANCODE;
            }

            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_KEYBOARD;
                input.Anonymous.ki = KEYBDINPUT {
                    wVk: vk,
                    wScan: scan,
                    dwFlags: flags,
                    time: 0,
                    dwExtraInfo: 0,
                };
                send_input_with_fallback(&[input]);
            }
        }
    }

    fn key_click(&mut self, key: &str) {
        self.key_down(key);
        std::thread::sleep(std::time::Duration::from_millis(10));
        self.key_up(key);
    }

    fn release_all_modifiers(&mut self) {
        if let Some(cancel) = self.repeat_cancel.take() {
            let _ = cancel.send(());
        }
        for key in &[
            "Control_L", "Control_R", "Alt_L", "Alt_R",
            "Shift_L", "Shift_R", "Super_L", "Super_R",
        ] {
            self.key_up(key);
        }
    }

    fn set_gamepad_mode(&mut self, enabled: bool) {
        if enabled {
            if self.gamepad.is_none() {
                match Client::connect() {
                    Ok(client) => {
                        let id = TargetId::XBOX360_WIRED;
                        let mut target = Xbox360Wired::new(client, id);
                        if let Err(e) = target.plugin() {
                            eprintln!("[WindowsDriver] Failed to plugin virtual Xbox 360 gamepad: {:?}", e);
                            return;
                        }
                        if let Err(e) = target.wait_ready() {
                            eprintln!("[WindowsDriver] Failed waiting for virtual Xbox 360 gamepad: {:?}", e);
                            return;
                        }
                        println!("[WindowsDriver] Virtual Xbox 360 Gamepad connected via ViGEmBus!");
                        self.gamepad_state = XGamepad::default();
                        let _ = target.update(&self.gamepad_state);
                        self.gamepad = Some(target);
                    }
                    Err(e) => {
                        eprintln!("[WindowsDriver] ViGEmBus driver not found or failed to connect ({:?}).", e);
                        eprintln!("[WindowsDriver] To use Xbox Gamepad on Windows, install ViGEmBus: 'winget install Nefarius.ViGEmBus'");
                    }
                }
            }
        } else {
            if let Some(mut pad) = self.gamepad.take() {
                let _ = pad.unplug();
                println!("[WindowsDriver] Virtual Xbox 360 Gamepad disconnected.");
            }
        }
    }

    fn gamepad_btn(&mut self, name: &str, value: i32) {
        let is_down = value != 0;
        let mask = match name.to_uppercase().as_str() {
            "A" => XButtons::A,
            "B" => XButtons::B,
            "X" => XButtons::X,
            "Y" => XButtons::Y,
            "L" | "LB" => XButtons::LB,
            "R" | "RB" => XButtons::RB,
            "SELECT" | "BACK" => XButtons::BACK,
            "START" => XButtons::START,
            "MODE" | "GUIDE" => XButtons::GUIDE,
            "THUMBL" => XButtons::LTHUMB,
            "THUMBR" => XButtons::RTHUMB,
            _ => return,
        };

        if is_down {
            self.gamepad_state.buttons.raw |= mask;
        } else {
            self.gamepad_state.buttons.raw &= !mask;
        }

        if let Some(pad) = self.gamepad.as_mut() {
            let _ = pad.update(&self.gamepad_state);
        }
    }

    fn gamepad_axis(&mut self, name: &str, val: i32) {
        match name.to_uppercase().as_str() {
            "X" => {
                self.gamepad_state.thumb_lx = scale_stick(val);
            }
            "Y" => {
                // In XInput / ViGEm, Y axis: positive is UP, negative is DOWN.
                self.gamepad_state.thumb_ly = -scale_stick(val);
            }
            "RX" => {
                self.gamepad_state.thumb_rx = scale_stick(val);
            }
            "RY" => {
                self.gamepad_state.thumb_ry = -scale_stick(val);
            }
            "Z" => {
                // Left Trigger (0 to 255)
                self.gamepad_state.left_trigger = val.clamp(0, 255) as u8;
            }
            "RZ" => {
                // Right Trigger (0 to 255)
                self.gamepad_state.right_trigger = val.clamp(0, 255) as u8;
            }
            "HATX" | "HAT0X" => {
                self.gamepad_state.buttons.raw &= !(XButtons::LEFT | XButtons::RIGHT);
                if val < 0 {
                    self.gamepad_state.buttons.raw |= XButtons::LEFT;
                } else if val > 0 {
                    self.gamepad_state.buttons.raw |= XButtons::RIGHT;
                }
            }
            "HATY" | "HAT0Y" => {
                self.gamepad_state.buttons.raw &= !(XButtons::UP | XButtons::DOWN);
                if val < 0 {
                    self.gamepad_state.buttons.raw |= XButtons::UP;
                } else if val > 0 {
                    self.gamepad_state.buttons.raw |= XButtons::DOWN;
                }
            }
            _ => return,
        }

        if let Some(pad) = self.gamepad.as_mut() {
            let _ = pad.update(&self.gamepad_state);
        }
    }
}

#[cfg(not(target_os = "windows"))]
impl InputDriver for WindowsDriver {
    fn mouse_move(&mut self, _dx: i32, _dy: i32) {}
    fn mouse_down(&mut self, _button: u8) {}
    fn mouse_up(&mut self, _button: u8) {}
    fn mouse_click(&mut self, _button: u8, _double: bool) {}
    fn smooth_scroll(&mut self, _dx: f64, _dy: f64) {}
    fn scroll_discrete(&mut self, _direction: &str, _steps: i32) {}
    fn key_down(&mut self, _key: &str) {}
    fn key_up(&mut self, _key: &str) {}
    fn key_click(&mut self, _key: &str) {}
    fn release_all_modifiers(&mut self) {}
    fn set_gamepad_mode(&mut self, _enabled: bool) {}
    fn gamepad_btn(&mut self, _name: &str, _value: i32) {}
    fn gamepad_axis(&mut self, _name: &str, _value: i32) {}
}

#[cfg(target_os = "windows")]
fn scale_stick(val: i32) -> i16 {
    let clamped = val.clamp(0, 255);
    let scaled = ((clamped - 128) as f64 / 128.0) * 32767.0;
    (scaled as i32).clamp(-32768, 32767) as i16
}

#[cfg(target_os = "windows")]
fn is_repeatable_key(key: &str) -> bool {
    !matches!(
        key,
        "Control_L" | "Control_R" | "Shift_L" | "Shift_R" |
        "Alt_L" | "Alt_R" | "Super_L" | "Super_R" | "Caps_Lock"
    )
}

#[cfg(target_os = "windows")]
fn is_extended_key(vk: u16) -> bool {
    matches!(
        vk,
        VK_LEFT | VK_RIGHT | VK_UP | VK_DOWN |
        VK_PRIOR | VK_NEXT | VK_END | VK_HOME |
        VK_INSERT | VK_DELETE | VK_DIVIDE |
        VK_RCONTROL | VK_RMENU | VK_LWIN | VK_RWIN
    )
}

#[cfg(target_os = "windows")]
fn map_windows_key(key: &str) -> Option<u16> {
    match key {
        // Alphabet
        "a" | "A" => Some(VK_A),
        "b" | "B" => Some(VK_B),
        "c" | "C" => Some(VK_C),
        "d" | "D" => Some(VK_D),
        "e" | "E" => Some(VK_E),
        "f" | "F" => Some(VK_F),
        "g" | "G" => Some(VK_G),
        "h" | "H" => Some(VK_H),
        "i" | "I" => Some(VK_I),
        "j" | "J" => Some(VK_J),
        "k" | "K" => Some(VK_K),
        "l" | "L" => Some(VK_L),
        "m" | "M" => Some(VK_M),
        "n" | "N" => Some(VK_N),
        "o" | "O" => Some(VK_O),
        "p" | "P" => Some(VK_P),
        "q" | "Q" => Some(VK_Q),
        "r" | "R" => Some(VK_R),
        "s" | "S" => Some(VK_S),
        "t" | "T" => Some(VK_T),
        "u" | "U" => Some(VK_U),
        "v" | "V" => Some(VK_V),
        "w" | "W" => Some(VK_W),
        "x" | "X" => Some(VK_X),
        "y" | "Y" => Some(VK_Y),
        "z" | "Z" => Some(VK_Z),

        // Numbers
        "0" => Some(VK_0),
        "1" => Some(VK_1),
        "2" => Some(VK_2),
        "3" => Some(VK_3),
        "4" => Some(VK_4),
        "5" => Some(VK_5),
        "6" => Some(VK_6),
        "7" => Some(VK_7),
        "8" => Some(VK_8),
        "9" => Some(VK_9),

        // Symbols & OEM punctuation
        "grave" | "`" => Some(VK_OEM_3),
        "minus" | "-" => Some(VK_OEM_MINUS),
        "equal" | "=" => Some(VK_OEM_PLUS),
        "bracketleft" | "[" => Some(VK_OEM_4),
        "bracketright" | "]" => Some(VK_OEM_6),
        "backslash" | "\\" => Some(VK_OEM_5),
        "semicolon" | ";" => Some(VK_OEM_1),
        "apostrophe" | "'" => Some(VK_OEM_7),
        "comma" | "," => Some(VK_OEM_COMMA),
        "period" | "." => Some(VK_OEM_PERIOD),
        "slash" | "/" => Some(VK_OEM_2),

        // Whitespace and Editing
        "space" => Some(VK_SPACE),
        "Return" | "Enter" => Some(VK_RETURN),
        "BackSpace" => Some(VK_BACK),
        "Tab" => Some(VK_TAB),
        "Escape" | "Esc" => Some(VK_ESCAPE),
        "Delete" => Some(VK_DELETE),
        "Insert" => Some(VK_INSERT),

        // Modifiers
        "Caps_Lock" => Some(VK_CAPITAL),
        "Control_L" => Some(VK_LCONTROL),
        "Control_R" => Some(VK_RCONTROL),
        "Shift_L" => Some(VK_LSHIFT),
        "Shift_R" => Some(VK_RSHIFT),
        "Alt_L" => Some(VK_LMENU),
        "Alt_R" => Some(VK_RMENU),
        "Super_L" => Some(VK_LWIN),
        "Super_R" => Some(VK_RWIN),

        // Navigation
        "Left" => Some(VK_LEFT),
        "Right" => Some(VK_RIGHT),
        "Up" => Some(VK_UP),
        "Down" => Some(VK_DOWN),
        "Home" => Some(VK_HOME),
        "End" => Some(VK_END),
        "Prior" | "Page_Up" => Some(VK_PRIOR),
        "Next" | "Page_Down" => Some(VK_NEXT),

        // System
        "Print" => Some(VK_SNAPSHOT),
        "Scroll_Lock" => Some(VK_SCROLL),
        "Pause" => Some(VK_PAUSE),

        // Function Keys
        "F1" => Some(VK_F1),
        "F2" => Some(VK_F2),
        "F3" => Some(VK_F3),
        "F4" => Some(VK_F4),
        "F5" => Some(VK_F5),
        "F6" => Some(VK_F6),
        "F7" => Some(VK_F7),
        "F8" => Some(VK_F8),
        "F9" => Some(VK_F9),
        "F10" => Some(VK_F10),
        "F11" => Some(VK_F11),
        "F12" => Some(VK_F12),
        _ => None,
    }
}
