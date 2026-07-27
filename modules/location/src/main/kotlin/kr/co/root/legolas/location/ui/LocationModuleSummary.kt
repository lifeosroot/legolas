package kr.co.root.legolas.location.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.permission.locationPermissionStatus
import kr.co.root.legolas.location.permission.requiresLocalNetworkPermission

internal enum class LocationModuleStatus {
    Checking,
    Off,
    ActionRequired,
    Ready,
}

internal fun locationModuleStatus(
    isLoading: Boolean,
    isEnabled: Boolean,
    canTrackInBackground: Boolean,
    canAccessPairedServer: Boolean = true,
    isServiceRunning: Boolean = true,
    hasError: Boolean = false,
): LocationModuleStatus = when {
    isLoading -> LocationModuleStatus.Checking
    !isEnabled -> LocationModuleStatus.Off
    !canTrackInBackground -> LocationModuleStatus.ActionRequired
    !canAccessPairedServer -> LocationModuleStatus.ActionRequired
    hasError -> LocationModuleStatus.ActionRequired
    !isServiceRunning -> LocationModuleStatus.Checking
    else -> LocationModuleStatus.Ready
}

@Composable
fun LocationModuleSummary(
    serverUrl: String,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationSettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionStatus by remember { mutableStateOf(context.locationPermissionStatus()) }
    val status = locationModuleStatus(
        isLoading = state.isLoading,
        isEnabled = state.isEnabled,
        canTrackInBackground = permissionStatus.canTrackInBackground,
        canAccessPairedServer = !requiresLocalNetworkPermission(serverUrl) ||
            permissionStatus.hasLocalNetworkAccess,
        isServiceRunning = state.isServiceRunning,
        hasError = state.lastError != null,
    )

    LifecycleResumeEffect(context) {
        permissionStatus = context.locationPermissionStatus()
        viewModel.syncTracking(permissionStatus.canTrackInBackground)
        onPauseOrDispose { }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.location_tracking),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = when (status) {
                            LocationModuleStatus.Checking -> stringResource(R.string.location_status_checking)
                            LocationModuleStatus.Off -> stringResource(R.string.location_feature_off)
                            LocationModuleStatus.ActionRequired -> stringResource(R.string.location_home_action_required_description)
                            LocationModuleStatus.Ready -> stringResource(R.string.location_home_ready_description)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LocationStatusBadge(status)
            }
            OutlinedButton(
                onClick = onManage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.location_manage))
            }
        }
    }
}

@Composable
private fun LocationStatusBadge(status: LocationModuleStatus) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor) = when (status) {
        LocationModuleStatus.Checking -> colorScheme.surface to colorScheme.onSurfaceVariant
        LocationModuleStatus.Off -> colorScheme.surface to colorScheme.onSurfaceVariant
        LocationModuleStatus.ActionRequired -> colorScheme.errorContainer to colorScheme.onErrorContainer
        LocationModuleStatus.Ready -> colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = when (status) {
                LocationModuleStatus.Checking -> stringResource(R.string.location_status_checking_badge)
                LocationModuleStatus.Off -> stringResource(R.string.location_status_off)
                LocationModuleStatus.ActionRequired -> stringResource(R.string.location_status_action_required)
                LocationModuleStatus.Ready -> stringResource(R.string.location_status_ready)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
