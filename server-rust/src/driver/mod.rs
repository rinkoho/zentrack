#[cfg(target_os = "linux")]
pub mod linux;
#[cfg(target_os = "windows")]
pub mod windows;

pub trait InputDriver: Send + Sync {
    fn mouse_move(&mut self, dx: i32, dy: i32);
    fn mouse_down(&mut self, button: u8);
    fn mouse_up(&mut self, button: u8);
    fn mouse_click(&mut self, button: u8, double: bool);
    fn smooth_scroll(&mut self, dx: f64, dy: f64);
    fn scroll_discrete(&mut self, direction: &str, steps: i32);
    fn key_down(&mut self, key: &str);
    fn key_up(&mut self, key: &str);
    fn key_click(&mut self, key: &str);
    fn release_all_modifiers(&mut self);
    fn set_gamepad_mode(&mut self, enabled: bool);
    fn gamepad_btn(&mut self, name: &str, value: i32);
    fn gamepad_axis(&mut self, name: &str, value: i32);
}

pub fn create_driver() -> Result<Box<dyn InputDriver>, String> {
    #[cfg(target_os = "linux")]
    {
        linux::LinuxDriver::new().map(|d| Box::new(d) as Box<dyn InputDriver>)
    }

    #[cfg(target_os = "windows")]
    {
        windows::WindowsDriver::new().map(|d| Box::new(d) as Box<dyn InputDriver>)
    }

    #[cfg(not(any(target_os = "linux", target_os = "windows")))]
    {
        Err("Unsupported operating system for ZenTrack native driver".to_string())
    }
}
