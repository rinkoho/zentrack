use serde::{Deserialize, Serialize};
use std::fs;
use std::path::Path;
use rand::RngCore;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub token: String,
    #[serde(default = "default_port")]
    pub port: u16,
    #[serde(default)]
    pub first_run_completed: bool,
}

fn default_port() -> u16 {
    3000
}

impl AppConfig {
    pub fn save<P: AsRef<Path>>(&self, config_path: P) {
        if let Ok(json_str) = serde_json::to_string_pretty(self) {
            let _ = fs::write(config_path, json_str);
        }
    }

    pub fn load_or_create<P: AsRef<Path>>(config_path: P) -> Self {
        let path = config_path.as_ref();
        if path.exists() {
            if let Ok(content) = fs::read_to_string(path) {
                if let Ok(cfg) = serde_json::from_str::<AppConfig>(&content) {
                    if !cfg.token.is_empty() {
                        return cfg;
                    }
                }
            }
        }

        // Generate a new 16-byte random token
        let mut random_bytes = [0u8; 16];
        rand::thread_rng().fill_bytes(&mut random_bytes);
        let token = hex::encode(random_bytes);

        let cfg = AppConfig {
            token,
            port: default_port(),
            first_run_completed: false,
        };

        cfg.save(path);
        cfg
    }
}

mod hex {
    pub fn encode<T: AsRef<[u8]>>(data: T) -> String {
        data.as_ref().iter().map(|b| format!("{:02x}", b)).collect()
    }
}
