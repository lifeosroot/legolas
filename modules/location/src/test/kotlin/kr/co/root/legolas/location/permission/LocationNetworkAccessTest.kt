package kr.co.root.legolas.location.permission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationNetworkAccessTest {
    @Test
    fun `private and local Arwen hosts need Android local network access`() {
        assertTrue(requiresLocalNetworkPermission("https://192.168.0.10"))
        assertTrue(requiresLocalNetworkPermission("https://172.16.0.1:8443"))
        assertTrue(requiresLocalNetworkPermission("https://arwen.local"))
        assertTrue(requiresLocalNetworkPermission("https://[fd00::1]"))
    }

    @Test
    fun `public Arwen host does not need Android local network access`() {
        assertFalse(requiresLocalNetworkPermission("https://arwen.example.com"))
        assertFalse(requiresLocalNetworkPermission("not a URL"))
    }
}
