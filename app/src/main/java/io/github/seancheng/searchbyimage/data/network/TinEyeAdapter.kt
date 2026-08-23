package io.github.seancheng.searchbyimage.data.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import io.github.seancheng.searchbyimage.domain.CredentialIds
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.NativeResultItem
import io.github.seancheng.searchbyimage.domain.SearchEngineAdapter
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.domain.SearchRequest
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class TinEyeAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome {
        val apiKey = request.credentials[CredentialIds.TINEYE_API_KEY]
            ?: return SearchOutcome.AuthRequired(descriptor, descriptor.credentialFields)
        val upload = runCatching { prepareUpload(request) }.getOrElse {
            return SearchOutcome.Error(
                descriptor,
                SearchFailure(SearchErrorCode.INVALID_IMAGE, it.message ?: "无法生成 TinEye 上传副本"),
            )
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("offset", "0")
            .addFormDataPart("limit", "20")
            .addFormDataPart("sort", "score")
            .addFormDataPart("order", "desc")
            .addFormDataPart(
                "image_upload",
                "search.jpg",
                upload.toRequestBody("image/jpeg".toMediaType()),
            )
            .build()
        val httpRequest = Request.Builder()
            .url(API_ENDPOINT)
            .header("x-api-key", apiKey)
            .header("Accept", "application/json")
            .post(body)
            .build()
        return try {
            HttpClientProvider.client.newCall(httpRequest).await().use { response ->
                if (!response.isSuccessful) return failureForStatus(descriptor, response)
                val payload = response.body.stringWithLimit()
                val json = JSONObject(payload)
                if (json.optInt("code", 200) != 200) {
                    return SearchOutcome.Error(
                        descriptor,
                        SearchFailure(SearchErrorCode.REMOTE_SERVICE, "TinEye 拒绝了搜索请求"),
                    )
                }
                if (json.optJSONObject("results")?.has("matches") != true) {
                    return SearchOutcome.Error(
                        descriptor,
                        SearchFailure(SearchErrorCode.PARSE, "TinEye 返回格式已变化"),
                    )
                }
                SearchOutcome.NativeResults(descriptor, parse(json))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: IOException) {
            SearchOutcome.Error(
                descriptor,
                SearchFailure(SearchErrorCode.NETWORK, "网络连接失败，请检查网络后重试", retryable = true),
            )
        } catch (exception: Exception) {
            SearchOutcome.Error(descriptor, SearchFailure(SearchErrorCode.PARSE, "无法解析 TinEye 返回的数据"))
        }
    }

    internal fun parse(root: JSONObject): List<NativeResultItem> {
        val matches = root.optJSONObject("results")?.optJSONArray("matches") ?: return emptyList()
        return buildList {
            for (matchIndex in 0 until matches.length()) {
                val match = matches.optJSONObject(matchIndex) ?: continue
                val backlinks = match.optJSONArray("backlinks") ?: continue
                val thumbnail = safeResultUrl(match.optString("image_url"))
                val score = match.optDouble("score").takeUnless { it.isNaN() }
                for (backlinkIndex in 0 until backlinks.length()) {
                    val backlink = backlinks.optJSONObject(backlinkIndex) ?: continue
                    val source = safeResultUrl(backlink.optString("backlink")) ?: continue
                    add(
                        NativeResultItem(
                            title = match.optString("domain").ifBlank { "TinEye 结果" },
                            subtitle = backlink.optString("crawl_date").takeIf(String::isNotBlank),
                            thumbnailUrl = thumbnail ?: safeResultUrl(backlink.optString("url")),
                            sourceUrl = source,
                            similarity = score,
                        ),
                    )
                    if (size >= MAX_RESULTS) return@buildList
                }
            }
        }
    }

    private suspend fun prepareUpload(request: SearchRequest): ByteArray = withContext(Dispatchers.IO) {
        val original = request.image.file.readBytes()
        if (original.size <= MAX_UPLOAD_BYTES && request.image.mimeType == "image/jpeg") return@withContext original

        val decoded = requireNotNull(BitmapFactory.decodeByteArray(original, 0, original.size)) { "无法解码图片" }
        var current = decoded.onWhiteBackground()
        if (current !== decoded) decoded.recycle()
        try {
            if (maxOf(current.width, current.height) > START_MAX_DIMENSION) {
                val scale = START_MAX_DIMENSION.toFloat() / maxOf(current.width, current.height)
                val scaled = current.scale(
                    (current.width * scale).toInt().coerceAtLeast(1),
                    (current.height * scale).toInt().coerceAtLeast(1),
                )
                if (scaled !== current) current.recycle()
                current = scaled
            }
            repeat(MAX_SCALE_ATTEMPTS) {
                ByteArrayOutputStream().use { output ->
                    check(current.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output))
                    val bytes = output.toByteArray()
                    if (bytes.size <= MAX_UPLOAD_BYTES) return@withContext bytes
                }
                val scaled = current.scale(
                    (current.width * SCALE_FACTOR).toInt().coerceAtLeast(1),
                    (current.height * SCALE_FACTOR).toInt().coerceAtLeast(1),
                )
                if (scaled !== current) current.recycle()
                current = scaled
            }
            error("TinEye 上传图片必须小于 1 MB")
        } finally {
            current.recycle()
        }
    }

    private fun Bitmap.onWhiteBackground(): Bitmap {
        if (!hasAlpha()) return this
        return createBitmap(width, height).also { output ->
            Canvas(output).apply {
                drawColor(Color.WHITE)
                drawBitmap(this@onWhiteBackground, 0f, 0f, null)
            }
        }
    }

    private companion object {
        const val API_ENDPOINT = "https://api.tineye.com/rest/search/"
        const val MAX_UPLOAD_BYTES = 1_000_000
        const val START_MAX_DIMENSION = 1_600
        const val JPEG_QUALITY = 82
        const val SCALE_FACTOR = 0.78f
        const val MAX_SCALE_ATTEMPTS = 6
        const val MAX_RESULTS = 20
    }
}
