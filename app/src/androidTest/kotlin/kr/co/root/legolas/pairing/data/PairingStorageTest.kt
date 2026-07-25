package kr.co.root.legolas.pairing.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.co.root.legolas.pairing.model.PairingConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PairingStorageTest {
    private val repository = PairingRepository(
        ApplicationProvider.getApplicationContext<Context>(),
    )

    @Before
    fun setUp() = clearPairing()

    @After
    fun tearDown() = clearPairing()

    private fun clearPairing() = runBlocking {
        repository.clear()
    }

    @Test
    fun encryptsAndDecryptsApiKey() {
        val apiKey = "arwen_test-key"
        val encrypted = ApiKeyCipher.encrypt(apiKey)

        assertNotEquals(apiKey, encrypted)
        assertEquals(apiKey, ApiKeyCipher.decrypt(encrypted))
    }

    @Test
    fun savesAndRestoresPairing() = runBlocking {
        val pairing = PairingConfig(
            serverUrl = "https://arwen.example.com",
            apiKey = "arwen_test-key",
        )

        repository.save(pairing)

        assertEquals(pairing, repository.pairing.first())
    }

    @Test
    fun clearsPairing() = runBlocking {
        repository.save(
            PairingConfig(
                serverUrl = "https://arwen.example.com",
                apiKey = "arwen_test-key",
            ),
        )

        repository.clear()

        assertNull(repository.pairing.first())
    }
}
