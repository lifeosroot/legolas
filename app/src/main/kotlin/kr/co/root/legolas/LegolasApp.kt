package kr.co.root.legolas

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kr.co.root.legolas.feature.LocationFeature
import kr.co.root.legolas.home.ui.HomeScreen
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
    isLocationAvailable: Boolean = true,
): LegolasDestination = when {
    isLoading || serverUrl == null -> LegolasDestination.Pairing
    requestedDestination == LegolasDestination.Pairing -> LegolasDestination.Home
    requestedDestination == LegolasDestination.Location && !isLocationAvailable ->
        LegolasDestination.Home
    else -> requestedDestination
}

@Composable
fun LegolasApp(
    state: PairingUiState,
    locationFeature: LocationFeature,
    onScan: () -> Unit,
    onForget: () -> Unit,
    onDismissHealthWarning: () -> Unit,
) {
    var requestedDestination by rememberSaveable { mutableStateOf(LegolasDestination.Home) }
    val serverUrl = state.serverUrl

    LaunchedEffect(serverUrl) { requestedDestination = LegolasDestination.Home }
    BackHandler(enabled = requestedDestination != LegolasDestination.Home) {
        requestedDestination = LegolasDestination.Home
    }

    when (
        destinationFor(
            state.isLoading,
            serverUrl,
            requestedDestination,
            locationFeature.isAvailable,
        )
    ) {
        LegolasDestination.Pairing -> PairingScreen(state, onScan)
        LegolasDestination.Location -> locationFeature.Screen(
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
            onSettings = { requestedDestination = LegolasDestination.Settings },
        ) {
            locationFeature.Summary(
                serverUrl = requireNotNull(serverUrl),
                onOpen = { requestedDestination = LegolasDestination.Location },
            )
        }
    }

    if (state.shouldSuggestLogout && serverUrl != null) {
        AlertDialog(
            onDismissRequest = onDismissHealthWarning,
            title = { Text(stringResource(R.string.arwen_unreachable_title)) },
            text = {
                Text(stringResource(R.string.arwen_unreachable_message, serverUrl))
            },
            confirmButton = {
                TextButton(onClick = onForget) {
                    Text(stringResource(R.string.sign_out))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissHealthWarning) {
                    Text(stringResource(R.string.stay_signed_in))
                }
            },
        )
    }
}
