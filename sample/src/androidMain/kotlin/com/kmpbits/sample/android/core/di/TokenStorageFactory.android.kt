package com.kmpbits.sample.android.core.di

import android.annotation.SuppressLint
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kmpbits.netflow_core.auth.TokenStorage
import com.kmpbits.netflow_token_storage.SettingsTokenStorage
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Set once from `App.onCreate()` — an expect/actual factory can't take a
 * `Context` parameter, so the app captures it here.
 */
object SampleAppContext {
    @SuppressLint("StaticFieldLeak")
    lateinit var applicationContext: Context
}

internal actual fun createTokenStorage(): TokenStorage {
    val context = SampleAppContext.applicationContext
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val prefs = EncryptedSharedPreferences.create(
        context,
        "netflow_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    return SettingsTokenStorage(SharedPreferencesSettings(prefs))
}
