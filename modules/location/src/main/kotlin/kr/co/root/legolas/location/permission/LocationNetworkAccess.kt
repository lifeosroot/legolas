package kr.co.root.legolas.location.permission

import java.net.URI

internal fun requiresLocalNetworkPermission(serverUrl: String): Boolean {
    val host = runCatching {
        URI.create(serverUrl).host
            ?.lowercase()
            ?.removePrefix("[")
            ?.removeSuffix("]")
    }.getOrNull() ?: return false
    if (host == "localhost" || host.endsWith(".local") || host.endsWith(".home.arpa")) return true
    if (host == "::1" || host.startsWith("fe80:") || host.startsWith("fc") || host.startsWith("fd")) {
        return true
    }
    val octets = host.split('.').mapNotNull(String::toIntOrNull)
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false
    return octets[0] == 10 ||
        octets[0] == 127 ||
        (octets[0] == 169 && octets[1] == 254) ||
        (octets[0] == 172 && octets[1] in 16..31) ||
        (octets[0] == 192 && octets[1] == 168)
}
