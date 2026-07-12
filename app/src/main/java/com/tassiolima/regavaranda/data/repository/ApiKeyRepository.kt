package com.tassiolima.regavaranda.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tassiolima.regavaranda.data.model.AiProvider

/**
 * Guarda a chave de cada provedor de IA criptografada no dispositivo (nunca sai do
 * aparelho, exceto nas chamadas diretas às APIs feitas pelo próprio app).
 */
class ApiKeyRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getKey(provider: AiProvider): String? =
        prefs.getString(keyFor(provider), null)?.takeIf { it.isNotBlank() }

    fun setKey(provider: AiProvider, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrBlank()) remove(keyFor(provider)) else putString(keyFor(provider), value.trim())
        }.apply()
    }

    fun getSelectedProvider(): AiProvider =
        prefs.getString(KEY_SELECTED_PROVIDER, null)?.let { name ->
            AiProvider.entries.firstOrNull { it.name == name }
        } ?: AiProvider.ANTHROPIC

    fun setSelectedProvider(provider: AiProvider) {
        prefs.edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
    }

    private fun keyFor(provider: AiProvider) = "api_key_${provider.name}"

    companion object {
        private const val KEY_SELECTED_PROVIDER = "selected_ai_provider"
    }
}
