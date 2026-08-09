package kr.co.root.legolas.location.data

interface LocationServerConfigProvider {
    suspend fun current(): LocationServerConfig?
}
