package com.duesoon.app.core.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "duesoon_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveAuthToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun saveDerivedMasterKey(key: SecretKey) {
        val encoded = Base64.encodeToString(key.encoded, Base64.DEFAULT)
        sharedPreferences.edit().putString("master_key", encoded).apply()
    }

    fun getDerivedMasterKey(): SecretKey? {
        val encoded = sharedPreferences.getString("master_key", null) ?: return null
        val decoded = Base64.decode(encoded, Base64.DEFAULT)
        return SecretKeySpec(decoded, "AES")
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
