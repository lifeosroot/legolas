package kr.co.root.legolas.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.data.LocationSettingsRepository
import kr.co.root.legolas.location.tracking.LocationTrackingCommander
import javax.inject.Inject

data class LocationSettingsUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val isExternalBasemapEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val motion: String = "unknown",
    val queuedCount: Int = 0,
    val lastCollectedAtMillis: Long? = null,
    val lastError: String? = null,
)

@HiltViewModel
class LocationSettingsViewModel @Inject constructor(
    private val repository: LocationSettingsRepository,
    private val commander: LocationTrackingCommander,
) : ViewModel() {
    val uiState = repository.state
        .map {
            LocationSettingsUiState(
                isLoading = false,
                isEnabled = it.enabled,
                isExternalBasemapEnabled = it.externalBasemapEnabled,
                isServiceRunning = it.serviceRunning,
                motion = it.motion,
                queuedCount = it.queuedCount,
                lastCollectedAtMillis = it.lastCollectedAtMillis,
                lastError = it.lastError,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocationSettingsUiState(),
        )

    fun setEnabled(enabled: Boolean, canTrackInBackground: Boolean = false) {
        viewModelScope.launch {
            if (!enabled) {
                commander.setTrackingEnabled(false)
            } else if (canTrackInBackground) {
                commander.setTrackingEnabled(true)
            } else {
                repository.setEnabled(true)
            }
        }
    }

    fun setExternalBasemapEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setExternalBasemapEnabled(enabled) }
    }

    fun syncTracking(canTrackInBackground: Boolean) {
        viewModelScope.launch {
            val state = repository.state.first()
            if (state.enabled && canTrackInBackground) {
                commander.start(requireBackgroundLocation = true)
            } else if (state.serviceRunning && (!state.enabled || !canTrackInBackground)) {
                commander.stop()
            }
        }
    }
}
