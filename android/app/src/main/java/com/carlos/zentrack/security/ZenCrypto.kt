package com.carlos.zentrack.security

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object ZenCrypto {
    private var secretKeySpec: SecretKeySpec? = null
    private val secureRandom = SecureRandom()

    fun init(token: String) {
        if (token.isEmpty()) return
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        secretKeySpec = SecretKeySpec(keyBytes, "AES")
    }

    fun encryptKeyboardPayload(jsonString: String): String? {
        val keySpec = secretKeySpec ?: return null
        return try {
            val iv = ByteArray(12)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            val ciphertextWithTag = cipher.doFinal(jsonString.toByteArray(Charsets.UTF_8))

            // AES-GCM output in Java includes 16-byte auth tag at the end of ciphertext
            val cipherLen = ciphertextWithTag.size - 16
            val ciphertext = ciphertextWithTag.copyOfRange(0, cipherLen)
            val authTag = ciphertextWithTag.copyOfRange(cipherLen, ciphertextWithTag.size)

            val encJson = JSONObject()
            encJson.put("type", "enc_key")
            encJson.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            encJson.put("data", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            encJson.put("tag", Base64.encodeToString(authTag, Base64.NO_WRAP))

            encJson.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
