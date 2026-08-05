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
}
