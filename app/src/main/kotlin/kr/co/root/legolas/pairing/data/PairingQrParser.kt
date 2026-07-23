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

        require(server.scheme == "http" || server.scheme == "https")
        require(server.host != null)
        require(apiKey.startsWith("arwen_") && apiKey.length > "arwen_".length)

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
}
