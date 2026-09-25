use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};
use rand::RngCore;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    pub token: String,
    #[serde(default = "default_port")]
    pub port: u16,
    #[serde(default)]
    pub first_run_completed: bool,
    #[serde(default = "default_linux_profile")]
    pub linux_profile: String,
}

fn default_linux_profile() -> String {
    "gnome".to_string()
}

fn default_port() -> u16 {
    3000
}

impl AppConfig {
    /// Returns the canonical platform-specific configuration path.
    /// Linux/macOS: $XDG_CONFIG_HOME/zentrack/config.json or ~/.config/zentrack/config.json
    /// Windows: %APPDATA%\ZenTrack\config.json
    pub fn get_canonical_path() -> PathBuf {
        #[cfg(target_os = "windows")]
        {
            if let Ok(appdata) = std::env::var("APPDATA") {
                if !appdata.is_empty() {
                    return PathBuf::from(appdata).join("ZenTrack").join("config.json");
                }
            }
            if let Ok(userprofile) = std::env::var("USERPROFILE") {
                if !userprofile.is_empty() {
                    return PathBuf::from(userprofile)
                        .join("AppData")
                        .join("Roaming")
                        .join("ZenTrack")
                        .join("config.json");
                }
            }
            PathBuf::from("config.json")
        }

        #[cfg(not(target_os = "windows"))]
        {
            // If running under sudo, determine real user's home to avoid root directory
            let home_dir = if let Ok(sudo_user) = std::env::var("SUDO_USER") {
                if !sudo_user.is_empty() && sudo_user != "root" {
                    let user_home = PathBuf::from(format!("/home/{}", sudo_user));
                    if user_home.exists() {
                        user_home
                    } else {
                        std::env::var("HOME").map(PathBuf::from).unwrap_or_else(|_| PathBuf::from("."))
                    }
                } else {
                    std::env::var("HOME").map(PathBuf::from).unwrap_or_else(|_| PathBuf::from("."))
                }
            } else if let Ok(home) = std::env::var("HOME") {
                PathBuf::from(home)
            } else {
                PathBuf::from(".")
            };

            // Standard XDG check (when not under sudo)
            if std::env::var("SUDO_USER").is_err() {
                if let Ok(xdg) = std::env::var("XDG_CONFIG_HOME") {
                    if !xdg.is_empty() {
                        return PathBuf::from(xdg).join("zentrack").join("config.json");
                    }
                }
            }

            home_dir.join(".config").join("zentrack").join("config.json")
        }
    }

    /// Checks legacy locations for existing config.json and migrates to canonical path.
    fn migrate_legacy_config(canonical_path: &Path) -> Option<Self> {
        let mut candidates = Vec::new();

        // 1. Current working directory
        candidates.push(PathBuf::from("config.json"));

        // 2. Parent directory (for dev inside server-rust)
        candidates.push(PathBuf::from("../config.json"));

        // 3. User's home root directory (~/config.json)
        #[cfg(not(target_os = "windows"))]
        {
            let home_dir = if let Ok(sudo_user) = std::env::var("SUDO_USER") {
                if !sudo_user.is_empty() && sudo_user != "root" {
                    PathBuf::from(format!("/home/{}", sudo_user))
                } else {
                    std::env::var("HOME").map(PathBuf::from).unwrap_or_else(|_| PathBuf::from("."))
                }
            } else if let Ok(home) = std::env::var("HOME") {
                PathBuf::from(home)
            } else {
                PathBuf::from(".")
            };
            candidates.push(home_dir.join("config.json"));
        }

        #[cfg(target_os = "windows")]
        {
            if let Ok(userprofile) = std::env::var("USERPROFILE") {
                candidates.push(PathBuf::from(userprofile).join("config.json"));
            }
        }

        // Search candidates for valid configs. Prioritize one with first_run_completed == true
        let mut best_cfg: Option<(Self, PathBuf)> = None;

        for candidate in candidates {
            if candidate.exists() && candidate != canonical_path {
                if let Ok(content) = fs::read_to_string(&candidate) {
                    if let Ok(cfg) = serde_json::from_str::<AppConfig>(&content) {
                        if !cfg.token.is_empty() {
                            if cfg.first_run_completed {
                                best_cfg = Some((cfg, candidate));
                                break;
                            } else if best_cfg.is_none() {
                                best_cfg = Some((cfg, candidate));
                            }
                        }
                    }
                }
            }
        }

        if let Some((cfg, legacy_path)) = best_cfg {
            println!(
                "[Config] Migrating legacy configuration from {:?} to canonical path {:?}",
                legacy_path, canonical_path
            );
            cfg.save(canonical_path);

            // Clean up legacy ~/config.json if it was in the home directory
            #[cfg(not(target_os = "windows"))]
            if let Ok(home) = std::env::var("HOME") {
                let home_legacy = PathBuf::from(home).join("config.json");
                if home_legacy.exists() && home_legacy != canonical_path {
                    let _ = fs::remove_file(home_legacy);
                }
            }

            return Some(cfg);
        }

        None
    }

    /// Loads the configuration from the platform canonical path, performing legacy migration if needed.
    pub fn load() -> Self {
        let canonical_path = Self::get_canonical_path();

        if canonical_path.exists() {
            if let Ok(content) = fs::read_to_string(&canonical_path) {
                if let Ok(cfg) = serde_json::from_str::<AppConfig>(&content) {
                    if !cfg.token.is_empty() {
                        return cfg;
                    }
                }
            }
        }

        // If canonical path doesn't exist, try migrating from legacy locations
        if let Some(migrated) = Self::migrate_legacy_config(&canonical_path) {
            return migrated;
        }

        // Otherwise generate a new configuration at canonical path
        Self::load_or_create(&canonical_path)
    }

    /// Saves the configuration to the canonical platform path.
    pub fn save_default(&self) {
        self.save(Self::get_canonical_path());
    }

    pub fn save<P: AsRef<Path>>(&self, config_path: P) {
        let path = config_path.as_ref();
        if let Some(parent) = path.parent() {
            let _ = fs::create_dir_all(parent);
        }
        if let Ok(json_str) = serde_json::to_string_pretty(self) {
            let _ = fs::write(path, json_str);
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
            linux_profile: default_linux_profile(),
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_canonical_path_format() {
        let path = AppConfig::get_canonical_path();
        assert!(path.to_string_lossy().contains("config.json"));
        #[cfg(target_os = "linux")]
        {
            assert!(path.to_string_lossy().contains("zentrack"));
        }
    }

    #[test]
    fn test_config_save_load() {
        let temp_dir = std::env::temp_dir().join(format!("zentrack_test_{}", rand::random::<u32>()));
        let config_file = temp_dir.join("subfolder").join("config.json");

        let cfg = AppConfig {
            token: "test_token_12345".to_string(),
            port: 3000,
            first_run_completed: true,
            linux_profile: "kde".to_string(),
        };

        cfg.save(&config_file);
        assert!(config_file.exists());

        let loaded = AppConfig::load_or_create(&config_file);
        assert_eq!(loaded.token, "test_token_12345");
        assert_eq!(loaded.port, 3000);
        assert!(loaded.first_run_completed);
        assert_eq!(loaded.linux_profile, "kde");

        let _ = fs::remove_dir_all(temp_dir);
    }
}
