use aes_gcm::{
    aead::{Aead, KeyInit},
    Aes256Gcm, Nonce,
};
use base64::prelude::*;
use serde::Deserialize;
use sha2::{Digest, Sha256};

#[derive(Debug, Deserialize)]
pub struct EncryptedPayload {
    pub iv: String,
    pub data: String,
    pub tag: String,
}

pub struct CryptoEngine {
    cipher: Aes256Gcm,
}

impl CryptoEngine {
    pub fn new(token: &str) -> Self {
        // Derive 32-byte AES key using SHA-256(token) matching Android ZenCrypto.kt
        let mut hasher = Sha256::new();
        hasher.update(token.as_bytes());
        let key_bytes = hasher.finalize();

        let cipher = Aes256Gcm::new_from_slice(&key_bytes)
            .expect("Valid 32-byte key for AES-256-GCM");

        Self { cipher }
    }

    /// Decrypts an E2EE keyboard payload received from ZenTrack Android client.
    /// In Java: ciphertextWithTag = ciphertext (N bytes) + authTag (16 bytes)
    pub fn decrypt(&self, payload: &EncryptedPayload) -> Option<String> {
        let iv = BASE64_STANDARD.decode(&payload.iv).ok()?;
        let data = BASE64_STANDARD.decode(&payload.data).ok()?;
        let tag = BASE64_STANDARD.decode(&payload.tag).ok()?;

        if iv.len() != 12 || tag.len() != 16 {
            return None;
        }

        let nonce = Nonce::from_slice(&iv);

        // aes-gcm crate expects ciphertext followed by 16-byte authentication tag
        let mut combined = data;
        combined.extend_from_slice(&tag);

        match self.cipher.decrypt(nonce, combined.as_ref()) {
            Ok(plaintext_bytes) => String::from_utf8(plaintext_bytes).ok(),
            Err(e) => {
                eprintln!("[Crypto] AES-GCM decryption failed: {:?}", e);
                None
            }
        }
    }
}
