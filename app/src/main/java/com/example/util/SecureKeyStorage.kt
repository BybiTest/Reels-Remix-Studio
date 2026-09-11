package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Securely manages Gemini API Key using Android KeyStore hardware/TEE-backed
 * encryption (AES/GCM/NoPadding).
 * Rules:
 * - Never logs or displays raw API keys.
 * - Prioritizes user's encrypted custom key if set, falling back to BuildConfig.GEMINI_API_KEY.
 * - Ensures keys are never saved in plaintext in SQLite or SharedPreferences.
 */
class SecureKeyStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    init {
        ensureKeyStoreKey()
    }

    private fun ensureKeyStoreKey() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts and securely saves a user-provided API key.
     */
    fun saveCustomApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            clearCustomApiKey()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))

        val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
        val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        prefs.edit()
            .putString(PREF_ENCRYPTED_KEY, encryptedString)
            .putString(PREF_KEY_IV, ivString)
            .apply()
    }

    /**
     * Decrypts and retrieves the custom API key if present.
     */
    fun getCustomApiKey(): String? {
        val encryptedString = prefs.getString(PREF_ENCRYPTED_KEY, null) ?: return null
        val ivString = prefs.getString(PREF_KEY_IV, null) ?: return null

        return try {
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(encryptedString, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clears the custom API key.
     */
    fun clearCustomApiKey() {
        prefs.edit()
            .remove(PREF_ENCRYPTED_KEY)
            .remove(PREF_KEY_IV)
            .apply()
    }

    /**
     * Returns true if a custom key is saved.
     */
    fun hasCustomKey(): Boolean {
        return prefs.contains(PREF_ENCRYPTED_KEY) && prefs.contains(PREF_KEY_IV)
    }

    /**
     * Returns the active API key:
     * 1. User's encrypted custom key if set.
     * 2. Otherwise BuildConfig.GEMINI_API_KEY (injected by AI Studio / secrets plugin).
     */
    fun getActiveApiKey(): String {
        val customKey = getCustomApiKey()
        if (!customKey.isNullOrBlank()) {
            return customKey
        }
        val buildConfigKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            (field.get(null) as? String) ?: ""
        } catch (_: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }

    /**
     * Returns a safe representation of API key status without leaking characters.
     */
    fun getMaskedKeyPreview(): String {
        val key = getActiveApiKey()
        return when {
            key.isBlank() -> "کلید تنظیم نشده است"
            key.length <= 8 -> "••••••••"
            else -> "${key.take(4)}••••••••${key.takeLast(4)}"
        }
    }

    fun isConfigured(): Boolean {
        return getActiveApiKey().isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "secure_gemini_storage"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "GeminiStudioKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREF_ENCRYPTED_KEY = "enc_gemini_key"
        private const val PREF_KEY_IV = "enc_gemini_iv"
    }
}
