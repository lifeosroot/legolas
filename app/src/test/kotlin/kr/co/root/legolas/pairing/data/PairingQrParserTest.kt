package kr.co.root.legolas.pairing.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingQrParserTest {
    @Test
    fun `parses Arwen pairing URI`() {
        val pairing = PairingQrParser.parse(
            "legolas://pair?server=http%3A%2F%2F192.168.0.10%3A8080&apiKey=arwen_test-key",
        )

        assertEquals("http://192.168.0.10:8080", pairing.serverUrl)
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
}
