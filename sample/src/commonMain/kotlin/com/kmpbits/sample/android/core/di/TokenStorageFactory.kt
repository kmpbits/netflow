package com.kmpbits.sample.android.core.di

import com.kmpbits.netflow_core.auth.TokenStorage

/**
 * Builds the platform's secure [TokenStorage]:
 * - iOS  → `SettingsTokenStorage` over `KeychainSettings`
 * - Android → `SettingsTokenStorage` over `EncryptedSharedPreferences`
 *
 * For tests, or an app that wants a fresh login every launch, use
 * `com.kmpbits.netflow_core.auth.InMemoryTokenStorage()` instead — it needs no
 * platform wiring and no `netflow-token-storage` dependency.
 */
internal expect fun createTokenStorage(): TokenStorage
