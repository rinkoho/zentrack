#![allow(dead_code)]

use std::fs;
use std::io::Cursor;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::atomic::Ordering;
use std::sync::Arc;
use std::thread;

use crate::common::{
    check_vigem_driver, get_desktop_dir, get_install_directory, get_programs_dir,
    init_common_controls, to_wide,
};
use crate::ui::{
    show_error_dialog, show_installed_dialog, show_progress_dialog, show_vigem_prompt,
    show_welcome_dialog, ProgressState,
};

pub const PAYLOAD: &[u8] = include_bytes!("../../../dist/windows/ZenTrack-Windows-x64-Portable.zip");

pub fn run_installer() {
    init_common_controls();

    // 1. Welcome dialog
    if !show_welcome_dialog() {
        return;
    }

    // 2. Target installation directory: %LOCALAPPDATA%\Programs\ZenTrack
    let install_dir = get_install_directory();
    if let Err(e) = fs::create_dir_all(&install_dir) {
        show_error_dialog(
            "Error de Instalación",
            "No se pudo crear el directorio de destino",
            &format!("Ruta: {}\nDetalles del error: {}", install_dir.display(), e),
        );
        return;
    }

    // 3. Extract payload with progress dialog
    let state = Arc::new(ProgressState::new());
    let worker_state = Arc::clone(&state);
    let worker_dest = install_dir.clone();

    let worker_handle = thread::spawn(move || {
        extract_payload_worker(worker_dest, worker_state);
    });

    // Run TaskDialog progress on UI thread
    show_progress_dialog(&state);

    let _ = worker_handle.join();

    if let Some(err) = state.error.lock().unwrap().as_ref() {
        show_error_dialog(
            "Error de Extracción",
            "Ocurrió un error al descomprimir los archivos de ZenTrack",
            err,
        );
        return;
    }

    let zentrack_exe = install_dir.join("ZenTrack.exe");

    // 4. ViGEmBus Driver Detection & Synchronous Installation
    let vigem_installed = check_vigem_driver();
    if !vigem_installed {
        let vigem_setup = install_dir.join("drivers").join("ViGEmBus_Setup.exe");
        if vigem_setup.exists() {
            let should_install = show_vigem_prompt();
            if should_install {
                match install_vigem_driver_sync(&vigem_setup) {
                    Ok(true) => {
                        // Driver installed and confirmed active in kernel
                    }
                    Ok(false) => {
                        show_error_dialog(
                            "Aviso del Controlador ViGEmBus",
                            "El controlador ViGEmBus no pudo ser verificado en el kernel",
                            "El instalador de ViGEmBus finalizó, pero el dispositivo virtual no respondió a tiempo.\n\
                            Puedes instalarlo manualmente ejecutando:\n\
                            'Instalar_Driver_Mando_Xbox.bat' en la carpeta de ZenTrack.",
                        );
                    }
                    Err(e) => {
                        show_error_dialog(
                            "Aviso del Controlador ViGEmBus",
                            "No se completó la instalación del controlador ViGEmBus",
                            &format!("{}\n\nPuedes instalarlo manualmente más adelante.", e),
                        );
                    }
                }
            }
        }
    }

    // 5. Create Desktop and Start Menu Shortcuts
    create_shortcuts(&zentrack_exe);

    // 6. Ensure Uninstall.exe exists in the installation directory
    let uninst_path = install_dir.join("Uninstall.exe");
    if !uninst_path.exists() {
        if let Ok(current_exe) = std::env::current_exe() {
            let _ = fs::copy(&current_exe, &uninst_path);
        }
    }

    // 7. Register in Windows Registry (Control Panel / Settings)
    if let Err(e) = register_uninstall_entry(&install_dir) {
        // Non-fatal, but logged
        eprintln!("No se pudo registrar la entrada de desinstalación: {}", e);
    }

    // 8. Launch ZenTrack Server
    let _ = Command::new(&zentrack_exe).spawn();

    // 9. Show Success Dialog
    show_installed_dialog();
}

fn extract_payload_worker(dest_dir: PathBuf, state: Arc<ProgressState>) {
    use windows_sys::Win32::UI::Controls::TDM_CLICK_BUTTON;
    use windows_sys::Win32::UI::WindowsAndMessaging::{PostMessageW, IDOK};

    let cursor = Cursor::new(PAYLOAD);
    let mut archive = match zip::ZipArchive::new(cursor) {
        Ok(a) => a,
        Err(e) => {
            *state.error.lock().unwrap() = Some(format!("Error al leer el archivo ZIP embebido: {}", e));
            state.finished.store(true, Ordering::SeqCst);
            let h = state.hwnd.load(Ordering::SeqCst);
            if h != 0 {
                unsafe { PostMessageW(h, TDM_CLICK_BUTTON as u32, IDOK as usize, 0) };
            }
            return;
        }
    };

    let total = archive.len();
    for i in 0..total {
        let mut file = match archive.by_index(i) {
            Ok(f) => f,
            Err(e) => {
                *state.error.lock().unwrap() = Some(format!("Error al descomprimir componente #{}: {}", i, e));
                break;
            }
        };

        let raw_name = file.name().to_string();
        let clean_name = if let Some(stripped) = raw_name.strip_prefix("ZenTrack-Portable/") {
            stripped
        } else {
            &raw_name
        };

        if clean_name.is_empty() {
            continue;
        }

        let outpath = dest_dir.join(clean_name);

        if let Ok(mut text) = state.current_text.lock() {
            *text = format!("Extrayendo ({}/{}): {}", i + 1, total, clean_name);
        }
        let pct = ((i + 1) * 100) / total.max(1);
        state.current_pct.store(pct, Ordering::Relaxed);

        if file.is_dir() {
            let _ = fs::create_dir_all(&outpath);
        } else {
            if let Some(p) = outpath.parent() {
                let _ = fs::create_dir_all(p);
            }
            match fs::File::create(&outpath) {
                Ok(mut outfile) => {
                    if let Err(e) = std::io::copy(&mut file, &mut outfile) {
                        *state.error.lock().unwrap() = Some(format!("Error escribiendo {}: {}", outpath.display(), e));
                        break;
                    }
                }
                Err(e) => {
                    *state.error.lock().unwrap() = Some(format!("Error creando {}: {}", outpath.display(), e));
                    break;
                }
            }
        }
    }

    state.finished.store(true, Ordering::SeqCst);
    let h = state.hwnd.load(Ordering::SeqCst);
    if h != 0 {
        unsafe { PostMessageW(h, TDM_CLICK_BUTTON as u32, IDOK as usize, 0) };
    }
}

pub fn install_vigem_driver_sync(installer_path: &Path) -> Result<bool, String> {
    use windows_sys::Win32::Foundation::{CloseHandle, INVALID_HANDLE_VALUE};
    use windows_sys::Win32::System::Threading::{GetExitCodeProcess, WaitForSingleObject, INFINITE};
    use windows_sys::Win32::UI::Shell::{ShellExecuteExW, SEE_MASK_NOCLOSEPROCESS, SHELLEXECUTEINFOW};
    use windows_sys::Win32::UI::WindowsAndMessaging::SW_SHOWNORMAL;

    if !installer_path.exists() {
        return Err(format!("No se encontró el instalador del driver en {:?}", installer_path));
    }

    let verb = to_wide("runas");
    let file = to_wide(&installer_path.to_string_lossy());
    let params = to_wide("");

    let mut sei: SHELLEXECUTEINFOW = unsafe { std::mem::zeroed() };
    sei.cbSize = std::mem::size_of::<SHELLEXECUTEINFOW>() as u32;
    sei.fMask = SEE_MASK_NOCLOSEPROCESS;
    sei.hwnd = 0;
    sei.lpVerb = verb.as_ptr();
    sei.lpFile = file.as_ptr();
    sei.lpParameters = params.as_ptr();
    sei.nShow = SW_SHOWNORMAL as i32;

    let success = unsafe { ShellExecuteExW(&mut sei) };
    if success == 0 {
        return Err("El usuario canceló la elevación de permisos (UAC) o el driver no pudo ser ejecutado.".to_string());
    }

    if sei.hProcess != 0 && sei.hProcess != INVALID_HANDLE_VALUE {
        unsafe {
            // CRÍTICO: Esperar a que el proceso termine determinísticamente
            WaitForSingleObject(sei.hProcess, INFINITE);
            let mut exit_code: u32 = 0;
            GetExitCodeProcess(sei.hProcess, &mut exit_code);
            CloseHandle(sei.hProcess);
        }
    }

    // Re-verificar con CreateFileW(r"\\.\ViGEmBus") para garantizar que el kernel montó el dispositivo
    for _ in 0..10 {
        if check_vigem_driver() {
            return Ok(true);
        }
        unsafe { windows_sys::Win32::System::Threading::Sleep(500) };
    }

    Ok(check_vigem_driver())
}

pub fn create_shortcuts(zentrack_exe: &Path) {
    let desktop_opt = get_desktop_dir();
    let programs_opt = get_programs_dir();
    let install_dir = zentrack_exe.parent().unwrap_or(Path::new(""));

    let exe_str = zentrack_exe.to_string_lossy().replace('\'', "''");
    let dir_str = install_dir.to_string_lossy().replace('\'', "''");

    let mut ps_script = String::from("$ws = New-Object -ComObject WScript.Shell;\n");

    if let Some(desktop) = desktop_opt {
        let lnk_path = desktop.join("ZenTrack.lnk");
        let lnk_str = lnk_path.to_string_lossy().replace('\'', "''");
        ps_script.push_str(&format!(
            "$s1 = $ws.CreateShortcut('{0}'); \
            $s1.TargetPath = '{1}'; \
            $s1.WorkingDirectory = '{2}'; \
            $s1.IconLocation = '{1},0'; \
            $s1.Description = 'ZenTrack Ultra-Low Latency Server'; \
            $s1.Save();\n",
            lnk_str, exe_str, dir_str
        ));
    }

    if let Some(programs) = programs_opt {
        let lnk_path = programs.join("ZenTrack.lnk");
        let lnk_str = lnk_path.to_string_lossy().replace('\'', "''");
        ps_script.push_str(&format!(
            "$s2 = $ws.CreateShortcut('{0}'); \
            $s2.TargetPath = '{1}'; \
            $s2.WorkingDirectory = '{2}'; \
            $s2.IconLocation = '{1},0'; \
            $s2.Description = 'ZenTrack Ultra-Low Latency Server'; \
            $s2.Save();\n",
            lnk_str, exe_str, dir_str
        ));
    }

    let _ = Command::new("powershell")
        .args(["-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", &ps_script])
        .status();
}

pub fn register_uninstall_entry(install_dir: &Path) -> Result<(), String> {
    use windows_sys::Win32::System::Registry::*;

    let subkey = to_wide(r"Software\Microsoft\Windows\CurrentVersion\Uninstall\ZenTrack");
    let mut hkey: HKEY = 0;
    let res = unsafe {
        RegCreateKeyExW(
            HKEY_CURRENT_USER,
            subkey.as_ptr(),
            0,
            std::ptr::null(),
            REG_OPTION_NON_VOLATILE,
            KEY_ALL_ACCESS,
            std::ptr::null(),
            &mut hkey,
            std::ptr::null_mut(),
        )
    };

    if res != 0 {
        return Err(format!("RegCreateKeyExW error {}", res));
    }

    let exe_path = install_dir.join("ZenTrack.exe");
    let uninst_path = install_dir.join("Uninstall.exe");
    let icon_str = format!("{},0", exe_path.display());
    let uninst_str = format!("\"{}\"", uninst_path.display());
    let install_dir_str = install_dir.display().to_string();

    set_reg_sz(hkey, "DisplayName", "ZenTrack");
    set_reg_sz(hkey, "DisplayVersion", "1.0.0");
    set_reg_sz(hkey, "Publisher", "ZenTrack Team");
    set_reg_sz(hkey, "InstallLocation", &install_dir_str);
    set_reg_sz(hkey, "DisplayIcon", &icon_str);
    set_reg_sz(hkey, "UninstallString", &uninst_str);
    set_reg_dword(hkey, "EstimatedSize", 35000);
    set_reg_dword(hkey, "NoModify", 1);
    set_reg_dword(hkey, "NoRepair", 1);

    unsafe { RegCloseKey(hkey) };
    Ok(())
}

fn set_reg_sz(hkey: windows_sys::Win32::System::Registry::HKEY, name: &str, value: &str) {
    use windows_sys::Win32::System::Registry::*;
    let name_w = to_wide(name);
    let val_w = to_wide(value);
    unsafe {
        RegSetValueExW(
            hkey,
            name_w.as_ptr(),
            0,
            REG_SZ,
            val_w.as_ptr() as *const u8,
            (val_w.len() * 2) as u32,
        );
    }
}

fn set_reg_dword(hkey: windows_sys::Win32::System::Registry::HKEY, name: &str, value: u32) {
    use windows_sys::Win32::System::Registry::*;
    let name_w = to_wide(name);
    unsafe {
        RegSetValueExW(
            hkey,
            name_w.as_ptr(),
            0,
            REG_DWORD,
            &value as *const u32 as *const u8,
            std::mem::size_of::<u32>() as u32,
        );
    }
}
