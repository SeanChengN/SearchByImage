package io.github.seancheng.searchbyimage.data.network

import android.util.Base64
import io.github.seancheng.searchbyimage.domain.CredentialIds
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.NativeResultItem
import io.github.seancheng.searchbyimage.domain.SearchEngineAdapter
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.domain.SearchRequest
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LensoAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome {
        val apiKey = request.credentials[CredentialIds.LENSO_API_KEY]
            ?: return SearchOutcome.AuthRequired(descriptor, descriptor.credentialFields)
        val encoded = withContext(Dispatchers.IO) {
            Base64.encodeToString(request.image.file.readBytes(), Base64.NO_WRAP)
        }
        val category = request.options["category"]?.takeIf { it in ALLOWED_CATEGORIES } ?: "duplicates"
        val sortType = request.options["sortType"]?.takeIf { it in ALLOWED_SORT_TYPES } ?: "QUALITY_DESCENDING"
        val payload = JSONObject()
            .put("image", encoded)
            .put("category", category)
            .put("sortType", sortType)
            .put("page", 1)
            .toString()
        val httpRequest = Request.Builder()
            .url("https://api.lenso.ai/search")
            .header("Authorization", "Bearer $apiKey")
            .header("Accept", "application/json")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return try {
            HttpClientProvider.client.newCall(httpRequest).await().use { response ->
                if (!response.isSuccessful) return failureForStatus(descriptor, response)
                val body = response.body.stringWithLimit()
                if (!JSONObject(body).has("results")) {
                    return SearchOutcome.Error(
                        descriptor,
                        SearchFailure(SearchErrorCode.PARSE, "Lenso.ai 返回格式已变化"),
                    )
                }
                val results = parse(body)
                SearchOutcome.NativeResults(descriptor, results)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: IOException) {
            SearchOutcome.Error(
                descriptor,
                SearchFailure(SearchErrorCode.NETWORK, "网络连接失败，请检查网络后重试", retryable = true),
            )
        } catch (exception: Exception) {
            SearchOutcome.Error(descriptor, SearchFailure(SearchErrorCode.PARSE, "无法解析 Lenso.ai 返回的数据"))
        }
    }

    internal fun parse(payload: String): List<NativeResultItem> {
        val results = JSONObject(payload).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (resultIndex in 0 until minOf(results.length(), 20)) {
                val result = results.optJSONObject(resultIndex) ?: continue
                val urls = result.optJSONArray("urlList") ?: continue
                for (urlIndex in 0 until urls.length()) {
                    val urlItem = urls.optJSONObject(urlIndex) ?: continue
                    val source = safeResultUrl(urlItem.optString("sourceUrl")) ?: continue
                    add(
                        NativeResultItem(
                            title = urlItem.optString("title").ifBlank { "Lenso.ai 结果" },
                            subtitle = result.optString("date").takeIf(String::isNotBlank),
                            thumbnailUrl = safeResultUrl(urlItem.optString("imageUrl")),
                            sourceUrl = source,
                            similarity = result.optDouble("confidenceScore").takeUnless { it.isNaN() },
                        ),
                    )
                    if (size >= 20) return@buildList
                }
            }
        }
    }

    private companion object {
        val ALLOWED_CATEGORIES = setOf("duplicates", "landmarks", "similar", "related")
        val ALLOWED_SORT_TYPES = setOf(
            "QUALITY_DESCENDING",
            "QUALITY_ASCENDING",
            "DATE_DESCENDING",
            "DATE_ASCENDING",
        )
    }
}
