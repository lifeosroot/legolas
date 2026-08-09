package kr.co.root.legolas.location.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kr.co.root.legolas.location.data.LocationTimelineEntry
import kr.co.root.legolas.location.data.LocationTimelineEntryType
import kr.co.root.legolas.location.permission.hasLocationServerAccess
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun LocationTimelineTab(
    serverUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LocationTimelineViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasServerAccess by remember(serverUrl) {
        mutableStateOf(context.hasLocationServerAccess(serverUrl))
    }
    LifecycleResumeEffect(context, serverUrl) {
        val currentAccess = context.hasLocationServerAccess(serverUrl)
        if (!hasServerAccess && currentAccess) viewModel.refresh()
        hasServerAccess = currentAccess
        onPauseOrDispose { }
    }
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasServerAccess = granted || context.hasLocationServerAccess(serverUrl)
        if (hasServerAccess) viewModel.refresh()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TimelineDateControls(
                state = state,
                onPrevious = { viewModel.moveDate(-1) },
                onToday = viewModel::selectToday,
                onNext = { viewModel.moveDate(1) },
                onRefresh = viewModel::refresh,
            )
        }
        item {
            Text(
                text = stringResource(R.string.location_timeline_private_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            !hasServerAccess -> item {
                TimelineMessageCard(
                    title = stringResource(R.string.location_route_local_network_title),
                    description = stringResource(R.string.location_timeline_local_network_description),
                    action = {
                        Button(
                            onClick = {
                                localNetworkPermissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                            },
                        ) {
                            Text(stringResource(R.string.allow_local_network))
                        }
                    },
                )
            }

            state.hasError -> item {
                TimelineMessageCard(
                    title = stringResource(R.string.location_timeline_error_title),
                    description = stringResource(R.string.location_timeline_error_description),
                    action = {
                        Button(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.location_retry))
                        }
                    },
                )
            }

            state.isLoading && state.entries.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.entries.isEmpty() -> item {
                TimelineMessageCard(
                    title = stringResource(R.string.location_timeline_empty_title),
                    description = stringResource(R.string.location_timeline_empty_description),
                )
            }

            else -> itemsIndexed(
                items = state.entries,
                key = { index, entry -> "${entry.entryType}:${entry.startedAt}:$index" },
            ) { _, entry ->
                TimelineEntryCard(entry)
            }
        }
    }
}

@Composable
private fun TimelineDateControls(
    state: LocationTimelineUiState,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.selectedDate.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.location_refresh),
                    )
                }
            }
            if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onPrevious) {
                    Text(stringResource(R.string.location_previous_day))
                }
                Button(onClick = onToday) {
                    Text(stringResource(R.string.location_today))
                }
                OutlinedButton(onClick = onNext) {
                    Text(stringResource(R.string.location_next_day))
                }
            }
        }
    }
}

@Composable
private fun TimelineEntryCard(entry: LocationTimelineEntry) {
    val unknownPlace = stringResource(R.string.location_timeline_unknown_place)
    val title = when (entry.entryType) {
        LocationTimelineEntryType.PLACE_ENTER -> stringResource(
            R.string.location_timeline_arrived,
            entry.placeName ?: unknownPlace,
        )
        LocationTimelineEntryType.PLACE_EXIT -> stringResource(
            R.string.location_timeline_departed,
            entry.placeName ?: unknownPlace,
        )
        LocationTimelineEntryType.MOVE -> stringResource(
            R.string.location_timeline_moved,
            entry.fromPlaceName ?: unknownPlace,
            entry.toPlaceName ?: unknownPlace,
        )
    }
    val time = entry.endedAt?.let { endedAt ->
        stringResource(
            R.string.location_timeline_time_range,
            TimelineTimeFormatter.format(entry.startedAt),
            TimelineTimeFormatter.format(endedAt),
        )
    } ?: TimelineTimeFormatter.format(entry.startedAt)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(time) },
        )
    }
}

@Composable
private fun TimelineMessageCard(
    title: String,
    description: String,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            action?.invoke()
        }
    }
}

private val TimelineTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(SeoulZone)
