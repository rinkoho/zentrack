#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::fs;
use std::io::Cursor;
use std::path::{Path, PathBuf};
use std::process::Command;

const PAYLOAD: &[u8] = include_bytes!("../../../dist/windows/ZenTrack-Windows-x64-Portable.zip");

fn main() {
    #[cfg(target_os = "windows")]
    run_installer();

    #[cfg(not(target_os = "windows"))]
    println!("Este instalador está diseñado para ejecutarse en Microsoft Windows.");
}

#[cfg(target_os = "windows")]
fn run_installer() {
    use windows_sys::Win32::UI::WindowsAndMessaging::*;

    // Welcome dialog
    let title = to_wide("Instalador Oficial de ZenTrack");
    let welcome_msg = to_wide(
        "⚡ Bienvenido al instalador de ZenTrack Ultra-Low Latency.\n\n\
        ZenTrack configurará en tu equipo:\n\
        • Servidor nativo de 500Hz para Trackpad háptico y Teclado mecánico\n\
        • Conexión por cable USB con latencia cero (<0.2ms) con ADB integrado\n\
        • Soporte nativo para Mando virtual de Xbox 360\n\
        • Accesos directos en Escritorio y Menú Inicio\n\n\
        ¿Deseas instalar ZenTrack ahora?"
    );

    let res = unsafe {
        MessageBoxW(
            0,
            welcome_msg.as_ptr(),
            title.as_ptr(),
            MB_YESNO | MB_ICONINFORMATION | MB_DEFBUTTON1,
        )
    };

    if res != IDYES {
        return;
    }

    // Determine target directory: %LOCALAPPDATA%\Programs\ZenTrack
    let local_appdata = std::env::var("LOCALAPPDATA")
        .unwrap_or_else(|_| r"C:\Users\Default\AppData\Local".to_string());
    let install_dir = PathBuf::from(&local_appdata).join("Programs").join("ZenTrack");

    if let Err(e) = fs::create_dir_all(&install_dir) {
        let err_msg = to_wide(&format!("Error al crear la carpeta de instalación:\n{}", e));
        unsafe { MessageBoxW(0, err_msg.as_ptr(), title.as_ptr(), MB_OK | MB_ICONERROR) };
        return;
    }

    // Extract embedded ZIP payload
    if let Err(e) = extract_payload(&install_dir) {
        let err_msg = to_wide(&format!("Error al extraer los archivos de ZenTrack:\n{}", e));
        unsafe { MessageBoxW(0, err_msg.as_ptr(), title.as_ptr(), MB_OK | MB_ICONERROR) };
        return;
    }

    let zentrack_exe = install_dir.join("ZenTrack.exe");

    // Check if ViGEmBus is installed
    let vigem_installed = check_vigem_driver();
    if !vigem_installed {
        let vigem_setup = install_dir.join("drivers").join("ViGEmBus_Setup.exe");
        if vigem_setup.exists() {
            let vigem_prompt = to_wide(
                "🎮 Driver de Mando Xbox 360 (ViGEmBus):\n\n\
                Para que Windows detecte tu celular como un mando oficial de Xbox,\n\
                se iniciará el instalador del driver ViGEmBus.\n\n\
                Por favor, presiona 'Aceptar' y confirma los permisos de administrador."
            );
            unsafe {
                MessageBoxW(
                    0,
                    vigem_prompt.as_ptr(),
                    title.as_ptr(),
                    MB_OK | MB_ICONINFORMATION,
                )
            };

            // Run ViGEmBus installer with UAC elevation
            run_elevated_installer(&vigem_setup);
        }
    }

    // Create Shortcuts on Desktop and Start Menu
    create_shortcuts(&zentrack_exe);

    // Launch ZenTrack
    let _ = Command::new(&zentrack_exe).spawn();

    // Success dialog
    let success_msg = to_wide(
        "🎉 ¡ZenTrack se ha instalado con éxito!\n\n\
        El servidor se está ejecutando en segundo plano y abrirá\n\
        el Centro de Conexión en tu navegador web.\n\n\
        Encontrarás el acceso directo a ZenTrack en tu Escritorio."
    );
    unsafe {
        MessageBoxW(
            0,
            success_msg.as_ptr(),
            title.as_ptr(),
            MB_OK | MB_ICONINFORMATION,
        )
    };
}

#[cfg(target_os = "windows")]
fn extract_payload(dest_dir: &Path) -> Result<(), Box<dyn std::error::Error>> {
    let cursor = Cursor::new(PAYLOAD);
    let mut archive = zip::ZipArchive::new(cursor)?;

    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;
        let name = file.name().to_string();

        // Strip "ZenTrack-Portable/" prefix if present
        let clean_name = if let Some(stripped) = name.strip_prefix("ZenTrack-Portable/") {
            stripped
        } else {
            &name
        };

        if clean_name.is_empty() {
            continue;
        }

        let outpath = dest_dir.join(clean_name);

        if file.is_dir() {
            fs::create_dir_all(&outpath)?;
        } else {
            if let Some(p) = outpath.parent() {
                if !p.exists() {
                    fs::create_dir_all(p)?;
                }
            }
            let mut outfile = fs::File::create(&outpath)?;
            std::io::copy(&mut file, &mut outfile)?;
        }
    }

    Ok(())
}

#[cfg(target_os = "windows")]
fn check_vigem_driver() -> bool {
    use windows_sys::Win32::Foundation::{GENERIC_READ, GENERIC_WRITE, INVALID_HANDLE_VALUE};
    use windows_sys::Win32::Storage::FileSystem::*;

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

    if handle != INVALID_HANDLE_VALUE {
        unsafe { windows_sys::Win32::Foundation::CloseHandle(handle) };
        true
    } else {
        false
    }
}

#[cfg(target_os = "windows")]
fn run_elevated_installer(installer_path: &Path) {
    use windows_sys::Win32::UI::Shell::ShellExecuteW;
    use windows_sys::Win32::UI::WindowsAndMessaging::SW_SHOWNORMAL;

    let verb = to_wide("runas");
    let file = to_wide(&installer_path.to_string_lossy());
    let params = to_wide("");

    unsafe {
        ShellExecuteW(
            0,
            verb.as_ptr(),
            file.as_ptr(),
            params.as_ptr(),
            std::ptr::null(),
            SW_SHOWNORMAL as i32,
        );
    }
}

#[cfg(target_os = "windows")]
fn create_shortcuts(target_exe: &Path) {
    let exe_str = target_exe.to_string_lossy().replace('\\', "\\\\");

    // PowerShell script to create Desktop and Start Menu shortcuts
    let ps_script = format!(
        "$ws = New-Object -ComObject WScript.Shell; \
        $desktop = [System.Environment]::GetFolderPath('Desktop'); \
        $startMenu = [System.Environment]::GetFolderPath('Programs'); \
        $s1 = $ws.CreateShortcut(\"$desktop\\ZenTrack.lnk\"); \
        $s1.TargetPath = \"{0}\"; \
        $s1.Description = \"ZenTrack Ultra-Low Latency Server\"; \
        $s1.Save(); \
        $s2 = $ws.CreateShortcut(\"$startMenu\\ZenTrack.lnk\"); \
        $s2.TargetPath = \"{0}\"; \
        $s2.Description = \"ZenTrack Ultra-Low Latency Server\"; \
        $s2.Save();",
        exe_str
    );

    let _ = Command::new("powershell")
        .args(["-NoProfile", "-NonInteractive", "-Command", &ps_script])
        .spawn();
}

fn to_wide(s: &str) -> Vec<u16> {
    s.encode_utf16().chain(std::iter::once(0)).collect()
}
