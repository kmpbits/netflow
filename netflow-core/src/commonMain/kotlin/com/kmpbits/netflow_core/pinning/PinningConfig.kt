package com.kmpbits.netflow_core.pinning

import com.kmpbits.netflow_core.annotations.NetFlowMarker
import com.kmpbits.netflow_core.exceptions.NetFlowException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

internal data class Pin(val pattern: String, val hashes: List<String>)

/**
 * Certificate pinning por hash SHA-256 da chave pública (SPKI).
 *
 * ```kotlin
 * netflowClient {
 *     pinning {
 *         pin("api.exemplo.com", "sha256/AAAA…=", "sha256/BBBB…=")
 *         pin("*.exemplo.com", "sha256/CCCC…=")
 *     }
 * }
 * ```
 *
 * Hosts sem pin declarado não são afectados — mantêm a validação normal do
 * sistema. Uma falha de pin faz o pedido falhar; não há modo report-only.
 *
 * **Declara sempre um pin de backup** para a próxima chave: sem ele, a rotação
 * de certificado deixa as apps instaladas sem conseguir ligar.
 */
@NetFlowMarker
class PinningConfig internal constructor() {

    internal val pins: MutableList<Pin> = mutableListOf()

    /**
     * @param host `api.exemplo.com` ou `*.exemplo.com` (exactamente um nível de
     * subdomínio). O padrão `**.` do OkHttp não é suportado.
     * @param sha256Hashes um ou mais hashes na forma `sha256/<base64 de 32 bytes>`.
     */
    fun pin(host: String, vararg sha256Hashes: String) {
        pins.add(Pin(host, sha256Hashes.toList()))
    }
}

private const val PREFIX = "sha256/"

/** Falha cedo, na construção do cliente, em vez de no primeiro pedido. */
@OptIn(ExperimentalEncodingApi::class)
internal fun PinningConfig.validate() {
    pins.forEach { pin ->
        if (pin.pattern.isBlank()) {
            throw NetFlowException("Pinning: o host não pode ser vazio")
        }
        if (pin.hashes.isEmpty()) {
            throw NetFlowException("Pinning: '${pin.pattern}' não tem nenhum pin declarado")
        }
        pin.hashes.forEach { hash ->
            if (!hash.startsWith(PREFIX)) {
                throw NetFlowException("Pinning: '$hash' tem de começar por '$PREFIX'")
            }
            val decoded = try {
                Base64.decode(hash.removePrefix(PREFIX))
            } catch (e: IllegalArgumentException) {
                throw NetFlowException("Pinning: '$hash' não é base64 válido")
            }
            if (decoded.size != 32) {
                throw NetFlowException("Pinning: '$hash' descodifica para ${decoded.size} bytes; um SHA-256 tem 32")
            }
        }
    }
}

/**
 * Semântica replicada do `CertificatePinner` do OkHttp: `*.exemplo.com` casa com
 * exactamente um nível de subdomínio, e não com o domínio base.
 */
internal fun matchesPattern(pattern: String, host: String): Boolean {
    if (!pattern.startsWith("*.")) return pattern.equals(host, ignoreCase = true)

    val suffix = pattern.substring(1) // ".exemplo.com"
    if (!host.endsWith(suffix, ignoreCase = true)) return false
    if (host.length <= suffix.length) return false

    val label = host.dropLast(suffix.length)
    return label.isNotEmpty() && !label.contains('.')
}

/** Todos os hashes declarados para [host], de todos os padrões que casam. Vazio = host não fixado. */
internal fun PinningConfig.hashesFor(host: String): List<String> =
    pins.filter { matchesPattern(it.pattern, host) }.flatMap { it.hashes }
