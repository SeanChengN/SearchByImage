package io.github.seancheng.searchbyimage.data.network

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

class TraceMoeAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                request.image.file.name,
                request.image.file.asRequestBody(request.image.mimeType.toMediaType()),
            )
            .build()
        val httpRequest = Request.Builder()
            .url("https://api.trace.moe/search?anilistInfo")
            .post(body)
            .header("Accept", "application/json")
            .build()
        return execute(httpRequest)
    }

    private suspend fun execute(request: Request): SearchOutcome = try {
        HttpClientProvider.client.newCall(request).await().use { response ->
            if (!response.isSuccessful) return failureForStatus(descriptor, response)
            val payload = response.body.stringWithLimit()
            if (!JSONObject(payload).has("result")) {
                return SearchOutcome.Error(
                    descriptor,
                    SearchFailure(SearchErrorCode.PARSE, "trace.moe 返回格式已变化"),
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
        SearchOutcome.Error(
            descriptor,
            SearchFailure(SearchErrorCode.PARSE, "无法解析 trace.moe 返回的数据"),
        )
    }

    internal fun parse(payload: String): List<NativeResultItem> {
        val array = JSONObject(payload).optJSONArray("result") ?: return emptyList()
        return buildList {
            for (index in 0 until minOf(array.length(), 20)) {
                val item = array.optJSONObject(index) ?: continue
                val anilist = item.optJSONObject("anilist")
                val titleObject = anilist?.optJSONObject("title")
                val title = titleObject?.optString("native")
                    ?.takeIf(String::isNotBlank)
                    ?: titleObject?.optString("english")?.takeIf(String::isNotBlank)
                    ?: item.optString("filename", "未知作品")
                val episode = item.opt("episode")?.toString()?.takeIf { it != "null" }
                val from = item.optDouble("from", 0.0)
                val minute = (from / 60).toInt()
                val second = (from % 60).toInt()
                val source = safeResultUrl(anilist?.optString("siteUrl"))
                    ?: safeResultUrl(item.optString("video"))
                    ?: continue
                add(
                    NativeResultItem(
                        title = title,
                        subtitle = listOfNotNull(
                            episode?.let { "第 $it 集" },
                            "%02d:%02d".format(minute, second),
                        ).joinToString(" · "),
                        thumbnailUrl = safeResultUrl(item.optString("image")),
                        sourceUrl = source,
                        similarity = item.optDouble("similarity", 0.0) * 100,
                    ),
                )
            }
        }
    }
}
