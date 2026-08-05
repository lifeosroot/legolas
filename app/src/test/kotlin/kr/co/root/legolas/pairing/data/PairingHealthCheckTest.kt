package kr.co.root.legolas.pairing.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PairingHealthCheckTest {
    @Test
    fun `successful response keeps pairing`() {
        assertEquals(PairingHealthResult.Healthy, pairingHealthResult(200))
        assertEquals(PairingHealthResult.Healthy, pairingHealthResult(204))
    }

    @Test
    fun `authentication rejection logs out`() {
        assertEquals(PairingHealthResult.Rejected, pairingHealthResult(401))
        assertEquals(PairingHealthResult.Rejected, pairingHealthResult(403))
    }

    @Test
    fun `temporary server failures keep pairing for retry`() {
        assertEquals(PairingHealthResult.Unavailable, pairingHealthResult(404))
        assertEquals(PairingHealthResult.Unavailable, pairingHealthResult(500))
    }

    @Test
    fun `health endpoint is independent of feature modules`() {
        assertEquals(
            "https://arwen.example.com/api/health",
            pairingHealthEndpoint("https://arwen.example.com/").toString(),
        )
    }

    @Test
    fun `five consecutive failures trigger the logout suggestion threshold`() {
        var failures = 0

        repeat(HEALTH_FAILURES_BEFORE_LOGOUT_SUGGESTION - 1) {
            failures = nextHealthFailureCount(failures, PairingHealthResult.Unavailable)
        }
        assertEquals(4, failures)

        failures = nextHealthFailureCount(failures, PairingHealthResult.Unavailable)
        assertEquals(HEALTH_FAILURES_BEFORE_LOGOUT_SUGGESTION, failures)
    }

    @Test
    fun `a completed health check resets consecutive failures`() {
        assertEquals(
            0,
            nextHealthFailureCount(4, PairingHealthResult.Healthy),
        )
        assertEquals(
            0,
            nextHealthFailureCount(4, PairingHealthResult.Rejected),
        )
    }
}
