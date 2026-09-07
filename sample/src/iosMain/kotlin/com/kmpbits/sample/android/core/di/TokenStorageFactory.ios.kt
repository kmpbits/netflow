package com.kmpbits.sample.android.core.di

import com.kmpbits.netflow_core.auth.TokenStorage
import com.kmpbits.netflow_token_storage.SettingsTokenStorage
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

@OptIn(ExperimentalSettingsImplementation::class)
internal actual fun createTokenStorage(): TokenStorage =
    SettingsTokenStorage(KeychainSettings(service = "com.kmpbits.netflowSample.auth"))
