use serde::{Deserialize, Serialize};
use std::fs;
use std::process::Command;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AdbDevice {
    pub serial: String,
    pub state: String, // "device", "unauthorized", "offline"
    pub model: String,
    pub product: String,
    pub is_authorized: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemHealth {
    pub os: String,
    pub distro_id: String,
    pub distro_name: String,
    pub package_manager: String,
    pub install_command: String,
    pub uinput_accessible: bool,
    pub uinput_message: String,
    pub gamepad_driver_installed: bool,
    pub gamepad_driver_message: String,
    pub adb_installed: bool,
    pub adb_path: Option<String>,
    pub adb_version: Option<String>,
    pub devices: Vec<AdbDevice>,
    pub reverse_active: bool,
}

/// Locate adb binary on the system
pub fn find_adb_binary() -> Option<String> {
    // 1. Try PATH
    if let Ok(output) = Command::new("adb").arg("version").output() {
        if output.status.success() {
            return Some("adb".to_string());
        }
    }

    // 2. Common Linux paths
    #[cfg(target_os = "linux")]
    {
        let candidates = [
            "/usr/bin/adb",
            "/bin/adb",
            "/usr/local/bin/adb",
        ];
        for path in &candidates {
            if fs::metadata(path).is_ok() {
                if let Ok(output) = Command::new(path).arg("version").output() {
                    if output.status.success() {
                        return Some(path.to_string());
                    }
                }
            }
        }

        // Check user Android SDK directory
        if let Ok(home) = std::env::var("HOME") {
            let sdk_adb = format!("{}/Android/Sdk/platform-tools/adb", home);
            if fs::metadata(&sdk_adb).is_ok() {
                if let Ok(output) = Command::new(&sdk_adb).arg("version").output() {
                    if output.status.success() {
                        return Some(sdk_adb);
                    }
                }
            }
        }
    }

    // 3. Common Windows paths
    #[cfg(target_os = "windows")]
    {
        let mut candidates = vec![
            "adb.exe".to_string(),
            ".\\adb.exe".to_string(),
            ".\\platform-tools\\adb.exe".to_string(),
            "C:\\platform-tools\\adb.exe".to_string(),
        ];

        if let Ok(local_appdata) = std::env::var("LOCALAPPDATA") {
            candidates.push(format!("{}\\Android\\Sdk\\platform-tools\\adb.exe", local_appdata));
        }
        if let Ok(user_profile) = std::env::var("USERPROFILE") {
            candidates.push(format!("{}\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe", user_profile));
            candidates.push(format!("{}\\scoop\\shims\\adb.exe", user_profile));
        }
        if let Ok(prog_files) = std::env::var("ProgramFiles") {
            candidates.push(format!("{}\\platform-tools\\adb.exe", prog_files));
        }

        for path in &candidates {
            if fs::metadata(path).is_ok() {
                if let Ok(output) = Command::new(path).arg("version").output() {
                    if output.status.success() {
                        return Some(path.clone());
                    }
                }
            }
        }
    }

    None
}

/// Extract short adb version string
pub fn get_adb_version(adb_bin: &str) -> Option<String> {
    if let Ok(output) = Command::new(adb_bin).arg("version").output() {
        if output.status.success() {
            let text = String::from_utf8_lossy(&output.stdout);
            for line in text.lines() {
                if line.contains("Android Debug Bridge version") {
                    return Some(line.trim().to_string());
                }
            }
            return Some("ADB OK".to_string());
        }
    }
    None
}

/// Query connected devices using adb devices -l
pub fn get_adb_devices(adb_bin: &str) -> Vec<AdbDevice> {
    let mut devices = Vec::new();
    if let Ok(output) = Command::new(adb_bin).args(["devices", "-l"]).output() {
        if output.status.success() {
            let text = String::from_utf8_lossy(&output.stdout);
            for line in text.lines() {
                let trimmed = line.trim();
                if trimmed.is_empty() || trimmed.starts_with("List of devices attached") || trimmed.starts_with('*') {
                    continue;
                }
                let tokens: Vec<&str> = trimmed.split_whitespace().collect();
                if tokens.len() >= 2 {
                    let serial = tokens[0].to_string();
                    let state = tokens[1].to_string();
                    let is_authorized = state == "device";

                    let mut model = serial.clone();
                    let mut product = String::new();

                    for token in tokens.iter().skip(2) {
                        if let Some(m) = token.strip_prefix("model:") {
                            model = m.replace('_', " ");
                        } else if let Some(p) = token.strip_prefix("product:") {
                            product = p.replace('_', " ");
                        }
                    }

                    devices.push(AdbDevice {
                        serial,
                        state,
                        model,
                        product,
                        is_authorized,
                    });
                }
            }
        }
    }
    devices
}

/// Check if reverse tunnel for port exists
pub fn is_adb_reverse_active(adb_bin: &str, port: u16) -> bool {
    if let Ok(output) = Command::new(adb_bin).args(["reverse", "--list"]).output() {
        if output.status.success() {
            let text = String::from_utf8_lossy(&output.stdout);
            let port_str = format!("tcp:{}", port);
            return text.contains(&port_str);
        }
    }
    false
}

/// Trigger adb reverse for port
pub fn run_adb_reverse(adb_bin: &str, port: u16) -> Result<(), String> {
    let port_str = format!("tcp:{}", port);
    match Command::new(adb_bin).args(["reverse", &port_str, &port_str]).output() {
        Ok(output) => {
            if output.status.success() {
                Ok(())
            } else {
                Err(String::from_utf8_lossy(&output.stderr).to_string())
            }
        }
        Err(e) => Err(e.to_string()),
    }
}

/// Detect Linux distribution and recommend installation commands
pub fn get_distro_info() -> (String, String, String, String) {
    let os = std::env::consts::OS.to_string();
    if os == "windows" {
        return (
            "windows".to_string(),
            "Windows".to_string(),
            "winget".to_string(),
            "winget install Google.PlatformTools".to_string(),
        );
    }
    if os != "linux" {
        return (
            os.clone(),
            os,
            "standalone".to_string(),
            "".to_string(),
        );
    }

    let mut distro_id = "linux".to_string();
    let mut distro_name = "GNU/Linux".to_string();

    if let Ok(content) = fs::read_to_string("/etc/os-release") {
        for line in content.lines() {
            if let Some(id) = line.strip_prefix("ID=") {
                distro_id = id.trim_matches('"').to_lowercase();
            } else if let Some(name) = line.strip_prefix("PRETTY_NAME=") {
                distro_name = name.trim_matches('"').to_string();
            }
        }
    }

    let (pkg_manager, install_cmd) = match distro_id.as_str() {
        "arch" | "manjaro" | "endeavouros" | "garuda" => (
            "pacman".to_string(),
            "sudo pacman -S --needed android-tools".to_string(),
        ),
        "ubuntu" | "debian" | "linuxmint" | "pop" | "elementary" | "kali" => (
            "apt".to_string(),
            "sudo apt update && sudo apt install -y adb".to_string(),
        ),
        "fedora" | "rhel" | "centos" | "rocky" | "almalinux" => (
            "dnf".to_string(),
            "sudo dnf install -y android-tools".to_string(),
        ),
        "opensuse" | "opensuse-tumbleweed" | "opensuse-leap" => (
            "zypper".to_string(),
            "sudo zypper install -y android-tools".to_string(),
        ),
        _ => (
            "generic".to_string(),
            "Instala 'android-tools' o 'adb' usando el gestor de paquetes de tu distribución".to_string(),
        ),
    };

    (distro_id, distro_name, pkg_manager, install_cmd)
}

/// Check uinput driver access on Linux
pub fn check_uinput_accessibility() -> (bool, String) {
    #[cfg(target_os = "linux")]
    {
        if !std::path::Path::new("/dev/uinput").exists() {
            return (
                false,
                "El dispositivo de kernel /dev/uinput no existe. ¿Está cargado el módulo 'uinput'?".to_string(),
            );
        }

        match fs::OpenOptions::new().write(true).open("/dev/uinput") {
            Ok(_) => (true, "Driver de kernel /dev/uinput accesible y operativo".to_string()),
            Err(e) => (
                false,
                format!(
                    "Permiso denegado al acceder a /dev/uinput ({e}). Asegúrate de pertenecer al grupo 'input' o tener la regla udev instalada.",
                ),
            ),
        }
    }

    #[cfg(target_os = "windows")]
    {
        (true, "Driver nativo SendInput de Windows activo".to_string())
    }

    #[cfg(not(any(target_os = "linux", target_os = "windows")))]
    {
        (true, "Driver de plataforma estándar".to_string())
    }
}

/// Check Xbox Gamepad driver (ViGEmBus on Windows, /dev/uinput on Linux)
pub fn check_gamepad_driver() -> (bool, String) {
    #[cfg(target_os = "linux")]
    {
        (true, "Driver de kernel Xbox 360 operativo (/dev/uinput)".to_string())
    }

    #[cfg(target_os = "windows")]
    {
        if vigem_client::Client::connect().is_ok() {
            (true, "Operativo (ViGEmBus Xbox 360 conectado)".to_string())
        } else {
            (false, "No instalado (Requiere ViGEmBus para Mando Xbox)".to_string())
        }
    }

    #[cfg(not(any(target_os = "linux", target_os = "windows")))]
    {
        (false, "No soportado en este SO".to_string())
    }
}

/// Aggregate full system diagnostic
pub fn inspect_system(port: u16) -> SystemHealth {
    let (distro_id, distro_name, package_manager, install_command) = get_distro_info();
    let (uinput_accessible, uinput_message) = check_uinput_accessibility();
    let (gamepad_driver_installed, gamepad_driver_message) = check_gamepad_driver();
    let adb_path = find_adb_binary();
    let adb_installed = adb_path.is_some();

    let (adb_version, devices, reverse_active) = if let Some(ref bin) = adb_path {
        let ver = get_adb_version(bin);
        let devs = get_adb_devices(bin);
        let rev = is_adb_reverse_active(bin, port);
        (ver, devs, rev)
    } else {
        (None, Vec::new(), false)
    };

    SystemHealth {
        os: std::env::consts::OS.to_string(),
        distro_id,
        distro_name,
        package_manager,
        install_command,
        uinput_accessible,
        uinput_message,
        gamepad_driver_installed,
        gamepad_driver_message,
        adb_installed,
        adb_path,
        adb_version,
        devices,
        reverse_active,
    }
}
