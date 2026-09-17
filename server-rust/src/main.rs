mod config;
mod crypto;
mod driver;
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

    let state = AppState {
        config: cfg,
        crypto,
        driver,
        connected_clients,
    };

    let local_ip = local_ip_address::local_ip()
        .map(|ip| ip.to_string())
        .unwrap_or_else(|_| "127.0.0.1".to_string());

    let mobile_url = format!("http://{}:{}/?token={}", local_ip, port, token);

    println!("========================================================");
    println!("        ⚡ ZENTRACK ULTRA-LOW LATENCY NATIVE SERVER ⚡   ");
    println!("========================================================");
    println!("Platform      : {}", std::env::consts::OS);
    println!("Local Address : http://127.0.0.1:{}", port);
    println!("Mobile URL    : {}", mobile_url);
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
