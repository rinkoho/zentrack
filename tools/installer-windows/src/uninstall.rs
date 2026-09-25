#![allow(dead_code)]

use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::thread;
use std::time::Duration;

use crate::common::{
    get_desktop_dir, get_install_directory, get_programs_dir, get_startup_dir,
    init_common_controls, terminate_process_by_name, to_wide,
};
use crate::ui::{
    show_uninstall_confirm_dialog, show_uninstall_finished_dialog,
};

pub fn run_uninstaller() {
    init_common_controls();

    let args: Vec<String> = std::env::args().collect();
    let is_temp = args.iter().any(|a| a == "--temp-uninstall");

    // If --temp-uninstall passed custom path, parse it; otherwise use default install dir
    let mut install_dir = get_install_directory();
    if let Some(pos) = args.iter().position(|a| a == "--temp-uninstall") {
        if pos + 1 < args.len() {
            let custom_path = PathBuf::from(&args[pos + 1]);
            if custom_path.exists() {
                install_dir = custom_path;
            }
        }
    }

    let current_exe = match std::env::current_exe() {
        Ok(p) => p,
        Err(_) => PathBuf::from(""),
    };

    // If running from inside the install directory, copy ourselves to %TEMP%
    // and re-launch from there so we do not lock files/folder while deleting them.
    let in_install_dir = if install_dir.as_os_str().is_empty() {
        false
    } else {
        current_exe.starts_with(&install_dir)
    };

    if in_install_dir && !is_temp {
        // Ask confirmation BEFORE copying to temp
        if !show_uninstall_confirm_dialog() {
            return;
        }

        let temp_exe = std::env::temp_dir().join("ZenTrack-Uninstall-Temp.exe");
        if let Ok(_) = fs::copy(&current_exe, &temp_exe) {
            let _ = Command::new(&temp_exe)
                .arg("--temp-uninstall")
                .arg(&install_dir)
                .spawn();
            std::process::exit(0);
        }
    }

    // If running directly outside install dir without --temp-uninstall, ask confirmation now
    if !is_temp && !in_install_dir {
        if !show_uninstall_confirm_dialog() {
            return;
        }
    }

    // Wait a brief moment to allow original process to exit cleanly
    thread::sleep(Duration::from_millis(500));

    // 1. Terminate running ZenTrack and ADB instances
    terminate_process_by_name("ZenTrack.exe");
    terminate_process_by_name("adb.exe");
    let mut cmd1 = Command::new("taskkill");
    let mut cmd2 = Command::new("taskkill");
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        cmd1.creation_flags(0x08000000);
        cmd2.creation_flags(0x08000000);
    }
    let _ = cmd1.args(["/F", "/IM", "ZenTrack.exe"]).output();
    let _ = cmd2.args(["/F", "/IM", "adb.exe"]).output();
    thread::sleep(Duration::from_millis(300));

    // 2. Remove Shortcuts from Desktop, Start Menu and Startup
    remove_shortcuts();

    // 3. Remove Windows Registry Uninstall & Autostart Entries
    unregister_uninstall_entry();
    unregister_autostart_entry();

    // 4. Remove Windows Firewall exception
    let mut fw_cmd = Command::new("netsh");
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        fw_cmd.creation_flags(0x08000000);
    }
    let _ = fw_cmd.args([
        "advfirewall", "firewall", "delete", "rule",
        "name=ZenTrack Server"
    ]).output();

    // 4. Remove all files from %LOCALAPPDATA%\Programs\ZenTrack
    if install_dir.exists() {
        for _ in 0..5 {
            if fs::remove_dir_all(&install_dir).is_ok() {
                break;
            }
            thread::sleep(Duration::from_millis(200));
        }
    }

    // 5. Show completion dialog
    show_uninstall_finished_dialog();

    // 6. If running as temporary uninstaller, schedule self-deletion
    if is_temp {
        schedule_self_delete(&current_exe);
    }
}

pub fn remove_shortcuts() {
    if let Some(desktop) = get_desktop_dir() {
        let lnk = desktop.join("ZenTrack.lnk");
        let _ = fs::remove_file(lnk);
    }
    if let Some(programs) = get_programs_dir() {
        let lnk = programs.join("ZenTrack.lnk");
        let _ = fs::remove_file(lnk);
    }
    if let Some(startup) = get_startup_dir() {
        let lnk = startup.join("ZenTrack.lnk");
        let _ = fs::remove_file(lnk);
    }
}

pub fn unregister_uninstall_entry() {
    use windows_sys::Win32::System::Registry::*;
    let subkey = to_wide(r"Software\Microsoft\Windows\CurrentVersion\Uninstall\ZenTrack");
    unsafe {
        RegDeleteKeyW(HKEY_CURRENT_USER, subkey.as_ptr());
    }
}

pub fn unregister_autostart_entry() {
    use windows_sys::Win32::System::Registry::*;
    let subkey = to_wide(r"Software\Microsoft\Windows\CurrentVersion\Run");
    let mut hkey: HKEY = 0;
    let res = unsafe {
        RegOpenKeyExW(
            HKEY_CURRENT_USER,
            subkey.as_ptr(),
            0,
            KEY_SET_VALUE,
            &mut hkey,
        )
    };
    if res == 0 {
        let val_w = to_wide("ZenTrack");
        unsafe {
            RegDeleteValueW(hkey, val_w.as_ptr());
            RegCloseKey(hkey);
        }
    }
}

fn schedule_self_delete(exe_path: &Path) {
    let path_str = exe_path.to_string_lossy();
    let cmd = format!("ping 127.0.0.1 -n 2 > nul & del /F /Q \"{}\"", path_str);
    let mut c = Command::new("cmd");
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        c.creation_flags(0x08000000);
    }
    let _ = c
        .args(["/C", &cmd])
        .spawn();
}
