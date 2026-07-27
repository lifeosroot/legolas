package kr.co.root.legolas.pairing.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kr.co.root.legolas.location.data.LocationServerConfig
import kr.co.root.legolas.location.data.LocationServerConfigProvider
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

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationServerConfigModule {
    @Binds
    abstract fun bindLocationServerConfigProvider(
        provider: PairedLocationServerConfigProvider,
    ): LocationServerConfigProvider
}
