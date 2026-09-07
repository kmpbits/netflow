package com.kmpbits.netflow_core.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalEncodingApi::class)
private val b64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

/** Builds a fake unsigned JWT with the given payload JSON. */
@OptIn(ExperimentalEncodingApi::class)
internal fun fakeJwt(payloadJson: String): String {
    val header = b64.encode("""{"alg":"none"}""".encodeToByteArray())
    val payload = b64.encode(payloadJson.encodeToByteArray())
    return "$header.$payload."
}

internal fun jwtExpiringAt(epochSeconds: Long): String = fakeJwt("""{"sub":"u1","exp":$epochSeconds}""")

class JwtTest {

    @Test
    fun `reads the exp claim`() {
        assertEquals(1_900_000_000L, Jwt.expiresAtEpochSeconds(jwtExpiringAt(1_900_000_000L)))
    }

    @Test
    fun `returns null when there is no exp claim`() {
        assertNull(Jwt.expiresAtEpochSeconds(fakeJwt("""{"sub":"u1"}""")))
    }

    @Test
    fun `returns null for an opaque non-JWT token`() {
        assertNull(Jwt.expiresAtEpochSeconds("opaque-access-token-12345"))
    }

    @Test
    fun `returns null for a malformed payload segment`() {
        assertNull(Jwt.expiresAtEpochSeconds("aaa.@@@not-base64@@@.bbb"))
    }

    @Test
    fun `returns null when exp is not a number`() {
        assertNull(Jwt.expiresAtEpochSeconds(fakeJwt("""{"exp":"soon"}""")))
    }
}
