package com.kmpbits.netflow_token_storage

import com.kmpbits.netflow_core.auth.BearerTokens
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsTokenStorageTest {

    @Test
    fun `load returns null when nothing stored`() = runTest {
        val storage = SettingsTokenStorage(MapSettings())
        assertNull(storage.load())
    }

    @Test
    fun `save then load round-trips both tokens`() = runTest {
        val storage = SettingsTokenStorage(MapSettings())
        storage.save(BearerTokens("access-1", "refresh-1"))
        assertEquals(BearerTokens("access-1", "refresh-1"), storage.load())
    }

    @Test
    fun `save then load round-trips a null refresh token`() = runTest {
        val storage = SettingsTokenStorage(MapSettings())
        storage.save(BearerTokens("access-only"))
        assertEquals(BearerTokens("access-only", null), storage.load())
    }

    @Test
    fun `clear removes the stored blob`() = runTest {
        val settings = MapSettings()
        val storage = SettingsTokenStorage(settings)
        storage.save(BearerTokens("a", "b"))
        storage.clear()
        assertNull(storage.load())
        assertNull(settings.getStringOrNull(SettingsTokenStorage.DEFAULT_KEY))
    }

    @Test
    fun `writes under the default key`() = runTest {
        val settings = MapSettings()
        SettingsTokenStorage(settings).save(BearerTokens("a", "b"))
        assertEquals(true, settings.getStringOrNull("netflow.auth.tokens") != null)
    }

    @Test
    fun `honours a custom key`() = runTest {
        val settings = MapSettings()
        SettingsTokenStorage(settings, key = "my.tokens").save(BearerTokens("a", "b"))
        assertEquals(true, settings.getStringOrNull("my.tokens") != null)
        assertNull(settings.getStringOrNull(SettingsTokenStorage.DEFAULT_KEY))
    }

    @Test
    fun `corrupt stored value is treated as empty`() = runTest {
        val settings = MapSettings()
        settings.putString(SettingsTokenStorage.DEFAULT_KEY, "not json")
        assertNull(SettingsTokenStorage(settings).load())
    }
}
