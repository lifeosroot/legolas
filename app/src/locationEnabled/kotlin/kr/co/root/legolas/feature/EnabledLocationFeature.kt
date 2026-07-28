package kr.co.root.legolas.feature

import androidx.compose.runtime.Composable
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kr.co.root.legolas.location.data.LocationServerConfig
import kr.co.root.legolas.location.data.LocationServerConfigProvider
import kr.co.root.legolas.location.tracking.LocationTrackingCommander
import kr.co.root.legolas.location.ui.LocationModuleScreen
import kr.co.root.legolas.location.ui.LocationModuleSummary
import kr.co.root.legolas.pairing.data.PairingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnabledLocationFeature @Inject constructor(
    private val commander: LocationTrackingCommander,
) : LocationFeature {
    override val isAvailable = true

    @Composable
    override fun Summary(serverUrl: String, onOpen: () -> Unit) {
        LocationModuleSummary(serverUrl = serverUrl, onManage = onOpen)
    }

    @Composable
    override fun Screen(serverUrl: String, onBack: () -> Unit) {
        LocationModuleScreen(serverUrl = serverUrl, onBack = onBack)
    }

    override suspend fun disableAndClear() = commander.disableAndClear()
}

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
abstract class EnabledLocationFeatureModule {
    @Binds
    abstract fun bindLocationFeature(feature: EnabledLocationFeature): LocationFeature

    @Binds
    abstract fun bindLocationServerConfigProvider(
        provider: PairedLocationServerConfigProvider,
    ): LocationServerConfigProvider
}
