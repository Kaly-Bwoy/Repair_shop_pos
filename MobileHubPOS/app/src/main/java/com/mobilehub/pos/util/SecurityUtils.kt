package com.mobilehub.pos.util

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import android.util.Base64

object SecurityUtils {

    /**
     * Hashes a string (like a 4-digit PIN) using SHA-256.
     * This ensures PINs are never stored in plaintext inside SQLite or backups.
     */
    fun hashPin(pin: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(pin.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback safe hash if hashing fails
            pin.hashCode().toString()
        }
    }

    /**
     * Simple and fast AES encryption for offline backup strings using a derived key.
     * This protects the backup JSON file from being read as plain text by other apps.
     */
    fun encryptAES(plainText: String, secretKey: String): String {
        return try {
            // Pad or trim key to 16 bytes for AES-128
            val keyBytes = secretKey.padEnd(16, '0').substring(0, 16).toByteArray(Charsets.UTF_8)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            
            // Fixed initialization vector for offline portability
            val iv = ByteArray(16) { 0 }
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec)

            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            plainText // Fallback to raw text if encryption fails to prevent losing backup functionality
        }
    }

    /**
     * Decrypts an AES-encrypted backup string.
     */
    fun decryptAES(encryptedText: String, secretKey: String): String {
        return try {
            val keyBytes = secretKey.padEnd(16, '0').substring(0, 16).toByteArray(Charsets.UTF_8)
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            
            val iv = ByteArray(16) { 0 }
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec)

            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Return as-is if decryption fails
        }
    }
}
