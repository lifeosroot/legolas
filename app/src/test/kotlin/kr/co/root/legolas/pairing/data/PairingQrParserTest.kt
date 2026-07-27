package kr.co.root.legolas.pairing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingQrParserTest {
    @Test
    fun `parses Arwen pairing URI`() {
        val pairing = PairingQrParser.parse(
            "legolas://pair?server=https%3A%2F%2F192.168.0.10%3A8443&apiKey=arwen_test-key",
        )

        assertEquals("https://192.168.0.10:8443", pairing.serverUrl)
        assertEquals("arwen_test-key", pairing.apiKey)
    }

    @Test
    fun `removes trailing slash from server URL`() {
        val pairing = PairingQrParser.parse(
            "legolas://pair?server=https%3A%2F%2Farwen.example.com%2F&apiKey=arwen_test",
        )

        assertEquals("https://arwen.example.com", pairing.serverUrl)
    }

    @Test
    fun `rejects non-Arwen URI`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse("https://example.com")
        }
    }

    @Test
    fun `rejects unsupported server scheme`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=ftp%3A%2F%2Fexample.com&apiKey=arwen_test",
            )
        }
    }

    @Test
    fun `allows cleartext only for local emulator development`() {
        val pairing = PairingQrParser.parse(
            "legolas://pair?server=http%3A%2F%2F10.0.2.2%3A8080&apiKey=arwen_test",
        )

        assertEquals("http://10.0.2.2:8080", pairing.serverUrl)
    }

    @Test
    fun `rejects cleartext LAN server because location and API key require TLS`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=http%3A%2F%2F192.168.0.10%3A8080&apiKey=arwen_test",
            )
        }
    }

    @Test
    fun `rejects server URL credentials query and fragment`() {
        listOf(
            "https%3A%2F%2Fuser%3Apass%40arwen.example.com",
            "https%3A%2F%2Farwen.example.com%3Fx%3D1",
            "https%3A%2F%2Farwen.example.com%23fragment",
        ).forEach { server ->
            assertThrows(IllegalArgumentException::class.java) {
                PairingQrParser.parse(
                    "legolas://pair?server=$server&apiKey=arwen_test",
                )
            }
        }
    }

    @Test
    fun `rejects missing API key`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=https%3A%2F%2Farwen.example.com",
            )
        }
    }

    @Test
    fun `rejects missing server URL`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?apiKey=arwen_test",
            )
        }
    }

    @Test
    fun `rejects server URL without host`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=http%3A%2F%2F%2Fpath&apiKey=arwen_test",
            )
        }
    }

    @Test
    fun `rejects invalid API key prefix`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=https%3A%2F%2Farwen.example.com&apiKey=invalid",
            )
        }
    }

    @Test
    fun `rejects control characters in API key`() {
        assertThrows(IllegalArgumentException::class.java) {
            PairingQrParser.parse(
                "legolas://pair?server=https%3A%2F%2Farwen.example.com&apiKey=arwen_test%0Ainjected",
            )
        }
    }
}
