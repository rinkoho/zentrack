#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod common;
mod ui;
mod install;
mod uninstall;

fn main() {
    #[cfg(target_os = "windows")]
    {
        let exe_name = std::env::current_exe()
            .ok()
            .and_then(|p| p.file_name().map(|n| n.to_string_lossy().to_lowercase()))
            .unwrap_or_default();
        let args: Vec<String> = std::env::args().collect();
        let is_uninstall = exe_name.contains("uninstall")
            || args.iter().any(|a| {
                let lower = a.to_lowercase();
                lower == "--uninstall" || lower == "/uninstall" || lower == "-uninstall" || lower == "--temp-uninstall"
            });

        if is_uninstall {
            uninstall::run_uninstaller();
        } else {
            install::run_installer();
        }
    }

    #[cfg(not(target_os = "windows"))]
    println!("Este instalador está diseñado para ejecutarse en Microsoft Windows.");
}
