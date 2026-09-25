#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

mod common;
mod ui;
mod uninstall;

fn main() {
    #[cfg(target_os = "windows")]
    uninstall::run_uninstaller();

    #[cfg(not(target_os = "windows"))]
    println!("Este desinstalador está diseñado para ejecutarse en Microsoft Windows.");
}
