package kr.co.root.legolas.pairing.data

import kr.co.root.legolas.pairing.model.PairingConfig
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object PairingQrParser {
    fun parse(value: String): PairingConfig {
        val uri = URI.create(value)
        require(uri.scheme == "legolas" && uri.host == "pair")

        val serverUrl = requireNotNull(uri.queryParameter("server"))
        val apiKey = requireNotNull(uri.queryParameter("apiKey"))
        val server = URI.create(serverUrl)

        val scheme = server.scheme?.lowercase()
        val host = server.host?.lowercase()

        require(host != null)
        require(
            scheme == "https" ||
                scheme == "http" && (host in CleartextLocalHosts || host.isPrivateIpv4Address()),
        )
        require(server.userInfo == null && server.rawQuery == null && server.rawFragment == null)
        require(server.port == -1 || server.port in 1..65_535)
        require(apiKey.startsWith("arwen_") && apiKey.length in 7..128)
        require(apiKey.all { it.isLetterOrDigit() || it == '_' || it == '-' })

        return PairingConfig(
            serverUrl = serverUrl.trimEnd('/'),
            apiKey = apiKey,
        )
    }

    private fun URI.queryParameter(name: String): String? = rawQuery
        ?.split('&')
        ?.asSequence()
        ?.map { it.split('=', limit = 2) }
        ?.firstOrNull { it.size == 2 && it[0] == name }
        ?.get(1)
        ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }

    private fun String.isPrivateIpv4Address(): Boolean {
        val octets = split('.').map { it.toIntOrNull() ?: return false }
        if (octets.size != 4 || octets.any { it !in 0..255 }) return false
        return octets[0] == 10 ||
            octets[0] == 172 && octets[1] in 16..31 ||
            octets[0] == 192 && octets[1] == 168
    }

    private val CleartextLocalHosts = setOf("localhost", "127.0.0.1", "::1")
}
