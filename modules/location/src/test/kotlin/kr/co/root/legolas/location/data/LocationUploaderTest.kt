package kr.co.root.legolas.location.data

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUploaderTest {
    @Test
    fun `only transient HTTP failures are retried`() {
        assertFalse(LocationUploadException("bad request", retryable = false).shouldRetryLocationUpload())
        assertTrue(LocationUploadException("server unavailable", retryable = true).shouldRetryLocationUpload())
    }

    @Test
    fun `network failures are retried`() {
        assertTrue(IOException("offline").shouldRetryLocationUpload())
        assertFalse(IllegalStateException("bad configuration").shouldRetryLocationUpload())
    }
}
