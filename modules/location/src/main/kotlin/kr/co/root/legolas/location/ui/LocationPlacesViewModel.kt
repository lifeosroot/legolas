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
import kr.co.root.legolas.location.data.LocationPlaceDraft
import kr.co.root.legolas.location.data.LocationPlaceRepository
import kr.co.root.legolas.location.data.LocationSampleQuery
import kr.co.root.legolas.location.data.LocationSettingsRepository
import javax.inject.Inject

@HiltViewModel
class LocationPlacesViewModel @Inject constructor(
    private val repository: LocationPlaceRepository,
    private val sampleQuery: LocationSampleQuery,
    private val settingsRepository: LocationSettingsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LocationPlacesUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        refresh()
        viewModelScope.launch {
            settingsRepository.state
                .map { it.externalBasemapEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    mutableUiState.update { it.copy(isExternalBasemapEnabled = enabled) }
                }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, hasLoadError = false) }
            try {
                val places = repository.findAll()
                val latestSample = try {
                    sampleQuery.latest()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (_: Exception) {
                    null
                }
                mutableUiState.update {
                    it.copy(
                        places = places,
                        suggestedLatitude = latestSample?.latitude,
                        suggestedLongitude = latestSample?.longitude,
                        isLoading = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isLoading = false, hasLoadError = true) }
            }
        }
    }

    fun save(placeId: Long?, draft: LocationPlaceDraft) {
        if (mutableUiState.value.isSaving) return
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    isSaving = true,
                    hasMutationError = false,
                    saveCompleted = false,
                )
            }
            try {
                val saved = if (placeId == null) {
                    repository.create(draft)
                } else {
                    repository.update(placeId, draft)
                }
                mutableUiState.update { state ->
                    state.copy(
                        places = (state.places.filterNot { it.id == saved.id } + saved)
                            .sortedBy { it.name.lowercase() },
                        isSaving = false,
                        hasMutationError = false,
                        saveCompleted = true,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        isSaving = false,
                        hasMutationError = true,
                        saveCompleted = false,
                    )
                }
            }
        }
    }

    fun delete(placeId: Long) {
        if (mutableUiState.value.isSaving) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, hasMutationError = false) }
            try {
                repository.delete(placeId)
                mutableUiState.update {
                    it.copy(
                        places = it.places.filterNot { place -> place.id == placeId },
                        isSaving = false,
                        hasMutationError = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update { it.copy(isSaving = false, hasMutationError = true) }
            }
        }
    }

    fun setExternalBasemapEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setExternalBasemapEnabled(enabled) }
    }

    fun clearMutationError() {
        mutableUiState.update { it.copy(hasMutationError = false) }
    }

    fun consumeSaveCompleted() {
        mutableUiState.update { it.copy(saveCompleted = false) }
    }
}
