use super::InputDriver;

#[cfg(target_os = "windows")]
use windows_sys::Win32::UI::Input::KeyboardAndMouse::*;

pub struct WindowsDriver {
    #[cfg(target_os = "windows")]
    accum_scroll_y: f64,
    #[cfg(target_os = "windows")]
    accum_scroll_x: f64,
}

impl WindowsDriver {
    pub fn new() -> Result<Self, String> {
        Ok(Self {
            #[cfg(target_os = "windows")]
            accum_scroll_y: 0.0,
            #[cfg(target_os = "windows")]
            accum_scroll_x: 0.0,
        })
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
            SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
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
            SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
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
            SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
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
        // WHEEL_DELTA is 120
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
                SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
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
                SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
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
        if let Some(vk) = map_windows_key(key) {
            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_KEYBOARD;
                input.Anonymous.ki = KEYBDINPUT {
                    wVk: vk,
                    wScan: 0,
                    dwFlags: 0,
                    time: 0,
                    dwExtraInfo: 0,
                };
                SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
            }
        }
    }

    fn key_up(&mut self, key: &str) {
        if let Some(vk) = map_windows_key(key) {
            unsafe {
                let mut input: INPUT = std::mem::zeroed();
                input.r#type = INPUT_KEYBOARD;
                input.Anonymous.ki = KEYBDINPUT {
                    wVk: vk,
                    wScan: 0,
                    dwFlags: KEYEVENTF_KEYUP,
                    time: 0,
                    dwExtraInfo: 0,
                };
                SendInput(1, &input, std::mem::size_of::<INPUT>() as i32);
            }
        }
    }

    fn key_click(&mut self, key: &str) {
        self.key_down(key);
        self.key_up(key);
    }

    fn release_all_modifiers(&mut self) {
        for key in &["Control_L", "Control_R", "Alt_L", "Alt_R", "Shift_L", "Shift_R", "Super_L", "Super_R"] {
            self.key_up(key);
        }
    }

    fn set_gamepad_mode(&mut self, _enabled: bool) {
        // Gamepad on Windows
    }

    fn gamepad_btn(&mut self, _name: &str, _value: i32) {}
    fn gamepad_axis(&mut self, _name: &str, _value: i32) {}
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
fn map_windows_key(key: &str) -> Option<u16> {
    match key {
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
        "space" => Some(VK_SPACE),
        "Return" => Some(VK_RETURN),
        "BackSpace" => Some(VK_BACK),
        "Tab" => Some(VK_TAB),
        "Escape" => Some(VK_ESCAPE),
        "Control_L" => Some(VK_LCONTROL),
        "Control_R" => Some(VK_RCONTROL),
        "Shift_L" => Some(VK_LSHIFT),
        "Shift_R" => Some(VK_RSHIFT),
        "Alt_L" => Some(VK_LMENU),
        "Alt_R" => Some(VK_RMENU),
        "Super_L" => Some(VK_LWIN),
        "Super_R" => Some(VK_RWIN),
        "Left" => Some(VK_LEFT),
        "Right" => Some(VK_RIGHT),
        "Up" => Some(VK_UP),
        "Down" => Some(VK_DOWN),
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
