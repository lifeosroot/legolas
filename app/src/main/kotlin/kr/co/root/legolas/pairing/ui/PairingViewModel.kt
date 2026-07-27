package kr.co.root.legolas.pairing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.co.root.legolas.pairing.data.PairingQrParser
import kr.co.root.legolas.pairing.data.PairingRepository
import kr.co.root.legolas.location.tracking.LocationTrackingCommander
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val isLoading: Boolean = true,
    val serverUrl: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: PairingRepository,
    private val locationTrackingCommander: LocationTrackingCommander,
) : ViewModel() {
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState = combine(repository.pairing, errorMessage) { pairing, error ->
        PairingUiState(
            isLoading = false,
            serverUrl = pairing?.serverUrl,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PairingUiState(),
    )

    fun onQrScanned(
        value: String,
        invalidMessage: String,
        saveFailedMessage: String,
    ) {
        val pairing = runCatching { PairingQrParser.parse(value) }
            .getOrElse {
                errorMessage.value = invalidMessage
                return
            }

        launchRepositoryAction(saveFailedMessage) {
            repository.save(pairing)
        }
    }

    fun onScanFailed(message: String) {
        errorMessage.value = message
    }

    fun clearError() {
        errorMessage.value = null
    }

    fun forget(failedMessage: String) {
        launchRepositoryAction(failedMessage) {
            locationTrackingCommander.disableAndClear()
            repository.clear()
        }
    }

    private fun launchRepositoryAction(
        failedMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                action()
                errorMessage.value = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                errorMessage.value = failedMessage
            }
        }
    }
}
