package kr.co.root.legolas.location.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.permission.LocationAccuracy
import kr.co.root.legolas.location.permission.LocationPermissionStatus
import kr.co.root.legolas.location.permission.appLocationSettingsIntent
import kr.co.root.legolas.location.permission.locationPermissionStatus
import kr.co.root.legolas.location.permission.requiresLocalNetworkPermission
import kr.co.root.legolas.location.permission.systemLocationSettingsIntent
import java.text.DateFormat
import java.util.Date

private enum class LocationTopTab {
    Route,
    Timeline,
    Places,
    Settings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationModuleScreen(
    serverUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = remember { LocationTopTab.entries }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.location_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.location_back),
                            )
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = when (tab) {
                                        LocationTopTab.Route -> stringResource(R.string.location_tab_route)
                                        LocationTopTab.Timeline -> stringResource(R.string.location_tab_timeline)
                                        LocationTopTab.Places -> stringResource(R.string.location_tab_places)
                                        LocationTopTab.Settings -> stringResource(R.string.location_tab_settings)
                                    },
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        when (tabs[selectedTabIndex]) {
            LocationTopTab.Route -> LocationRouteTab(
                serverUrl = serverUrl,
                modifier = Modifier.padding(innerPadding),
            )
            LocationTopTab.Timeline -> LocationEmptyTab(
                title = stringResource(R.string.location_timeline_empty_title),
                description = stringResource(R.string.location_timeline_empty_description),
                modifier = Modifier.padding(innerPadding),
            )
            LocationTopTab.Places -> LocationEmptyTab(
                title = stringResource(R.string.location_places_empty_title),
                description = stringResource(R.string.location_places_empty_description),
                modifier = Modifier.padding(innerPadding),
            )
            LocationTopTab.Settings -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LocationModuleSettings(
                    serverUrl = serverUrl,
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun LocationEmptyTab(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocationModuleSettings(
    serverUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LocationSettingsViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionStatus by remember { mutableStateOf(context.locationPermissionStatus()) }
    val needsLocalNetworkAccess = remember(serverUrl) {
        requiresLocalNetworkPermission(serverUrl)
    }
    var showEnableDisclosure by rememberSaveable { mutableStateOf(false) }
    var showBackgroundAccessGuide by rememberSaveable { mutableStateOf(false) }
    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionStatus = context.locationPermissionStatus()
        viewModel.syncTracking(permissionStatus.canTrackInBackground)
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        permissionStatus = context.locationPermissionStatus()
        viewModel.syncTracking(permissionStatus.canTrackInBackground)
    }

    LifecycleResumeEffect(context) {
        permissionStatus = context.locationPermissionStatus()
        viewModel.syncTracking(permissionStatus.canTrackInBackground)
        onPauseOrDispose { }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.location_tracking),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.location_tracking_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.location_feature)) },
                supportingContent = {
                    Text(
                        if (state.isEnabled) {
                            stringResource(R.string.location_feature_enabled_description)
                        } else {
                            stringResource(R.string.location_feature_off_description)
                        },
                    )
                },
                trailingContent = {
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showEnableDisclosure = true
                            } else {
                                viewModel.setEnabled(false)
                            }
                        },
                        enabled = !state.isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
                            checkedTrackColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    )
                },
                colors = transparentListItemColors(),
            )
            HorizontalDivider()
            StatusItem(
                label = stringResource(R.string.location_collection),
                value = collectionStatus(state.isEnabled, state.isServiceRunning, permissionStatus),
            )
            StatusItem(
                label = stringResource(R.string.location_device_services),
                value = if (permissionStatus.isSystemLocationEnabled) {
                    stringResource(R.string.location_on)
                } else {
                    stringResource(R.string.location_off)
                },
            )
            StatusItem(
                label = stringResource(R.string.location_accuracy),
                value = permissionStatus.accuracyLabel(),
            )
            StatusItem(
                label = stringResource(R.string.background_location),
                value = permissionStatus.backgroundLabel(),
            )
            StatusItem(
                label = stringResource(R.string.location_activity_recognition),
                value = if (permissionStatus.hasActivityRecognitionAccess) {
                    stringResource(R.string.location_allowed)
                } else {
                    stringResource(R.string.location_not_allowed)
                },
            )
            StatusItem(
                label = stringResource(R.string.location_notifications),
                value = if (permissionStatus.hasNotificationAccess) {
                    stringResource(R.string.location_allowed)
                } else {
                    stringResource(R.string.location_not_allowed)
                },
            )
            StatusItem(
                label = stringResource(R.string.location_local_network),
                value = if (!needsLocalNetworkAccess) {
                    stringResource(R.string.location_not_required)
                } else if (permissionStatus.hasLocalNetworkAccess) {
                    stringResource(R.string.location_allowed)
                } else {
                    stringResource(R.string.location_not_allowed)
                },
            )
            StatusItem(
                label = stringResource(R.string.location_last_collected),
                value = state.lastCollectedAtMillis?.let {
                    DateFormat.getDateTimeInstance().format(Date(it))
                } ?: stringResource(R.string.location_never),
            )
            StatusItem(
                label = stringResource(R.string.location_pending_uploads),
                value = state.queuedCount.toString(),
            )
            state.lastError?.let {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.location_last_error)) },
                    supportingContent = { Text(it) },
                    colors = transparentListItemColors(),
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.location_privacy_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.location_privacy_collected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.location_privacy_destination, serverUrl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.location_privacy_control),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isEnabled) {
            when {
                !permissionStatus.hasPreciseAccess -> Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(requiredRuntimePermissions(needsLocalNetworkAccess))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.allow_location))
                }

                !permissionStatus.hasBackgroundAccess -> Button(
                    onClick = { showBackgroundAccessGuide = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.how_to_allow_background_location))
                }

                !permissionStatus.hasActivityRecognitionAccess -> Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(requiredRuntimePermissions(needsLocalNetworkAccess))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.allow_activity_recognition))
                }

                !permissionStatus.hasNotificationAccess -> Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(requiredRuntimePermissions(needsLocalNetworkAccess))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.allow_notifications))
                }

                !permissionStatus.isSystemLocationEnabled -> Button(
                    onClick = { settingsLauncher.launch(systemLocationSettingsIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.turn_on_device_location))
                }

                needsLocalNetworkAccess && !permissionStatus.hasLocalNetworkAccess -> Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(requiredRuntimePermissions(needsLocalNetworkAccess))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.allow_local_network))
                }

                else -> OutlinedButton(
                    onClick = { settingsLauncher.launch(context.appLocationSettingsIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.manage_location_permission))
                }
            }
        }
    }

    if (showEnableDisclosure) {
        AlertDialog(
            onDismissRequest = { showEnableDisclosure = false },
            title = { Text(stringResource(R.string.location_enable_title)) },
            text = { Text(stringResource(R.string.location_enable_disclosure, serverUrl)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableDisclosure = false
                        viewModel.setEnabled(
                            enabled = true,
                            canTrackInBackground = permissionStatus.canTrackInBackground,
                        )
                        if (!permissionStatus.hasPreciseAccess ||
                            !permissionStatus.hasActivityRecognitionAccess ||
                            !permissionStatus.hasNotificationAccess ||
                            (needsLocalNetworkAccess && !permissionStatus.hasLocalNetworkAccess)
                        ) {
                            foregroundPermissionLauncher.launch(
                                requiredRuntimePermissions(needsLocalNetworkAccess),
                            )
                        }
                    },
                ) {
                    Text(stringResource(R.string.location_enable))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnableDisclosure = false }) {
                    Text(stringResource(R.string.location_keep_off))
                }
            },
        )
    }

    if (showBackgroundAccessGuide) {
        AlertDialog(
            onDismissRequest = { showBackgroundAccessGuide = false },
            title = { Text(stringResource(R.string.background_location_guide_title)) },
            text = { Text(stringResource(R.string.background_location_guide_description)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundAccessGuide = false
                        settingsLauncher.launch(context.appLocationSettingsIntent())
                    },
                ) {
                    Text(stringResource(R.string.open_location_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundAccessGuide = false }) {
                    Text(stringResource(R.string.location_not_now))
                }
            },
        )
    }
}

@Composable
private fun StatusItem(label: String, value: String) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
            )
        },
        colors = transparentListItemColors(),
    )
}

@Composable
private fun collectionStatus(
    isEnabled: Boolean,
    isServiceRunning: Boolean,
    permissionStatus: LocationPermissionStatus,
): String = when {
    !isEnabled -> stringResource(R.string.location_collection_off)
    !permissionStatus.hasPreciseAccess -> stringResource(R.string.location_collection_needs_precise)
    !permissionStatus.hasBackgroundAccess -> stringResource(R.string.location_collection_needs_background)
    !permissionStatus.hasActivityRecognitionAccess ->
        stringResource(R.string.location_collection_needs_activity)
    !permissionStatus.hasNotificationAccess ->
        stringResource(R.string.location_collection_needs_notifications)
    !permissionStatus.isSystemLocationEnabled ->
        stringResource(R.string.location_collection_needs_device_location)
    isServiceRunning -> stringResource(R.string.location_collection_active)
    else -> stringResource(R.string.location_collection_starting)
}

private fun requiredRuntimePermissions(needsLocalNetworkAccess: Boolean): Array<String> = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (needsLocalNetworkAccess && Build.VERSION.SDK_INT >= Android17ApiLevel) {
        add(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
}.toTypedArray()

@Composable
private fun LocationPermissionStatus.accuracyLabel(): String = when (accuracy) {
    LocationAccuracy.None -> stringResource(R.string.location_not_allowed)
    LocationAccuracy.Approximate -> stringResource(R.string.location_approximate)
    LocationAccuracy.Precise -> stringResource(R.string.location_precise)
}

@Composable
private fun LocationPermissionStatus.backgroundLabel(): String = when {
    hasBackgroundAccess -> stringResource(R.string.location_always_allowed)
    hasForegroundAccess -> stringResource(R.string.location_while_using)
    else -> stringResource(R.string.location_not_allowed)
}

@Composable
private fun transparentListItemColors() = ListItemDefaults.colors(
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
)

private const val Android17ApiLevel = 37
