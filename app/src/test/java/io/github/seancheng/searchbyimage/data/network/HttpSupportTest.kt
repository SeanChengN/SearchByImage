package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpSupportTest {
    private val engine = requireNotNull(EngineCatalog.find("trace_moe"))

    @Test
    fun awaitUsesLocalFixtureAndMapsRateLimit() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse.Builder()
                    .code(429)
                    .addHeader("Retry-After", "17")
                    .body("fixture body must not be exposed")
                    .build(),
            )
            val request = Request.Builder().url(server.url("/search")).build()
            OkHttpClient().newCall(request).await().use { response ->
                val failure = failureForStatus(engine, response).failure
                assertEquals(SearchErrorCode.RATE_LIMITED, failure.code)
                assertEquals(17L, failure.retryAfterSeconds)
                assertTrue(failure.retryable)
                assertTrue("fixture body" !in failure.message)
            }
            assertEquals("/search", server.takeRequest().url.encodedPath)
        } finally {
            server.close()
        }
    }

    @Test
    fun statusMappingCoversAuthenticationPaymentRedirectAndServerErrors() {
        assertEquals(SearchErrorCode.AUTHENTICATION, failureForStatus(engine, response(401)).failure.code)
        assertEquals(SearchErrorCode.PAYMENT_REQUIRED, failureForStatus(engine, response(402)).failure.code)
        assertEquals(SearchErrorCode.AUTHENTICATION, failureForStatus(engine, response(403)).failure.code)
        assertEquals(SearchErrorCode.UNSAFE_REDIRECT, failureForStatus(engine, response(302)).failure.code)
        assertEquals(SearchErrorCode.REMOTE_SERVICE, failureForStatus(engine, response(503)).failure.code)
    }

    @Test
    fun responseReaderRejectsOversizedBodies() {
        val body = ByteArray(2 * 1024 * 1024 + 1).toResponseBody()
        assertTrue(runCatching { body.stringWithLimit() }.isFailure)
    }

    private fun response(code: Int) = okhttp3.Response.Builder()
        .request(Request.Builder().url("https://example.com/search").build())
        .protocol(okhttp3.Protocol.HTTP_1_1)
        .code(code)
        .message("fixture")
        .body(ByteArray(0).toResponseBody())
        .build()
}
