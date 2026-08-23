package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.data.db.EndpointValidator
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

object HttpClientProvider {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(true)
        .build()
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        },
    )
}

fun failureForStatus(
    engine: EngineDescriptor,
    response: Response,
): SearchOutcome.Error {
    val retryAfter = response.header("Retry-After")?.toLongOrNull()
    val (code, message, retryable) = when (response.code) {
        401, 403 -> Triple(SearchErrorCode.AUTHENTICATION, "凭据无效或没有访问权限", false)
        402 -> Triple(SearchErrorCode.PAYMENT_REQUIRED, "此服务需要有效订阅或余额", false)
        429 -> Triple(SearchErrorCode.RATE_LIMITED, "请求过于频繁，请稍后重试", true)
        in 300..399 -> Triple(SearchErrorCode.UNSAFE_REDIRECT, "服务返回了未验证的跳转", false)
        in 500..599 -> Triple(SearchErrorCode.REMOTE_SERVICE, "搜索服务暂时不可用", true)
        else -> Triple(SearchErrorCode.REMOTE_SERVICE, "搜索服务拒绝了请求", false)
    }
    return SearchOutcome.Error(
        engine,
        SearchFailure(code, message, retryable, retryAfter),
    )
}

fun ResponseBody.stringWithLimit(maxBytes: Long = MAX_RESPONSE_BYTES): String {
    val declaredLength = contentLength()
    require(declaredLength < 0 || declaredLength <= maxBytes) { "搜索服务响应过大" }
    val source = source()
    source.request(maxBytes + 1)
    require(source.buffer.size <= maxBytes) { "搜索服务响应过大" }
    val charset = contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
    return source.buffer.clone().readString(charset)
}

fun safeResultUrl(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val uri = EndpointValidator.validate(raw).getOrThrow()
        uri.toASCIIString()
    }.getOrNull()
}

private const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024
