package kr.co.root.legolas.pairing.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.root.legolas.feature.LocationFeature
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingHealthMonitor @Inject constructor(
    private val repository: PairingRepository,
    private val locationFeature: LocationFeature,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val suggestionDismissed = AtomicBoolean(false)
    private val mutableShouldSuggestLogout = MutableStateFlow(false)

    val shouldSuggestLogout = mutableShouldSuggestLogout.asStateFlow()

    fun start() {
        scope.launch {
            repository.pairing.collectLatest { pairing ->
                resetSuggestion()
                if (pairing == null) return@collectLatest
                var consecutiveFailures = 0
                while (true) {
                    val result = repository.checkHealth(pairing)
                    consecutiveFailures = nextHealthFailureCount(consecutiveFailures, result)
                    when (result) {
                        PairingHealthResult.Healthy -> resetSuggestion()
                        PairingHealthResult.Unavailable -> {
                            if (
                                consecutiveFailures >= HEALTH_FAILURES_BEFORE_LOGOUT_SUGGESTION &&
                                !suggestionDismissed.get()
                            ) {
                                mutableShouldSuggestLogout.value = true
                            }
                        }
                        PairingHealthResult.Rejected -> {
                            try {
                                locationFeature.disableAndClear()
                                repository.clear()
                                return@collectLatest
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                // Keep the pairing and retry if local cleanup could not complete safely.
                            }
                        }
                    }
                    delay(HEALTH_CHECK_INTERVAL_MILLIS)
                }
            }
        }
    }

    fun dismissLogoutSuggestion() {
        suggestionDismissed.set(true)
        mutableShouldSuggestLogout.value = false
    }

    private fun resetSuggestion() {
        suggestionDismissed.set(false)
        mutableShouldSuggestLogout.value = false
    }

    private companion object {
        const val HEALTH_CHECK_INTERVAL_MILLIS = 60_000L
    }
}

internal const val HEALTH_FAILURES_BEFORE_LOGOUT_SUGGESTION = 5

internal fun nextHealthFailureCount(
    previous: Int,
    result: PairingHealthResult,
): Int = if (result == PairingHealthResult.Unavailable) {
    (previous + 1).coerceAtMost(HEALTH_FAILURES_BEFORE_LOGOUT_SUGGESTION)
} else {
    0
}
