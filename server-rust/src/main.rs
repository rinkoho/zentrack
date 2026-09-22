mod config;
mod crypto;
mod discovery;
mod driver;
mod health;
mod protocol;
mod web;

use config::AppConfig;
use crypto::CryptoEngine;
use driver::create_driver;
use web::{create_router, AppState};

use qrcode::render::unicode::Dense1x2;
use qrcode::QrCode;
use std::net::SocketAddr;
use std::path::PathBuf;
use std::sync::atomic::AtomicUsize;
use std::sync::Arc;
use tokio::sync::Mutex;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    #[cfg(target_os = "windows")]
    unsafe {
        use windows_sys::Win32::System::Console::*;
        let handle = GetStdHandle(STD_INPUT_HANDLE);
        let mut mode = 0;
        if GetConsoleMode(handle, &mut mode) != 0 {
            // Disable ENABLE_QUICK_EDIT_MODE (0x0040) so clicking the terminal never pauses the process
            let new_mode = (mode & !ENABLE_QUICK_EDIT_MODE) | ENABLE_EXTENDED_FLAGS;
            SetConsoleMode(handle, new_mode);
        }
    }

    // Determine config path (prefer current dir or parent dir if running from server-rust)
    let config_path = if PathBuf::from("config.json").exists() {
        PathBuf::from("config.json")
    } else if PathBuf::from("../config.json").exists() {
        PathBuf::from("../config.json")
    } else {
        PathBuf::from("config.json")
    };

    let cfg = AppConfig::load_or_create(&config_path);
    let port = cfg.port;
    let token = cfg.token.clone();

    // Initialize native platform input driver
    let driver = match create_driver() {
        Ok(d) => Arc::new(Mutex::new(d)),
        Err(e) => {
            eprintln!("[Driver Error] Failed to initialize native driver: {}", e);
            eprintln!("On Linux, make sure your user has write permissions to /dev/uinput or run with proper group privileges.");
            return Err(e.into());
        }
    };

    let crypto = Arc::new(CryptoEngine::new(&token));
    let connected_clients = Arc::new(AtomicUsize::new(0));
    let device_registry = discovery::new_device_registry();

    let local_ip = local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());

    // Start background UDP discovery & beacon engine
    discovery::start_discovery(local_ip.clone(), port, token.clone(), device_registry.clone()).await;

    // Spawn background USB ADB auto-reverse watcher task
    let reverse_port = port;
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(std::time::Duration::from_secs(3));
        loop {
            interval.tick().await;
            if let Some(adb) = health::find_adb_binary() {
                let devs = health::get_adb_devices(&adb);
                let has_authorized = devs.iter().any(|d| d.is_authorized);
                if has_authorized && !health::is_adb_reverse_active(&adb, reverse_port) {
                    if let Ok(_) = health::run_adb_reverse(&adb, reverse_port) {
                        println!("[USB ADB Auto-Reverse] Túnel inverso activado automáticamente en puerto {}", reverse_port);
                    }
                }
            }
        }
    });

    let args: Vec<String> = std::env::args().collect();
    let no_browser = args.iter().any(|a| a == "--no-browser" || a == "--headless" || a == "-d");

    let pairing_url = format!("http://127.0.0.1:{}/pair", port);

    // Automatic browser launch (unless --no-browser or --headless is passed)
    if !no_browser {
        let open_url = pairing_url.clone();
        tokio::spawn(async move {
            tokio::time::sleep(std::time::Duration::from_millis(600)).await;
            #[cfg(target_os = "linux")]
            let _ = std::process::Command::new("xdg-open").arg(&open_url).spawn();

            #[cfg(target_os = "windows")]
            {
                // On Windows, explorer.exe <url> launches default browser reliably via Shell
                if std::process::Command::new("explorer").arg(&open_url).spawn().is_err() {
                    let _ = std::process::Command::new("cmd").args(["/C", "start", "", &open_url]).spawn();
                }
            }

            #[cfg(target_os = "macos")]
            let _ = std::process::Command::new("open").arg(&open_url).spawn();
        });
    }

    let state = AppState {
        config: cfg,
        crypto,
        driver,
        connected_clients,
        device_registry,
    };

    let mobile_url = format!("http://{}:{}/?token={}", local_ip, port, token);

    println!("========================================================");
    println!("        ⚡ ZENTRACK ULTRA-LOW LATENCY NATIVE SERVER ⚡   ");
    println!("========================================================");
    println!("Platform      : {}", std::env::consts::OS);
    println!("Local Address : http://127.0.0.1:{}", port);
    println!("Mobile URL    : {}", mobile_url);
    println!("Pairing GUI   : {}", pairing_url);
    println!("Security Token: {}", token);
    println!("--------------------------------------------------------");
    println!("Scan this QR Code with your ZenTrack Mobile App / Camera:");

    if let Ok(code) = QrCode::new(mobile_url.as_bytes()) {
        let qr_string = code
            .render::<Dense1x2>()
            .dark_color(Dense1x2::Dark)
            .light_color(Dense1x2::Light)
            .build();
        println!("{}", qr_string);
    }

    println!("========================================================");
    println!("Ready for 500Hz+ connections. Waiting for client...");

    let app = create_router(state);
    let addr = SocketAddr::from(([0, 0, 0, 0], port));
    let listener = tokio::net::TcpListener::bind(addr).await?;

    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await?;

    Ok(())
}
