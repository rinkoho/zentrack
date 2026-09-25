#![allow(dead_code)]

use std::path::PathBuf;
use windows_sys::Win32::Foundation::*;
use windows_sys::Win32::Storage::FileSystem::*;
use windows_sys::Win32::UI::Controls::*;
use windows_sys::Win32::UI::Shell::*;
use windows_sys::Win32::System::Diagnostics::ToolHelp::*;
use windows_sys::Win32::System::Threading::*;

pub fn to_wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}

pub fn init_common_controls() {
    let icex = INITCOMMONCONTROLSEX {
        dwSize: std::mem::size_of::<INITCOMMONCONTROLSEX>() as u32,
        dwICC: ICC_STANDARD_CLASSES | ICC_PROGRESS_CLASS | ICC_WIN95_CLASSES,
    };
    unsafe {
        InitCommonControlsEx(&icex);
    }
}

pub fn get_shell_folder(csidl: i32) -> Option<PathBuf> {
    let mut buf = [0u16; 260];
    let res = unsafe {
        SHGetFolderPathW(0, csidl, 0, 0, buf.as_mut_ptr())
    };
    if res == 0 {
        let len = buf.iter().position(|&c| c == 0).unwrap_or(buf.len());
        let s = String::from_utf16_lossy(&buf[..len]);
        Some(PathBuf::from(s))
    } else {
        None
    }
}

pub fn get_install_directory() -> PathBuf {
    let local_appdata = get_shell_folder(CSIDL_LOCAL_APPDATA as i32)
        .or_else(|| std::env::var("LOCALAPPDATA").ok().map(PathBuf::from))
        .unwrap_or_else(|| PathBuf::from(r"C:\Users\Default\AppData\Local"));
    local_appdata.join("Programs").join("ZenTrack")
}

pub fn get_desktop_dir() -> Option<PathBuf> {
    get_shell_folder(CSIDL_DESKTOPDIRECTORY as i32)
        .or_else(|| std::env::var("USERPROFILE").ok().map(|p| PathBuf::from(p).join("Desktop")))
}

pub fn get_programs_dir() -> Option<PathBuf> {
    get_shell_folder(CSIDL_PROGRAMS as i32)
        .or_else(|| std::env::var("APPDATA").ok().map(|p| PathBuf::from(p).join("Microsoft").join("Windows").join("Start Menu").join("Programs")))
}

pub fn check_vigem_driver() -> bool {
    let dev_name = to_wide(r"\\.\ViGEmBus");
    let handle = unsafe {
        CreateFileW(
            dev_name.as_ptr(),
            GENERIC_READ | GENERIC_WRITE,
            FILE_SHARE_READ | FILE_SHARE_WRITE,
            std::ptr::null(),
            OPEN_EXISTING,
            FILE_ATTRIBUTE_NORMAL,
            0,
        )
    };

    if handle != INVALID_HANDLE_VALUE && handle != 0 {
        unsafe { CloseHandle(handle) };
        true
    } else {
        false
    }
}

pub fn terminate_process_by_name(process_name: &str) {
    let snapshot = unsafe { CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0) };
    if snapshot == INVALID_HANDLE_VALUE || snapshot == 0 {
        return;
    }

    let mut entry: PROCESSENTRY32W = unsafe { std::mem::zeroed() };
    entry.dwSize = std::mem::size_of::<PROCESSENTRY32W>() as u32;

    if unsafe { Process32FirstW(snapshot, &mut entry) } != 0 {
        loop {
            let name_len = entry.szExeFile.iter().position(|&c| c == 0).unwrap_or(entry.szExeFile.len());
            let name = String::from_utf16_lossy(&entry.szExeFile[..name_len]);

            if name.eq_ignore_ascii_case(process_name) {
                let pid = entry.th32ProcessID;
                let h_proc = unsafe { OpenProcess(PROCESS_TERMINATE, 0, pid) };
                if h_proc != 0 && h_proc != INVALID_HANDLE_VALUE {
                    unsafe {
                        TerminateProcess(h_proc, 0);
                        CloseHandle(h_proc);
                    }
                }
            }

            if unsafe { Process32NextW(snapshot, &mut entry) } == 0 {
                break;
            }
        }
    }
    unsafe { CloseHandle(snapshot) };
}
