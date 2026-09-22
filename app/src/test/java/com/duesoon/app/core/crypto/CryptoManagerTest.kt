package com.duesoon.app.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class CryptoManagerTest {

    private val cryptoManager = CryptoManager()

    @Test
    fun `deriveMasterKey produces deterministic key`() {
        val passphrase = "MySuperSecretPassword".toCharArray()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val key1 = cryptoManager.deriveMasterKey(passphrase, salt)
        val key2 = cryptoManager.deriveMasterKey(passphrase, salt)

        assertArrayEquals("Derived keys with same salt and password should match", key1.encoded, key2.encoded)
    }

    @Test
    fun `deriveAccountAuthToken produces deterministic token`() {
        val passphrase = "MySuperSecretPassword".toCharArray()
        val salt = ByteArray(16)
        val masterKey = cryptoManager.deriveMasterKey(passphrase, salt)

        val token1 = cryptoManager.deriveAccountAuthToken(masterKey)
        val token2 = cryptoManager.deriveAccountAuthToken(masterKey)

        assertEquals("Tokens derived from the same master key should match", token1, token2)
        assertEquals("Token should be 64 characters long (SHA-256 hex string)", 64, token1.length)
    }

    @Test
    fun `encrypt and decrypt roundtrip`() {
        val passphrase = "MySuperSecretPassword".toCharArray()
        val salt = ByteArray(16)
        val masterKey = cryptoManager.deriveMasterKey(passphrase, salt)

        val plaintext = "This is a secret task title!".toByteArray(Charsets.UTF_8)
        
        val ciphertextWithIv = cryptoManager.encrypt(plaintext, masterKey)
        
        // Ensure ciphertext is different from plaintext and contains IV
        assertNotEquals(plaintext.size, ciphertextWithIv.size)
        
        val decryptedBytes = cryptoManager.decrypt(ciphertextWithIv, masterKey)
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)
        
        assertEquals("Decrypted text should match original plaintext", "This is a secret task title!", decryptedText)
    }

    @Test
    fun `decrypt throws exception on tampered payload`() {
        val passphrase = "MySuperSecretPassword".toCharArray()
        val salt = ByteArray(16)
        val masterKey = cryptoManager.deriveMasterKey(passphrase, salt)

        val plaintext = "This is a secret task title!".toByteArray(Charsets.UTF_8)
        val ciphertextWithIv = cryptoManager.encrypt(plaintext, masterKey)
        
        // Tamper with the ciphertext (change the last byte, which is part of the GCM authentication tag)
        ciphertextWithIv[ciphertextWithIv.size - 1] = (ciphertextWithIv[ciphertextWithIv.size - 1] + 1).toByte()

        assertThrows(AEADBadTagException::class.java) {
            cryptoManager.decrypt(ciphertextWithIv, masterKey)
        }
    }

    @Test
    fun `decrypt throws exception on tampered IV`() {
        val passphrase = "MySuperSecretPassword".toCharArray()
        val salt = ByteArray(16)
        val masterKey = cryptoManager.deriveMasterKey(passphrase, salt)

        val plaintext = "This is a secret task title!".toByteArray(Charsets.UTF_8)
        val ciphertextWithIv = cryptoManager.encrypt(plaintext, masterKey)
        
        // Tamper with the IV (first 12 bytes)
        ciphertextWithIv[0] = (ciphertextWithIv[0] + 1).toByte()

        assertThrows(AEADBadTagException::class.java) {
            cryptoManager.decrypt(ciphertextWithIv, masterKey)
        }
    }
}
