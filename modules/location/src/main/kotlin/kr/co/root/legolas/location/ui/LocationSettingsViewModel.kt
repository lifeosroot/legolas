package kr.co.root.legolas.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.co.root.legolas.location.data.LocationSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocationSettingsUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
)

@HiltViewModel
class LocationSettingsViewModel @Inject constructor(
    private val repository: LocationSettingsRepository,
) : ViewModel() {
    val uiState = repository.enabled
        .map { LocationSettingsUiState(isLoading = false, isEnabled = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LocationSettingsUiState(),
        )

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(enabled)
        }
    }
}
