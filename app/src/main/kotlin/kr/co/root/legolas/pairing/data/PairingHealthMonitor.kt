package kr.co.root.legolas.pairing.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.root.legolas.feature.LocationFeature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingHealthMonitor @Inject constructor(
    private val repository: PairingRepository,
    private val locationFeature: LocationFeature,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        scope.launch {
            repository.pairing.collectLatest { pairing ->
                if (pairing == null) return@collectLatest
                while (true) {
                    if (repository.checkHealth(pairing) == PairingHealthResult.Rejected) {
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
                    delay(HEALTH_CHECK_INTERVAL_MILLIS)
                }
            }
        }
    }

    private companion object {
        const val HEALTH_CHECK_INTERVAL_MILLIS = 60_000L
    }
}
