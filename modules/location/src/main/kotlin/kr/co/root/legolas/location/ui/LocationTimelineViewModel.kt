package kr.co.root.legolas.location.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.co.root.legolas.location.data.LocationTimelineQuery
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LocationTimelineViewModel @Inject constructor(
    private val query: LocationTimelineQuery,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LocationTimelineUiState())
    val uiState = mutableUiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadEntries()
    }

    fun moveDate(days: Long) {
        mutableUiState.update { it.copy(selectedDate = it.selectedDate.plusDays(days)) }
        loadEntries()
    }

    fun selectToday() {
        mutableUiState.update { it.copy(selectedDate = LocalDate.now(SeoulZone)) }
        loadEntries()
    }

    fun refresh() = loadEntries()

    private fun loadEntries() {
        val date = mutableUiState.value.selectedDate
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val entries = query.entriesOn(date)
                mutableUiState.update {
                    if (it.selectedDate == date) {
                        it.copy(entries = entries, isLoading = false)
                    } else {
                        it
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableUiState.update {
                    if (it.selectedDate == date) {
                        it.copy(isLoading = false, hasError = true)
                    } else {
                        it
                    }
                }
            }
        }
    }
}
