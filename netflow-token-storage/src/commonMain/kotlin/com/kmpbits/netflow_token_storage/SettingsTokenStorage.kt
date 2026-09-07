package com.kmpbits.netflow_token_storage

import com.kmpbits.netflow_core.auth.BearerTokens
import com.kmpbits.netflow_core.auth.TokenStorage
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A [TokenStorage] backed by a `multiplatform-settings` [Settings] instance. The
 * two tokens are stored as a single JSON blob under [key].
 *
 * This class adds no encryption. Pass a [Settings] backed by a secure store:
 * `KeychainSettings` on iOS, an `EncryptedSharedPreferences`-backed
 * `SharedPreferencesSettings` on Android. See the README for a full wiring
 * example.
 *
 * @param settings the backing store.
 * @param key the settings key the token blob is written under.
 */
class SettingsTokenStorage(
    private val settings: Settings,
    private val key: String = DEFAULT_KEY,
) : TokenStorage {

    override suspend fun load(): BearerTokens? {
        val raw = settings.getStringOrNull(key) ?: return null
        val stored = runCatching { json.decodeFromString<StoredTokens>(raw) }.getOrNull() ?: return null
        return BearerTokens(stored.accessToken, stored.refreshToken)
    }

    override suspend fun save(tokens: BearerTokens) {
        val raw = json.encodeToString(StoredTokens(tokens.accessToken, tokens.refreshToken))
        settings.putString(key, raw)
    }

    override suspend fun clear() {
        settings.remove(key)
    }

    @Serializable
    private data class StoredTokens(
        val accessToken: String,
        val refreshToken: String? = null,
    )

    companion object {
        const val DEFAULT_KEY: String = "netflow.auth.tokens"

        private val json = Json { ignoreUnknownKeys = true }
    }
}
