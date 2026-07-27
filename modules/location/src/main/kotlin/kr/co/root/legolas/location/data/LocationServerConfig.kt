package kr.co.root.legolas.location.data

data class LocationServerConfig(
    val serverUrl: String,
    val apiKey: String,
)

interface LocationServerConfigProvider {
    suspend fun current(): LocationServerConfig?
}
