package kr.co.root.legolas

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kr.co.root.legolas.home.ui.HomeScreen
import kr.co.root.legolas.location.ui.LocationModuleScreen
import kr.co.root.legolas.pairing.ui.PairingScreen
import kr.co.root.legolas.pairing.ui.PairingUiState
import kr.co.root.legolas.settings.ui.SettingsScreen

internal enum class LegolasDestination {
    Pairing,
    Home,
    Location,
    Settings,
}

internal fun destinationFor(
    isLoading: Boolean,
    serverUrl: String?,
    requestedDestination: LegolasDestination,
): LegolasDestination = when {
    isLoading || serverUrl == null -> LegolasDestination.Pairing
    requestedDestination == LegolasDestination.Pairing -> LegolasDestination.Home
    else -> requestedDestination
}

@Composable
fun LegolasApp(
    state: PairingUiState,
    onScan: () -> Unit,
    onForget: () -> Unit,
) {
    var requestedDestination by rememberSaveable { mutableStateOf(LegolasDestination.Home) }
    val serverUrl = state.serverUrl

    LaunchedEffect(serverUrl) { requestedDestination = LegolasDestination.Home }
    BackHandler(enabled = requestedDestination != LegolasDestination.Home) {
        requestedDestination = LegolasDestination.Home
    }

    when (destinationFor(state.isLoading, serverUrl, requestedDestination)) {
        LegolasDestination.Pairing -> PairingScreen(state, onScan)
        LegolasDestination.Location -> LocationModuleScreen(
            serverUrl = requireNotNull(serverUrl),
            onBack = { requestedDestination = LegolasDestination.Home },
        )
        LegolasDestination.Settings -> SettingsScreen(
            serverUrl = requireNotNull(serverUrl),
            errorMessage = state.errorMessage,
            onBack = { requestedDestination = LegolasDestination.Home },
            onForget = onForget,
        )
        LegolasDestination.Home -> HomeScreen(
            serverUrl = requireNotNull(serverUrl),
            onSettings = { requestedDestination = LegolasDestination.Settings },
            onLocation = { requestedDestination = LegolasDestination.Location },
        )
    }
}
