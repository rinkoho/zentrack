use crate::config::AppConfig;
use crate::crypto::CryptoEngine;
use crate::driver::InputDriver;
use crate::protocol::{InputCommand, PongResponse};

use axum::{
    extract::{
        ws::{Message, WebSocket, WebSocketUpgrade},
        Query, State,
    },
    http::{header, HeaderMap, StatusCode, Uri},
    response::{IntoResponse, Response},
    routing::{get, post},
    Router,
};
use rust_embed::Embed;
use serde::Deserialize;
use serde_json::json;
use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;
use tokio::sync::Mutex;

use qrcode::render::svg;
use qrcode::QrCode;

#[derive(Embed)]
#[folder = "../public/"]
struct Assets;

#[derive(Clone)]
pub struct AppState {
    pub config: AppConfig,
    pub crypto: Arc<CryptoEngine>,
    pub driver: Arc<Mutex<Box<dyn InputDriver>>>,
    pub connected_clients: Arc<AtomicUsize>,
    pub device_registry: crate::discovery::DeviceRegistry,
}

#[derive(Deserialize)]
pub struct WsQuery {
    pub token: Option<String>,
}

pub fn create_router(state: AppState) -> Router {
    Router::new()
        .route("/", get(ws_or_index_handler))
        .route("/status", get(status_handler))
        .route("/api/devices", get(devices_handler))
        .route("/api/system-health", get(health_handler))
        .route("/api/usb/reverse", post(usb_reverse_handler))
        .route("/api/install_vigem", post(install_vigem_handler))
        .route("/api/system/restart", post(restart_system_handler))
        .route("/api/config/profile", post(config_profile_handler))
        .route("/qr.svg", get(qr_svg_handler))
        .route("/pair", get(pair_handler))
        .fallback(static_handler)
        .with_state(state)
}

async fn install_vigem_handler() -> impl IntoResponse {
    #[cfg(target_os = "windows")]
    {
        let candidates = [
            "drivers\\ViGEmBus_Setup.exe",
            ".\\ViGEmBus_Setup.exe",
            "..\\drivers\\ViGEmBus_Setup.exe",
        ];
        for path in &candidates {
            if std::path::Path::new(path).exists() {
                let mut cmd = std::process::Command::new("cmd");
                use std::os::windows::process::CommandExt;
                cmd.creation_flags(0x08000000);
                let _ = cmd.args(["/C", "start", "", path]).spawn();
                let res = json!({"success": true, "message": "Instalador oficial ViGEmBus iniciado. Sigue los pasos en pantalla."});
                return ([(header::CONTENT_TYPE, "application/json; charset=utf-8")], res.to_string());
            }
        }

        let mut cmd = std::process::Command::new("cmd");
        use std::os::windows::process::CommandExt;
        cmd.creation_flags(0x08000000);
        let _ = cmd
            .args(["/C", "start", "", "https://github.com/nefarius/ViGEmBus/releases/download/v1.22.0/ViGEmBus_1.22.0_x64_x86_arm64.exe"])
            .spawn();

        let res = json!({"success": true, "message": "Descargando instalador oficial de ViGEmBus..."});
        ([(header::CONTENT_TYPE, "application/json; charset=utf-8")], res.to_string())
    }

    #[cfg(not(target_os = "windows"))]
    {
        let res = json!({"success": true, "message": "En Linux el driver de gamepad viene integrado en el kernel (/dev/uinput)"});
        ([(header::CONTENT_TYPE, "application/json; charset=utf-8")], res.to_string())
    }
}

async fn health_handler(State(state): State<AppState>) -> impl IntoResponse {
    let port = state.config.port;
    let health = tokio::task::spawn_blocking(move || {
        crate::health::inspect_system(port)
    }).await.unwrap();
    
    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        serde_json::to_string(&health).unwrap_or_else(|_| "{}".to_string()),
    )
}

async fn usb_reverse_handler(State(state): State<AppState>) -> impl IntoResponse {
    let port = state.config.port;
    let res = tokio::task::spawn_blocking(move || {
        if let Some(adb) = crate::health::find_adb_binary() {
            crate::health::run_adb_reverse(&adb, port)
                .map(|_| json!({"success": true, "message": "Túnel USB ADB activado con éxito"}))
                .unwrap_or_else(|e| json!({"success": false, "error": e}))
        } else {
            json!({"success": false, "error": "Herramienta ADB no encontrada en el sistema"})
        }
    }).await.unwrap();

    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        res.to_string(),
    )
}

async fn restart_system_handler() -> impl IntoResponse {
    std::thread::spawn(|| {
        std::thread::sleep(std::time::Duration::from_millis(500));
        #[cfg(target_os = "linux")]
        let _ = std::process::Command::new("systemctl").arg("--user").arg("restart").arg("zentrack").spawn();
        std::process::exit(0);
    });
    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        json!({"success": true, "message": "Reiniciando servidor..."}).to_string(),
    )
}

async fn config_profile_handler(body: String) -> impl IntoResponse {
    let profile = body.trim().to_string();
    if ["gnome", "kde", "bspwm"].contains(&profile.as_str()) {
        let mut cfg = crate::config::AppConfig::load();
        cfg.linux_profile = profile;
        cfg.save_default();
        (StatusCode::OK, "OK")
    } else {
        (StatusCode::BAD_REQUEST, "Invalid profile")
    }
}

async fn devices_handler(State(state): State<AppState>) -> impl IntoResponse {
    let devices = crate::discovery::get_discovered_devices(&state.device_registry).await;
    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        serde_json::to_string(&devices).unwrap_or_else(|_| "[]".to_string()),
    )
}

async fn qr_svg_handler(State(state): State<AppState>) -> impl IntoResponse {
    let local_ip = crate::discovery::get_local_ip();
    let url = format!(
        "http://{}:{}/?token={}",
        local_ip, state.config.port, state.config.token
    );

    // High contrast QR: Pure white background (#ffffff) and black modules (#000000)
    // Ensures ISO/IEC 18004 compliance and instant camera detection
    let svg_content = match QrCode::new(url.as_bytes()) {
        Ok(code) => code
            .render::<svg::Color>()
            .min_dimensions(320, 320)
            .dark_color(svg::Color("#000000"))
            .light_color(svg::Color("#ffffff"))
            .build(),
        Err(_) => String::new(),
    };

    ([(header::CONTENT_TYPE, "image/svg+xml; charset=utf-8")], svg_content)
}

async fn pair_handler() -> Response {
    let mut resp = serve_static_asset("pair.html");
    resp.headers_mut().insert(
        header::CACHE_CONTROL,
        header::HeaderValue::from_static("no-cache, no-store, must-revalidate"),
    );
    resp
}

async fn status_handler(State(state): State<AppState>) -> impl IntoResponse {
    let count = state.connected_clients.load(Ordering::Relaxed);
    let body = json!({
        "status": "active",
        "port": state.config.port,
        "token": state.config.token,
        "connectedCount": count
    });
    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        body.to_string(),
    )
}

async fn ws_or_index_handler(
    headers: HeaderMap,
    ws: Option<WebSocketUpgrade>,
    Query(query): Query<WsQuery>,
    State(state): State<AppState>,
) -> Response {
    // Check if client is requesting a WebSocket upgrade
    let is_ws = headers
        .get(header::UPGRADE)
        .and_then(|h| h.to_str().ok())
        .map(|s| s.eq_ignore_ascii_case("websocket"))
        .unwrap_or(false);

    if is_ws {
        if let Some(ws_upgrade) = ws {
            // Verify security token
            if let Some(ref client_token) = query.token {
                if client_token == &state.config.token {
                    return ws_upgrade.on_upgrade(move |socket| handle_socket(socket, state));
                }
            }
            eprintln!("[Security] Rejected unauthorized WebSocket connection attempt (Invalid or missing token)");
            return (StatusCode::UNAUTHORIZED, "Unauthorized: Invalid security token").into_response();
        }
    }

    // Otherwise serve index.html
    serve_static_asset("index.html")
}

async fn static_handler(uri: Uri) -> Response {
    let path = uri.path().trim_start_matches('/');
    let target = if path.is_empty() { "index.html" } else { path };
    serve_static_asset(target)
}

fn serve_static_asset(path: &str) -> Response {
    // Check local filesystem first (allows instant live customization)
    let local_candidates = [
        format!("public/{}", path),
        format!("../public/{}", path),
    ];
    for p in &local_candidates {
        if let Ok(bytes) = std::fs::read(p) {
            let mime = mime_guess::from_path(path).first_or_octet_stream();
            return ([(header::CONTENT_TYPE, mime.as_ref())], bytes).into_response();
        }
    }

    match Assets::get(path) {
        Some(content) => {
            let mime = mime_guess::from_path(path).first_or_octet_stream();
            ([(header::CONTENT_TYPE, mime.as_ref())], content.data).into_response()
        }
        None => (StatusCode::NOT_FOUND, "404 Not Found").into_response(),
    }
}

async fn handle_socket(mut socket: WebSocket, state: AppState) {
    state.connected_clients.fetch_add(1, Ordering::SeqCst);
    println!("[WebSocket] Client connected! (Active: {})", state.connected_clients.load(Ordering::Relaxed));

    // Send initial handshake settings
    let init_settings = json!({
        "type": "sync_settings",
        "highPolling": true
    });
    let _ = socket.send(Message::Text(init_settings.to_string())).await;

    // PACER JITTER BUFFER
    let (tx, mut rx) = tokio::sync::mpsc::unbounded_channel::<(i32, i32)>();
    let state_clone = state.clone();
    tokio::spawn(async move {
        while let Some((dx, dy)) = rx.recv().await {
            let mut driver = state_clone.driver.lock().await;
            driver.mouse_move(dx, dy);
            drop(driver); // release lock quickly
            tokio::time::sleep(tokio::time::Duration::from_millis(2)).await; // 500Hz pace
        }
    });

    let mut last_packet = std::time::Instant::now();
    let mut batch_count = 0;
    use std::io::Write;

    while let Some(msg_result) = socket.recv().await {
        let msg = match msg_result {
            Ok(m) => m,
            Err(_) => break,
        };

        match msg {
            Message::Binary(bytes) => {
                let now = std::time::Instant::now();
                let dt = now.duration_since(last_packet).as_millis();
                last_packet = now;
                
                // Only log if dt > 0 to avoid massive spam, or just log everything to a file
                batch_count += 1;
                if batch_count % 50 == 0 {
                    let mut file = std::fs::OpenOptions::new().create(true).append(true).open("/tmp/zentrack_jitter.log").unwrap();
                    writeln!(file, "Binary packet received. dt={}ms", dt).unwrap();
                } else if dt > 12 {
                    let mut file = std::fs::OpenOptions::new().create(true).append(true).open("/tmp/zentrack_jitter.log").unwrap();
                    writeln!(file, "LARGE JITTER DETECTED: dt={}ms", dt).unwrap();
                }

                if let Some(cmd) = InputCommand::parse_binary(&bytes) {
                    execute_command(cmd, &state, &mut socket, &tx).await;
                }
            }
            Message::Text(text) => {
                if let Some(cmd) = InputCommand::parse_json(&text) {
                    execute_command(cmd, &state, &mut socket, &tx).await;
                }
            }
            Message::Ping(payload) => {
                let _ = socket.send(Message::Pong(payload)).await;
            }
            Message::Close(_) => break,
            _ => {}
        }
    }

    state.connected_clients.fetch_sub(1, Ordering::SeqCst);
    println!("[WebSocket] Client disconnected. (Active: {})", state.connected_clients.load(Ordering::Relaxed));

    // Safety: Release all held modifier keys and stop gamepad
    let mut driver = state.driver.lock().await;
    driver.release_all_modifiers();
    driver.set_gamepad_mode(false);
}

async fn execute_command(cmd: InputCommand, state: &AppState, socket: &mut WebSocket, tx: &tokio::sync::mpsc::UnboundedSender<(i32, i32)>) {
    match cmd {
        InputCommand::MouseMove { dx, dy } => {
            let _ = tx.send((dx, dy));
        }
        InputCommand::SmoothScroll { dx, dy } => {
            let mut driver = state.driver.lock().await;
            driver.smooth_scroll(dx, dy);
        }
        InputCommand::MouseClick { button, double } => {
            let mut driver = state.driver.lock().await;
            driver.mouse_click(button, double);
        }
        InputCommand::MouseDown { button } => {
            let mut driver = state.driver.lock().await;
            driver.mouse_down(button);
        }
        InputCommand::MouseUp { button } => {
            let mut driver = state.driver.lock().await;
            driver.mouse_up(button);
        }
        InputCommand::ScrollDiscrete { direction, steps } => {
            let mut driver = state.driver.lock().await;
            driver.scroll_discrete(&direction, steps);
        }
        InputCommand::KeyDown { key } => {
            let mut driver = state.driver.lock().await;
            driver.key_down(&key);
        }
        InputCommand::KeyUp { key } => {
            let mut driver = state.driver.lock().await;
            driver.key_up(&key);
        }
        InputCommand::KeyClick { key } => {
            let mut driver = state.driver.lock().await;
            driver.key_click(&key);
        }
        InputCommand::Shortcut { action } => {
            let driver = state.driver.lock().await;
            handle_system_shortcut(&action, driver).await;
        }
        InputCommand::GamepadMode { enabled } => {
            let mut driver = state.driver.lock().await;
            driver.set_gamepad_mode(enabled);
        }
        InputCommand::GamepadButton { name, value } => {
            let mut driver = state.driver.lock().await;
            driver.gamepad_btn(&name, value);
        }
        InputCommand::GamepadAxis { name, value } => {
            let mut driver = state.driver.lock().await;
            driver.gamepad_axis(&name, value);
        }
        InputCommand::Ping { id } => {
            let resp = PongResponse {
                msg_type: "pong",
                id,
            };
            if let Ok(json_str) = serde_json::to_string(&resp) {
                let _ = socket.send(Message::Text(json_str)).await;
            }
        }
        InputCommand::EncryptedKey { iv, data, tag } => {
            let payload = crate::crypto::EncryptedPayload { iv, data, tag };
            if let Some(decrypted_json) = state.crypto.decrypt(&payload) {
                if let Some(inner_cmd) = InputCommand::parse_json(&decrypted_json) {
                    Box::pin(execute_command(inner_cmd, state, socket, tx)).await;
                }
            }
        }
        InputCommand::Unknown => {}
    }
}

async fn handle_system_shortcut(action: &str, mut driver: tokio::sync::MutexGuard<'_, Box<dyn crate::driver::InputDriver>>) {
    #[cfg(target_os = "linux")]
    {
        let cfg = crate::config::AppConfig::load();
        let profile = cfg.linux_profile.as_str();
        let keys = match action {
            "terminal" => match profile {
                "bspwm" => vec!["Super_L", "Return"],
                _ => vec!["Control_L", "Alt_L", "t"], // Gnome/KDE default
            },
            "browser" => match profile {
                "bspwm" => vec!["Super_L", "b"],
                _ => vec![], // No standard global browser shortcut in Gnome/KDE
            },
            "file_manager" => vec!["Super_L", "e"], // Super+E is standard on Windows, sometimes mapped in Linux
            "rofi" => match profile {
                "bspwm" => vec!["Super_L", "space"],
                _ => vec!["Super_L"], // Gnome/KDE launcher
            },
            "close_window" => match profile {
                "bspwm" => vec!["Super_L", "x"],
                _ => vec!["Alt_L", "F4"], // Standard close
            },
            "workspace_left" => match profile {
                "bspwm" => vec!["Super_L", "Left"],
                "kde" => vec!["Control_L", "Super_L", "Left"],
                _ => vec!["Super_L", "Page_Up"], // Gnome default
            },
            "workspace_right" => match profile {
                "bspwm" => vec!["Super_L", "Right"],
                "kde" => vec!["Control_L", "Super_L", "Right"],
                _ => vec!["Super_L", "Page_Down"], // Gnome default
            },
            _ => vec![],
        };
        
        for k in &keys {
            driver.key_down(k);
        }
        for k in keys.iter().rev() {
            driver.key_up(k);
        }
    }

    #[cfg(target_os = "windows")]
    {
        // Windows system shortcuts mapping to driver
        let keys = match action {
            "workspace_left" => vec!["Control_L", "Super_L", "Left"],
            "workspace_right" => vec!["Control_L", "Super_L", "Right"],
            "close_window" => vec!["Alt_L", "F4"],
            _ => vec![],
        };
        for k in &keys {
            driver.key_down(k);
        }
        for k in keys.iter().rev() {
            driver.key_up(k);
        }
    }
}
