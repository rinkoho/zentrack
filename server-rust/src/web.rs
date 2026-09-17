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
    routing::get,
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
        .route("/qr.svg", get(qr_svg_handler))
        .route("/pair", get(pair_handler))
        .fallback(static_handler)
        .with_state(state)
}

async fn devices_handler(State(state): State<AppState>) -> impl IntoResponse {
    let devices = crate::discovery::get_discovered_devices(&state.device_registry).await;
    (
        [(header::CONTENT_TYPE, "application/json; charset=utf-8")],
        serde_json::to_string(&devices).unwrap_or_else(|_| "[]".to_string()),
    )
}

async fn qr_svg_handler(State(state): State<AppState>) -> impl IntoResponse {
    let local_ip = local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());
    let url = format!(
        "http://{}:{}/?token={}",
        local_ip, state.config.port, state.config.token
    );

    let svg_content = match QrCode::new(url.as_bytes()) {
        Ok(code) => code
            .render::<svg::Color>()
            .min_dimensions(300, 300)
            .dark_color(svg::Color("#ff6b35"))
            .light_color(svg::Color("#000000"))
            .build(),
        Err(_) => String::new(),
    };

    ([(header::CONTENT_TYPE, "image/svg+xml; charset=utf-8")], svg_content)
}

async fn pair_handler() -> Response {
    serve_static_asset("pair.html")
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

    while let Some(msg_result) = socket.recv().await {
        let msg = match msg_result {
            Ok(m) => m,
            Err(_) => break,
        };

        match msg {
            Message::Binary(bytes) => {
                if let Some(cmd) = InputCommand::parse_binary(&bytes) {
                    execute_command(cmd, &state, &mut socket).await;
                }
            }
            Message::Text(text) => {
                if let Some(cmd) = InputCommand::parse_json(&text) {
                    execute_command(cmd, &state, &mut socket).await;
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

async fn execute_command(cmd: InputCommand, state: &AppState, socket: &mut WebSocket) {
    match cmd {
        InputCommand::MouseMove { dx, dy } => {
            let mut driver = state.driver.lock().await;
            driver.mouse_move(dx, dy);
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
            handle_system_shortcut(&action).await;
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
                    Box::pin(execute_command(inner_cmd, state, socket)).await;
                }
            }
        }
        InputCommand::Unknown => {}
    }
}

async fn handle_system_shortcut(action: &str) {
    #[cfg(target_os = "linux")]
    {
        let (cmd, args) = match action {
            "terminal" => ("xdotool", vec!["key", "super+Return"]),
            "browser" => ("xdotool", vec!["key", "super+b"]),
            "file_manager" => ("xdotool", vec!["key", "super+f"]),
            "rofi" => ("xdotool", vec!["key", "super+space"]),
            "close_window" => ("xdotool", vec!["key", "super+x"]),
            "workspace_left" => ("xdotool", vec!["key", "super+Left"]),
            "workspace_right" => ("xdotool", vec!["key", "super+Right"]),
            _ => return,
        };
        let _ = tokio::process::Command::new(cmd).args(args).spawn();
    }

    #[cfg(target_os = "windows")]
    {
        // Windows system shortcuts
        let _ = action;
    }
}
