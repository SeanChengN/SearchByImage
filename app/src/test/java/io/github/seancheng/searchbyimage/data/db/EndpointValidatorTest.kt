package io.github.seancheng.searchbyimage.data.db

import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidatorTest {
    @Test
    fun acceptsStandardPublicHttpsEndpoint() {
        val result = EndpointValidator.validate("https://example.com/search")
        assertTrue(result.isSuccess)
        assertEquals("example.com", result.getOrThrow().host)
    }

    @Test
    fun rejectsCleartextAndCredentials() {
        assertTrue(EndpointValidator.validate("http://example.com/search").isFailure)
        assertTrue(EndpointValidator.validate("https://user:pass@example.com/search").isFailure)
    }

    @Test
    fun rejectsLoopbackAndPrivateLiterals() {
        assertTrue(EndpointValidator.validate("https://127.0.0.1/search").isFailure)
        assertTrue(EndpointValidator.validate("https://10.0.0.2/search").isFailure)
        assertTrue(EndpointValidator.validate("https://192.168.1.20/search").isFailure)
        assertFalse(EndpointValidator.isPublic(InetAddress.getByName("::1")))
    }

    @Test
    fun rejectsNonStandardPortsAndLocalNames() {
        assertTrue(EndpointValidator.validate("https://example.com:8443/search").isFailure)
        assertTrue(EndpointValidator.validate("https://printer.local/upload").isFailure)
        assertTrue(EndpointValidator.validate("https://localhost/upload").isFailure)
    }
}
