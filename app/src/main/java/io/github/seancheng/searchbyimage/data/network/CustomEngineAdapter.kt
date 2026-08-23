package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.data.db.CustomEngine
import io.github.seancheng.searchbyimage.data.db.EndpointValidator
import io.github.seancheng.searchbyimage.domain.EngineBadge
import io.github.seancheng.searchbyimage.domain.EngineCategory
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.EngineMode
import io.github.seancheng.searchbyimage.domain.SearchEngineAdapter
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.domain.SearchRequest
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

class CustomEngineAdapter(
    private val engine: CustomEngine,
) : SearchEngineAdapter {
    override val descriptor = EngineDescriptor(
        id = engine.stableId,
        name = engine.name,
        summary = "用户配置的 HTTPS multipart 搜索端点",
        category = EngineCategory.GENERAL,
        mode = EngineMode.CUSTOM,
        badges = setOf(EngineBadge.AUTOMATIC),
        officialUrl = engine.endpoint,
    )

    private val guardedClient: OkHttpClient = HttpClientProvider.client.newBuilder()
        .dns { hostname ->
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isEmpty() || addresses.any { !EndpointValidator.isPublic(it) }) {
                throw UnknownHostException("Endpoint resolved to a non-public address")
            }
            addresses
        }
        .build()

    override suspend fun search(request: SearchRequest): SearchOutcome {
        val endpoint = EndpointValidator.validate(engine.endpoint).getOrElse {
            return SearchOutcome.Error(
                descriptor,
                SearchFailure(SearchErrorCode.NOT_AVAILABLE, it.message ?: "自定义端点无效"),
            )
        }
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).apply {
            engine.staticFields.forEach { (name, value) -> addFormDataPart(name, value) }
            addFormDataPart(
                engine.fileField,
                request.image.file.name,
                request.image.file.asRequestBody(request.image.mimeType.toMediaType()),
            )
        }.build()
        val httpRequest = Request.Builder()
            .url(endpoint.toASCIIString())
            .header("Accept", "application/json")
            .post(body)
            .build()
        return try {
            guardedClient.newCall(httpRequest).await().use { response ->
                if (!response.isSuccessful) return failureForStatus(descriptor, response)
                val payload = response.body.stringWithLimit(MAX_RESPONSE_BYTES)
                val url = extractJsonPath(JSONObject(payload), engine.resultUrlJsonField)?.toString()
                    ?.let(::safeResultUrl)
                    ?: return SearchOutcome.Error(
                        descriptor,
                        SearchFailure(SearchErrorCode.PARSE, "响应中没有安全的 HTTPS 结果链接"),
                    )
                SearchOutcome.WebResult(descriptor, url)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: IOException) {
            SearchOutcome.Error(
                descriptor,
                SearchFailure(SearchErrorCode.NETWORK, "无法连接自定义端点", retryable = true),
            )
        } catch (exception: Exception) {
            SearchOutcome.Error(descriptor, SearchFailure(SearchErrorCode.PARSE, "无法解析自定义端点响应"))
        }
    }

    private fun extractJsonPath(root: JSONObject, path: String): Any? {
        var current: Any? = root
        path.split('.').forEach { segment ->
            current = (current as? JSONObject)?.opt(segment) ?: return null
        }
        return current
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 2L * 1024 * 1024
    }
}
