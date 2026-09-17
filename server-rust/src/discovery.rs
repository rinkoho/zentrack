use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::UdpSocket;
use tokio::sync::RwLock;
use tokio::time::{Duration, Instant};

pub const DISCOVERY_PORT: u16 = 37020;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiscoveredDevice {
    pub ip: String,
    pub name: String,
    pub last_seen_secs_ago: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct DiscoveryPacket {
    cmd: String,
    #[serde(default)]
    device: Option<String>,
    #[serde(default)]
    ip: Option<String>,
    #[serde(default)]
    name: Option<String>,
    #[serde(default)]
    port: Option<u16>,
    #[serde(default)]
    token: Option<String>,
    #[serde(default)]
    platform: Option<String>,
}

pub type DeviceRegistry = Arc<RwLock<HashMap<String, (String, Instant)>>>;

pub fn new_device_registry() -> DeviceRegistry {
    Arc::new(RwLock::new(HashMap::new()))
}

/// Spawns the UDP discovery service for zero-config pairing and client detection.
pub async fn start_discovery(
    server_ip: String,
    server_port: u16,
    server_token: String,
    registry: DeviceRegistry,
) {
    let listen_addr = format!("0.0.0.0:{}", DISCOVERY_PORT);
    let socket = match UdpSocket::bind(&listen_addr).await {
        Ok(s) => {
            let _ = s.set_broadcast(true);
            Arc::new(s)
        }
        Err(e) => {
            eprintln!("[Discovery] Warning: Could not bind UDP port {}: {}", DISCOVERY_PORT, e);
            return;
        }
    };

    println!("[Discovery] Zero-Config Auto-Discovery running on UDP :{}", DISCOVERY_PORT);

    // 1. Task: Listen for discovery probes and client announcements
    let recv_socket = socket.clone();
    let reg_clone = registry.clone();
    let announce_ip = server_ip.clone();
    let announce_token = server_token.clone();

    tokio::spawn(async move {
        let mut buf = [0u8; 2048];
        loop {
            match recv_socket.recv_from(&mut buf).await {
                Ok((len, peer_addr)) => {
                    if let Ok(text) = std::str::from_utf8(&buf[..len]) {
                        if let Ok(packet) = serde_json::from_str::<DiscoveryPacket>(text) {
                            let peer_ip = peer_addr.ip().to_string();

                            // Record client announcement or discovery probe
                            if let Some(device_name) = packet.device {
                                let mut reg = reg_clone.write().await;
                                reg.insert(peer_ip.clone(), (device_name.clone(), Instant::now()));
                                println!("[Discovery] 📱 Detected ZenTrack client: {} ({})", device_name, peer_ip);
                            }

                            // If client is probing for servers, respond immediately
                            if packet.cmd == "DISCOVER" {
                                let response = serde_json::json!({
                                    "cmd": "ANNOUNCE",
                                    "name": "ZenTrack Host",
                                    "ip": announce_ip,
                                    "port": server_port,
                                    "token": announce_token,
                                    "platform": std::env::consts::OS,
                                });

                                if let Ok(resp_bytes) = serde_json::to_vec(&response) {
                                    let _ = recv_socket.send_to(&resp_bytes, peer_addr).await;
                                }
                            }
                        }
                    }
                }
                Err(_) => {
                    tokio::time::sleep(Duration::from_millis(100)).await;
                }
            }
        }
    });

    // 2. Task: Periodic beacon to local broadcast
    let beacon_socket = socket.clone();
    let broadcast_addr: SocketAddr = format!("255.255.255.255:{}", DISCOVERY_PORT).parse().unwrap();

    tokio::spawn(async move {
        let beacon_msg = serde_json::json!({
            "cmd": "BEACON",
            "name": "ZenTrack Host",
            "ip": server_ip,
            "port": server_port,
            "token": server_token,
            "platform": std::env::consts::OS,
        });

        if let Ok(beacon_bytes) = serde_json::to_vec(&beacon_msg) {
            loop {
                let _ = beacon_socket.send_to(&beacon_bytes, broadcast_addr).await;
                tokio::time::sleep(Duration::from_secs(3)).await;
            }
        }
    });
}

pub async fn get_discovered_devices(registry: &DeviceRegistry) -> Vec<DiscoveredDevice> {
    let reg = registry.read().await;
    let now = Instant::now();
    let mut list = Vec::new();

    for (ip, (name, seen)) in reg.iter() {
        let secs = now.duration_since(*seen).as_secs();
        // Keep devices seen in the last 120 seconds
        if secs < 120 {
            list.push(DiscoveredDevice {
                ip: ip.clone(),
                name: name.clone(),
                last_seen_secs_ago: secs,
            });
        }
    }

    list.sort_by_key(|d| d.last_seen_secs_ago);
    list
}
