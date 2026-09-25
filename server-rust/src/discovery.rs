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

/// Helper to retrieve the current, most accurate local network IPv4 address dynamically.
pub fn get_local_ip() -> String {
    if let Ok(ip) = local_ip_address::local_ip() {
        if !ip.is_loopback() {
            return ip.to_string();
        }
    }

    if let Ok(interfaces) = local_ip_address::list_afinet_netifas() {
        for (name, ip) in interfaces {
            // Ignore loopback and virtual/container bridges
            if !ip.is_loopback() && ip.is_ipv4() {
                let lower = name.to_lowercase();
                if !lower.starts_with("docker")
                    && !lower.starts_with("br-")
                    && !lower.starts_with("veth")
                    && !lower.starts_with("virbr")
                {
                    return ip.to_string();
                }
            }
        }
    }

    "127.0.0.1".to_string()
}

/// Spawns the UDP discovery service for zero-config pairing and client detection.
pub async fn start_discovery(
    _initial_ip: String,
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

                            // If client is probing for servers, respond immediately with the current dynamically resolved IP
                            if packet.cmd == "DISCOVER" {
                                let current_ip = get_local_ip();
                                let response = serde_json::json!({
                                    "cmd": "ANNOUNCE",
                                    "name": "ZenTrack Host",
                                    "ip": current_ip,
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

    // 2. Task: Periodic beacon to local broadcast with dynamic IP evaluation
    let beacon_socket = socket.clone();
    let broadcast_addr: SocketAddr = format!("255.255.255.255:{}", DISCOVERY_PORT).parse().unwrap();
    let beacon_token = server_token.clone();

    tokio::spawn(async move {
        let mut last_announced_ip = String::new();
        loop {
            let current_ip = get_local_ip();
            if current_ip != last_announced_ip {
                if !last_announced_ip.is_empty() {
                    println!("[Discovery] Network IP transition: {} -> {}", last_announced_ip, current_ip);
                }
                last_announced_ip = current_ip.clone();
            }

            let beacon_msg = serde_json::json!({
                "cmd": "BEACON",
                "name": "ZenTrack Host",
                "ip": current_ip,
                "port": server_port,
                "token": beacon_token,
                "platform": std::env::consts::OS,
            });

            if let Ok(beacon_bytes) = serde_json::to_vec(&beacon_msg) {
                let _ = beacon_socket.send_to(&beacon_bytes, broadcast_addr).await;
            }

            tokio::time::sleep(Duration::from_secs(3)).await;
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_get_local_ip_validity() {
        let ip = get_local_ip();
        assert!(!ip.is_empty());
        // Verify it parses as a valid IPv4 address
        let parsed: Result<std::net::Ipv4Addr, _> = ip.parse();
        assert!(parsed.is_ok(), "Expected valid IPv4 string, got: {}", ip);
    }
}
