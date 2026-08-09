package kr.co.root.legolas.feature

import androidx.compose.runtime.Composable
import kr.co.root.legolas.location.tracking.LocationTrackingCommander
import kr.co.root.legolas.location.ui.LocationModuleScreen
import kr.co.root.legolas.location.ui.LocationModuleSummary
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
