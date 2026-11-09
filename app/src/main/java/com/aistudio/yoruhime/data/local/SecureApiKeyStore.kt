package com.aistudio.yoruhime.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the user-supplied provider key with an AES-GCM key held by Android Keystore.
 * Only ciphertext and the per-value IV are written to app-private preferences.
 */
class SecureApiKeyStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("yoruhime_secure_settings", Context.MODE_PRIVATE)

    fun saveApiKey(value: String) {
        if (value.isBlank()) {
            preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun readApiKey(): String {
        val ivText = preferences.getString(KEY_IV, null) ?: return ""
        val ciphertextText = preferences.getString(KEY_CIPHERTEXT, null) ?: return ""
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(ciphertextText, Base64.NO_WRAP)),
                Charsets.UTF_8
            )
        } catch (_: Exception) {
            // A reset/invalidated Keystore key must never crash app startup.
            preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).apply()
            ""
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "yoruhime_openrouter_api_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_IV = "api_key_iv"
        const val KEY_CIPHERTEXT = "api_key_ciphertext"
    }
}
