package kr.co.root.legolas.location.ui

import android.Manifest
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
            LocationTopTab.Route -> LocationEmptyTab(
                title = stringResource(R.string.location_route_empty_title),
                description = stringResource(R.string.location_route_empty_description),
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
    var showEnableDisclosure by rememberSaveable { mutableStateOf(false) }
    var showBackgroundAccessGuide by rememberSaveable { mutableStateOf(false) }
    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionStatus = context.locationPermissionStatus()
    }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        permissionStatus = context.locationPermissionStatus()
    }

    LifecycleResumeEffect(context) {
        permissionStatus = context.locationPermissionStatus()
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
                value = collectionStatus(state.isEnabled, permissionStatus),
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
                label = stringResource(R.string.location_last_collected),
                value = stringResource(R.string.location_never),
            )
            StatusItem(
                label = stringResource(R.string.location_pending_uploads),
                value = "0",
            )
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
                !permissionStatus.hasForegroundAccess -> Button(
                    onClick = {
                        foregroundPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
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
                        viewModel.setEnabled(true)
                        if (!permissionStatus.hasForegroundAccess) {
                            foregroundPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
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
    permissionStatus: LocationPermissionStatus,
): String = when {
    !isEnabled -> stringResource(R.string.location_collection_off)
    !permissionStatus.hasForegroundAccess -> stringResource(R.string.location_collection_needs_permission)
    !permissionStatus.hasBackgroundAccess -> stringResource(R.string.location_collection_needs_background)
    else -> stringResource(R.string.location_collection_ready)
}

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
