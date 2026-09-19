package com.duesoon.app.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val IV_LENGTH_BYTES = 12
        private const val TAG_LENGTH_BIT = 128
        private const val ITERATION_COUNT = 120_000
        private const val KEY_LENGTH = 256
    }

    fun deriveMasterKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, "AES")
    }

    fun deriveAccountAuthToken(masterKey: SecretKey): String {
        // Derive token via SHA-256 hash of the master key
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(masterKey.encoded)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun encrypt(plaintext: ByteArray, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val ciphertext = cipher.doFinal(plaintext)
        
        // Prepend IV to ciphertext
        val result = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
        
        return result
    }

    fun decrypt(ciphertextWithIv: ByteArray, secretKey: SecretKey): ByteArray {
        if (ciphertextWithIv.size < IV_LENGTH_BYTES) {
            throw IllegalArgumentException("Invalid ciphertext: too short to contain IV")
        }
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTES)
        System.arraycopy(ciphertextWithIv, 0, iv, 0, IV_LENGTH_BYTES)
        
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        val actualCiphertext = ByteArray(ciphertextWithIv.size - IV_LENGTH_BYTES)
        System.arraycopy(ciphertextWithIv, IV_LENGTH_BYTES, actualCiphertext, 0, actualCiphertext.size)
        
        return cipher.doFinal(actualCiphertext)
    }
}
