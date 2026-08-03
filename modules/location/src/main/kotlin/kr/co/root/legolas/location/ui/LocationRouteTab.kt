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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.R
import kr.co.root.legolas.location.data.LocationSample
import kr.co.root.legolas.location.data.LocationSampleQuality
import kr.co.root.legolas.location.data.LocationSampleQuery
import kr.co.root.legolas.location.data.LocationSettingsRepository
import kr.co.root.legolas.location.permission.hasLocationServerAccess
import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class LocationRouteUiState(
    val selectedDate: LocalDate = LocalDate.now(SeoulZone),
    val selectedQuality: LocationSampleQuality? = null,
    val samples: List<LocationSample> = emptyList(),
    val isExternalBasemapEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class LocationRouteViewModel @Inject constructor(
    private val query: LocationSampleQuery,
    private val settingsRepository: LocationSettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LocationRouteUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadSamples()
        viewModelScope.launch {
            settingsRepository.state
                .map { it.externalBasemapEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    mutableUiState.update { it.copy(isExternalBasemapEnabled = enabled) }
                }
        }
    }

    fun moveDate(days: Long) {
        mutableUiState.update { it.copy(selectedDate = it.selectedDate.plusDays(days)) }
        loadSamples()
    }

    fun selectToday() {
        mutableUiState.update { it.copy(selectedDate = LocalDate.now(SeoulZone)) }
        loadSamples()
    }

    fun selectQuality(quality: LocationSampleQuality?) {
        mutableUiState.update { it.copy(selectedQuality = quality) }
        loadSamples()
    }

    fun refresh() = loadSamples()

    fun setExternalBasemapEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setExternalBasemapEnabled(enabled) }
    }

    private fun loadSamples() {
        val date = mutableUiState.value.selectedDate
        val quality = mutableUiState.value.selectedQuality
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val samples = query.samplesOn(date, quality)
                mutableUiState.update {
                    if (it.selectedDate == date && it.selectedQuality == quality) {
                        it.copy(samples = samples, isLoading = false)
                    } else {
                        it
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update {
                    if (it.selectedDate == date && it.selectedQuality == quality) {
                        it.copy(isLoading = false, hasError = true)
                    } else {
                        it
                    }
                }
            }
        }
    }
}

@Composable
internal fun LocationRouteTab(
    serverUrl: String,
    modifier: Modifier = Modifier,
    viewModel: LocationRouteViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBasemapDisclosure by rememberSaveable { mutableStateOf(false) }
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
            RouteDateControls(
                state = state,
                onPrevious = { viewModel.moveDate(-1) },
                onToday = viewModel::selectToday,
                onNext = { viewModel.moveDate(1) },
                onRefresh = viewModel::refresh,
                onQualitySelected = viewModel::selectQuality,
            )
        }

        when {
            !hasServerAccess -> item {
                RouteMessageCard(
                    title = stringResource(R.string.location_route_local_network_title),
                    description = stringResource(R.string.location_route_local_network_description),
                    action = {
                        Button(
                            onClick = {
                                localNetworkPermissionLauncher.launch(
                                    Manifest.permission.ACCESS_LOCAL_NETWORK,
                                )
                            },
                        ) {
                            Text(stringResource(R.string.allow_local_network))
                        }
                    },
                )
            }

            state.hasError -> item {
                RouteMessageCard(
                    title = stringResource(R.string.location_route_error_title),
                    description = stringResource(R.string.location_route_error_description),
                    action = {
                        Button(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.location_retry))
                        }
                    },
                )
            }

            state.isLoading && state.samples.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.samples.isEmpty() -> item {
                RouteMessageCard(
                    title = stringResource(R.string.location_route_no_samples_title),
                    description = stringResource(R.string.location_route_no_samples_description),
                )
            }

            else -> {
                item {
                    LocationRouteMapCard(
                        samples = state.samples,
                        isExternalBasemapEnabled = state.isExternalBasemapEnabled,
                        onEnableExternalBasemap = { showBasemapDisclosure = true },
                        onDisableExternalBasemap = {
                            viewModel.setExternalBasemapEnabled(false)
                        },
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.location_route_sample_count, state.samples.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.samples, key = LocationSample::id) { sample ->
                    LocationSampleCard(sample)
                }
            }
        }
    }

    if (showBasemapDisclosure) {
        ExternalBasemapDisclosureDialog(
            onConfirm = {
                showBasemapDisclosure = false
                viewModel.setExternalBasemapEnabled(true)
            },
            onDismiss = { showBasemapDisclosure = false },
        )
    }
}

@Composable
private fun RouteDateControls(
    state: LocationRouteUiState,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    onQualitySelected: (LocationSampleQuality?) -> Unit,
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QualityChip(
                    label = stringResource(R.string.location_quality_all),
                    selected = state.selectedQuality == null,
                    onClick = { onQualitySelected(null) },
                )
                LocationSampleQuality.entries.forEach { quality ->
                    QualityChip(
                        label = quality.label(),
                        selected = state.selectedQuality == quality,
                        onClick = { onQualitySelected(quality) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun LocationSampleCard(sample: LocationSample) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    String.format(
                        Locale.US,
                        "%.5f, %.5f",
                        sample.latitude,
                        sample.longitude,
                    ),
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date.from(sample.collectedAt)))
                    Text(
                        buildList {
                            sample.horizontalAccuracyM?.let {
                                add(stringResource(R.string.location_accuracy_meters, it))
                            }
                            add(sample.source)
                            sample.activityType?.let(::add)
                            sample.saveReason?.let(::add)
                        }.joinToString(" · "),
                    )
                }
            },
            trailingContent = { Text(sample.quality.label()) },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}

@Composable
private fun RouteMessageCard(
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun LocationSampleQuality.label(): String = stringResource(
    when (this) {
        LocationSampleQuality.GOOD -> R.string.location_quality_good
        LocationSampleQuality.FAIR -> R.string.location_quality_fair
        LocationSampleQuality.BAD -> R.string.location_quality_bad
    },
)

private val SeoulZone: ZoneId = ZoneId.of("Asia/Seoul")
