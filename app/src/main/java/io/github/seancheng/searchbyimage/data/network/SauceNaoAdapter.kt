package io.github.seancheng.searchbyimage.data.network

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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject

class SauceNaoAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome {
        val apiKey = request.credentials[CredentialIds.SAUCENAO_API_KEY]
            ?: return SearchOutcome.AuthRequired(descriptor, descriptor.credentialFields)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("output_type", "2")
            .addFormDataPart("numres", "16")
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart(
                "file",
                request.image.file.name,
                request.image.file.asRequestBody(request.image.mimeType.toMediaType()),
            )
            .build()
        val httpRequest = Request.Builder()
            .url("https://saucenao.com/search.php")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return try {
            HttpClientProvider.client.newCall(httpRequest).await().use { response ->
                if (!response.isSuccessful) return failureForStatus(descriptor, response)
                val payload = response.body.stringWithLimit()
                if (!JSONObject(payload).has("results")) {
                    return SearchOutcome.Error(
                        descriptor,
                        SearchFailure(SearchErrorCode.PARSE, "SauceNAO 返回格式已变化"),
                    )
                }
                val results = parse(payload)
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
            SearchOutcome.Error(descriptor, SearchFailure(SearchErrorCode.PARSE, "无法解析 SauceNAO 返回的数据"))
        }
    }

    internal fun parse(payload: String): List<NativeResultItem> {
        val array = JSONObject(payload).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(array.length(), 20)) {
                val result = array.optJSONObject(index) ?: continue
                val header = result.optJSONObject("header") ?: continue
                val data = result.optJSONObject("data") ?: continue
                val urls = data.optJSONArray("ext_urls")
                val source = (0 until (urls?.length() ?: 0))
                    .firstNotNullOfOrNull { safeResultUrl(urls?.optString(it)) }
                    ?: continue
                val title = sequenceOf("title", "eng_name", "jp_name", "source")
                    .map { data.optString(it) }
                    .firstOrNull(String::isNotBlank)
                    ?: "来源结果"
                val creator = sequenceOf("author_name", "creator", "member_name")
                    .map { data.optString(it) }
                    .firstOrNull(String::isNotBlank)
                add(
                    NativeResultItem(
                        title = title,
                        subtitle = creator,
                        thumbnailUrl = safeResultUrl(header.optString("thumbnail")),
                        sourceUrl = source,
                        similarity = header.optString("similarity").toDoubleOrNull(),
                    ),
                )
            }
        }
    }
}
