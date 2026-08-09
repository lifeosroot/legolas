package kr.co.root.legolas.feature

import kotlinx.coroutines.flow.first
import kr.co.root.legolas.location.data.LocationServerConfig
import kr.co.root.legolas.location.data.LocationServerConfigProvider
import kr.co.root.legolas.pairing.data.PairingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairedLocationServerConfigProvider @Inject constructor(
    private val pairingRepository: PairingRepository,
) : LocationServerConfigProvider {
    override suspend fun current(): LocationServerConfig? =
        pairingRepository.pairing.first()?.let {
            LocationServerConfig(serverUrl = it.serverUrl, apiKey = it.apiKey)
        }
}
