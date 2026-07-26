package kr.co.root.legolas

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kr.co.root.legolas.home.ui.HomeScreen
import kr.co.root.legolas.pairing.ui.PairingScreen
import kr.co.root.legolas.pairing.ui.PairingUiState
import kr.co.root.legolas.settings.ui.SettingsScreen

internal enum class LegolasDestination {
    Pairing,
    Home,
    Settings,
}

internal fun destinationFor(
    isLoading: Boolean,
    serverUrl: String?,
    showSettings: Boolean,
): LegolasDestination = when {
    isLoading || serverUrl == null -> LegolasDestination.Pairing
    showSettings -> LegolasDestination.Settings
    else -> LegolasDestination.Home
}

@Composable
fun LegolasApp(
    state: PairingUiState,
    onScan: () -> Unit,
    onForget: () -> Unit,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val serverUrl = state.serverUrl

    LaunchedEffect(serverUrl) { showSettings = false }
    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    when (destinationFor(state.isLoading, serverUrl, showSettings)) {
        LegolasDestination.Pairing -> PairingScreen(state, onScan)
        LegolasDestination.Settings -> SettingsScreen(
            serverUrl = requireNotNull(serverUrl),
            errorMessage = state.errorMessage,
            onBack = { showSettings = false },
            onForget = onForget,
        )
        LegolasDestination.Home -> HomeScreen(onSettings = { showSettings = true })
    }
}
