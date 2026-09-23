mod config;
mod crypto;
mod discovery;
mod driver;
mod health;
mod protocol;
mod web;
mod tui;

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

async fn run_server() -> Result<(), Box<dyn std::error::Error>> {
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
    let no_browser = args.iter().any(|a| a == "--no-browser" || a == "--headless" || a == "-d" || a == "--tray");

    let pairing_url = format!("http://127.0.0.1:{}/pair", port);

    // Automatic browser launch (unless --no-browser or --headless or --tray is passed)
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
    print_banner(port, &token, &local_ip, &pairing_url, &mobile_url);

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

fn print_banner(port: u16, token: &str, local_ip: &str, pairing_url: &str, mobile_url: &str) {
    println!("========================================================");
    println!("        ⚡ ZENTRACK ULTRA-LOW LATENCY NATIVE SERVER ⚡   ");
    println!("========================================================");
    println!("Platform      : {}", std::env::consts::OS);
    println!("Local Address : http://127.0.0.1:{}", port);
    println!("Mobile URL    : {}", mobile_url);
    println!("Pairing GUI   : {}", pairing_url);
    println!("Security Token: {}", token);
    println!("--------------------------------------------------------");
    println!("Scan this QR Code with your ZenTrack Mobile App / Camera:\n");

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
}

fn run_tray() {
    use tao::event_loop::{ControlFlow, EventLoopBuilder};
    use tray_icon::{
        menu::{Menu, MenuItem, PredefinedMenuItem},
        TrayIconBuilder, TrayIconEvent,
    };

    let event_loop = EventLoopBuilder::new().build();
    let tray_menu = Menu::new();
    let open_i = MenuItem::new("Abrir Web GUI", true, None);
    let adb_i = MenuItem::new("Activar USB ADB", true, None);
    let restart_i = MenuItem::new("Reiniciar", true, None);
    let quit_i = MenuItem::new("Salir", true, None);
    
    tray_menu.append_items(&[
        &open_i,
        &adb_i,
        &PredefinedMenuItem::separator(),
        &restart_i,
        &quit_i,
    ]).unwrap();

    let mut tray_icon = Some(
        TrayIconBuilder::new()
            .with_menu(Box::new(tray_menu))
            .with_tooltip("ZenTrack Server")
            .build()
            .unwrap(),
    );

    let menu_channel = tray_icon::menu::MenuEvent::receiver();
    let _tray_channel = TrayIconEvent::receiver();

    event_loop.run(move |_event, _, control_flow| {
        *control_flow = ControlFlow::WaitUntil(std::time::Instant::now() + std::time::Duration::from_millis(50));

        if let Ok(event) = menu_channel.try_recv() {
            if event.id == quit_i.id() {
                tray_icon.take();
                *control_flow = ControlFlow::Exit;
                std::process::exit(0);
            } else if event.id == open_i.id() {
                let url = "http://127.0.0.1:3000/pair";
                #[cfg(target_os = "windows")]
                let _ = std::process::Command::new("explorer").arg(url).spawn();
                #[cfg(target_os = "linux")]
                let _ = std::process::Command::new("xdg-open").arg(url).spawn();
            } else if event.id == restart_i.id() {
                std::process::exit(0);
            } else if event.id == adb_i.id() {
                if let Some(adb) = health::find_adb_binary() {
                    let _ = health::run_adb_reverse(&adb, 3000);
                }
            }
        }
    });
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = std::env::args().collect();
    let tray_mode = cfg!(target_os = "windows") || args.iter().any(|a| a == "--tray");
    let tui_mode = args.iter().any(|a| a == "--tui");
    let info_mode = args.iter().any(|a| a == "--info");

    if info_mode {
        let config_path = if PathBuf::from("config.json").exists() {
            PathBuf::from("config.json")
        } else if PathBuf::from("../config.json").exists() {
            PathBuf::from("../config.json")
        } else {
            PathBuf::from("config.json") // Or whatever standard path we have, but load_or_create handles it
        };
        // wait, we can just use the config loading from run_server.
        let cfg = AppConfig::load_or_create(&config_path);
        let port = cfg.port;
        let token = cfg.token;
        let local_ip = local_ip_address::local_ip().map(|ip| ip.to_string()).unwrap_or_else(|_| "127.0.0.1".to_string());
        let pairing_url = format!("http://127.0.0.1:{}/pair", port);
        let mobile_url = format!("http://{}:{}/?token={}", local_ip, port, token);
        print_banner(port, &token, &local_ip, &pairing_url, &mobile_url);
        std::process::exit(0);
    }

    // Start Tokio in a background thread
    let rt = tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()?;

    // Handle server thread via channel
    let (tx, rx) = std::sync::mpsc::channel();
    let is_gui = tui_mode || tray_mode;

    std::thread::spawn(move || {
        rt.block_on(async {
            if let Err(e) = run_server().await {
                let err_str = e.to_string();
                if err_str.contains("Address already in use") || err_str.contains("os error 98") || err_str.contains("Solo se permite un uso") {
                    if is_gui {
                        // Already running, GUI can operate as client
                    } else {
                        eprintln!("[ZenTrack] El servidor ya está en ejecución en el puerto 3000.");
                        let _ = tx.send(false);
                    }
                } else {
                    eprintln!("Server error: {}", e);
                    let _ = tx.send(false);
                }
            } else {
                let _ = tx.send(true);
            }
        });
    });

    if tui_mode {
        if let Err(e) = tui::run_tui() {
            eprintln!("TUI Error: {}", e);
        }
        std::process::exit(0);
    } else if tray_mode {
        run_tray();
    } else {
        // If headless, wait for the server thread to finish or fail
        if let Ok(success) = rx.recv() {
            if !success {
                std::process::exit(1);
            }
        }
    }
    
    Ok(())
}
