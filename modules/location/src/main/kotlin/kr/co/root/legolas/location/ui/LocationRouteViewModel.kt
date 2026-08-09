package kr.co.root.legolas.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.data.LocationSampleQuality
import kr.co.root.legolas.location.data.LocationSampleQuery
import kr.co.root.legolas.location.data.LocationSettingsRepository
import java.time.LocalDate
import javax.inject.Inject

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
