package com.kmpbits.netflow_core.pinning

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.Security.SecCertificateCreateWithData
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
class SpkiHashTest {

    // Certificate DER, base64-encoded — generated with openssl (see the implementation plan, Task 8 Step 2).
    private val rsaCertDer = "MIIDDTCCAfWgAwIBAgIUQNphXpBaQMlsSCwpsgj0aX0IIOwwDQYJKoZIhvcNAQELBQAwFjEUMBIGA1UEAwwLZml4dHVyZS1yc2EwHhcNMjYwOTA5MTQwMDQ0WhcNMzYwOTA2MTQwMDQ0WjAWMRQwEgYDVQQDDAtmaXh0dXJlLXJzYTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKvcUmrxgJrA+lo7HmlYmEWA4BgyiGEcEPwySlOZcpEBgL4e9UxMJKMzfpBPr6MYqgDqxhv4z6qHcTueN985SdpRGvWyrad/bbfaGbVudD2q7ex+5RNa2SXQr4osApE2YsIyT8EFNzDLYXCRBda0KH7fCprzeNzAEF6ayTGEg+hKU9OT85H5niWww9ITfrMMNo7NXmULU+hyCPQMDACOGsPgES9flTSl10KPRx+Hvg9D2MRUu1MiAS+W+SL58h4vPqBoV22+XkXG/RtzlAeorN830xPUPYmYEUWh4y84XFzISVjWmUiT7X2QLzTBimtXgujLw0Cqk4teG8Dcd254qAMCAwEAAaNTMFEwHQYDVR0OBBYEFOhd+UD9+Rdxr8QDL+9mamTsHCpWMB8GA1UdIwQYMBaAFOhd+UD9+Rdxr8QDL+9mamTsHCpWMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAIiejtCz2d8nuD5ZH1s9K2CRbfBAQGQ5YO0qhruyaBvsHi8HgWlnnZmi5cH9GTQLVPEi89+HQiy+tbfd6EvK3O2kK2w1vDUx+pZhzn85SDYE97l65K4Z6mg4SGajAdDiJRE2tII1S16ATv90TNKmvNF1N9h/eKBkRx9UZV7PbaQYA8NbFOmLtJw5126S9WGbLj0sqUGV+hr3SBL7tUZ39uGNlfLhzurg7u6BPUD9EHd8eBLIPUJDdeCdEfzX+C89eIiDw5EKr/f0ZaiRRv3trGbkiJw0ufiHipwsyq4b0AoYueHElsiQjFPyeUxOnWoUnJZ0HnbCSlqbKkEaoHFCNJ0="
    private val ecCertDer = "MIIBfzCCASWgAwIBAgIUJr3lJLsC4qmt+RdTb4gz0uhIAZEwCgYIKoZIzj0EAwIwFTETMBEGA1UEAwwKZml4dHVyZS1lYzAeFw0yNjA5MDkxNDAwNDRaFw0zNjA5MDYxNDAwNDRaMBUxEzARBgNVBAMMCmZpeHR1cmUtZWMwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAASObhVYafcJ5fXOpZ0ZmtOwpKzLuXnCzrIM3JQyG+myofSt7dQ0FTWLD6lAigL8x54UebNeG8U704xa0+aU/GKio1MwUTAdBgNVHQ4EFgQUz4exNbeeHIQe0XB34uMDFg7JnXwwHwYDVR0jBBgwFoAUz4exNbeeHIQe0XB34uMDFg7JnXwwDwYDVR0TAQH/BAUwAwEB/zAKBggqhkjOPQQDAgNIADBFAiBF57DvCs/jgI4sDXFO8bf3hdcclOcN+n5Smbal8HnupgIhALPNXk/AvzSqK+fEF9EU5p5xjMnb+AobNiXwx8IJowS5"
    private val rsa1024CertDer = "MIICEDCCAXmgAwIBAgIUXPGsrndg0jRgZs6UVRd67XA25I0wDQYJKoZIhvcNAQELBQAwGjEYMBYGA1UEAwwPZml4dHVyZS1yc2ExMDI0MB4XDTI2MDkwOTE0MDA0NFoXDTM2MDkwNjE0MDA0NFowGjEYMBYGA1UEAwwPZml4dHVyZS1yc2ExMDI0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxHBbOgvoaOP7gMec1wRG4yDK3akADFy95Ykg9KvIPgfv+GFEkewmG65ENqIT5q620gC5LuaTTbQN3zw//29yCGQAbZbtWwO02RbhVztw4i1WSb7UjFv3wOY2iLCRzJM3ITpTL4VlGmZGIOSA0hb2IhO4q75PjwWbgRSlMR/4aCQIDAQABo1MwUTAdBgNVHQ4EFgQUZc/wkU0adnNNnV7KhZKTZdymQicwHwYDVR0jBBgwFoAUZc/wkU0adnNNnV7KhZKTZdymQicwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOBgQB6a4Hl/6qigI6uWAxbniXLWL1duAqb1Jfa1KGWobpFmb3/rhwQbUnUUDBRCZetm092pxfZaVYhV0pvSPpC+fo+XyNOBNZyKklv7Kbo7C5eBEd2P1JhMFVAmuwt2KNj5ryDvUcF7q3X57rsuZlDEAWwoyAkRzCxV7DXwrAqpSpqWg=="

    // Expected SPKI hash, computed with openssl — an independent source of truth.
    private val rsaExpected = "sha256/NipRNiaZmkD/Ur4/2RJy7R6Hmt9jEcXgZ2VTJmK+Z6M="
    private val ecExpected = "sha256/fGdF5YMBPSnET7DpHEodUfHGxd9Rnn8tRbmmHxQp/6Q="

    private fun certificate(derBase64: String): platform.Security.SecCertificateRef {
        val der = Base64.decode(derBase64)
        val cfData = der.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), der.size.toLong())
        }
        val cert = SecCertificateCreateWithData(null, cfData)
        CFRelease(cfData)
        return assertNotNull(cert, "the fixture's DER did not produce a SecCertificate")
    }

    @Test
    fun `SPKI hash of an RSA-2048 certificate matches openssl's`() {
        assertEquals(rsaExpected, spkiSha256(certificate(rsaCertDer)))
    }

    @Test
    fun `SPKI hash of an EC P-256 certificate matches openssl's`() {
        assertEquals(ecExpected, spkiSha256(certificate(ecCertDer)))
    }

    @Test
    fun `an unsupported key type returns null instead of a wrong hash`() {
        assertNull(spkiSha256(certificate(rsa1024CertDer)))
    }
}
