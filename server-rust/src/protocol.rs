use byteorder::{ByteOrder, LittleEndian};
use serde::{Deserialize, Serialize};

#[derive(Debug)]
pub enum InputCommand {
    MouseMove { dx: i32, dy: i32 },
    SmoothScroll { dx: f64, dy: f64 },
    MouseClick { button: u8, double: bool },
    MouseDown { button: u8 },
    MouseUp { button: u8 },
    ScrollDiscrete { direction: String, steps: i32 },
    KeyDown { key: String },
    KeyUp { key: String },
    KeyClick { key: String },
    Shortcut { action: String },
    GamepadMode { enabled: bool },
    GamepadButton { name: String, value: i32 },
    GamepadAxis { name: String, value: i32 },
    Ping { id: serde_json::Value },
    EncryptedKey { iv: String, data: String, tag: String },
    Unknown,
}

#[derive(Debug, Deserialize)]
#[serde(tag = "type")]
pub enum JsonPayload {
    #[serde(rename = "ping")]
    Ping { id: serde_json::Value },
    #[serde(rename = "move")]
    Move { dx: i32, dy: i32 },
    #[serde(rename = "click")]
    Click {
        #[serde(default = "default_button")]
        button: u8,
        #[serde(default)]
        double: bool,
    },
    #[serde(rename = "mousedown")]
    MouseDown {
        #[serde(default = "default_button")]
        button: u8,
    },
    #[serde(rename = "mouseup")]
    MouseUp {
        #[serde(default = "default_button")]
        button: u8,
    },
    #[serde(rename = "scroll")]
    Scroll {
        direction: String,
        #[serde(default = "default_steps")]
        steps: i32,
    },
    #[serde(rename = "smooth_scroll")]
    SmoothScroll {
        #[serde(default)]
        dx: f64,
        #[serde(default)]
        dy: f64,
    },
    #[serde(rename = "keydown")]
    KeyDown { key: String },
    #[serde(rename = "keyup")]
    KeyUp { key: String },
    #[serde(rename = "key")]
    Key { key: String },
    #[serde(rename = "shortcut")]
    Shortcut { action: String },
    #[serde(rename = "gamepad_mode")]
    GamepadMode { enabled: bool },
    #[serde(rename = "gp_btn")]
    GpBtn { name: String, value: i32 },
    #[serde(rename = "gp_axis")]
    GpAxis { name: String, value: i32 },
    #[serde(rename = "enc_key")]
    EncKey { iv: String, data: String, tag: String },
    #[serde(other)]
    Unknown,
}

fn default_button() -> u8 {
    1
}

fn default_steps() -> i32 {
    1
}

#[derive(Debug, Serialize)]
pub struct PongResponse {
    #[serde(rename = "type")]
    pub msg_type: &'static str,
    pub id: serde_json::Value,
}

impl InputCommand {
    /// Fast zero-allocation binary protocol parser
    /// Packet: 6 bytes Little-Endian: [cmd: i16, x: i16, y: i16]
    pub fn parse_binary(bytes: &[u8]) -> Option<Self> {
        if bytes.len() < 6 {
            return None;
        }

        let cmd = LittleEndian::read_i16(&bytes[0..2]);
        let x = LittleEndian::read_i16(&bytes[2..4]);
        let y = LittleEndian::read_i16(&bytes[4..6]);

        match cmd {
            1 => Some(InputCommand::MouseMove {
                dx: x as i32,
                dy: y as i32,
            }),
            2 => Some(InputCommand::SmoothScroll {
                dx: (x as f64) / 10.0,
                dy: (y as f64) / 10.0,
            }),
            _ => None,
        }
    }

    /// JSON Protocol parser
    pub fn parse_json(text: &str) -> Option<Self> {
        let parsed: JsonPayload = serde_json::from_str(text).ok()?;
        match parsed {
            JsonPayload::Ping { id } => Some(InputCommand::Ping { id }),
            JsonPayload::Move { dx, dy } => Some(InputCommand::MouseMove { dx, dy }),
            JsonPayload::Click { button, double } => Some(InputCommand::MouseClick { button, double }),
            JsonPayload::MouseDown { button } => Some(InputCommand::MouseDown { button }),
            JsonPayload::MouseUp { button } => Some(InputCommand::MouseUp { button }),
            JsonPayload::Scroll { direction, steps } => Some(InputCommand::ScrollDiscrete { direction, steps }),
            JsonPayload::SmoothScroll { dx, dy } => Some(InputCommand::SmoothScroll { dx, dy }),
            JsonPayload::KeyDown { key } => Some(InputCommand::KeyDown { key }),
            JsonPayload::KeyUp { key } => Some(InputCommand::KeyUp { key }),
            JsonPayload::Key { key } => Some(InputCommand::KeyClick { key }),
            JsonPayload::Shortcut { action } => Some(InputCommand::Shortcut { action }),
            JsonPayload::GamepadMode { enabled } => Some(InputCommand::GamepadMode { enabled }),
            JsonPayload::GpBtn { name, value } => Some(InputCommand::GamepadButton { name, value }),
            JsonPayload::GpAxis { name, value } => Some(InputCommand::GamepadAxis { name, value }),
            JsonPayload::EncKey { iv, data, tag } => Some(InputCommand::EncryptedKey { iv, data, tag }),
            JsonPayload::Unknown => Some(InputCommand::Unknown),
        }
    }
}
