package io.github.seancheng.searchbyimage.domain

import android.net.Uri
import java.io.File

enum class EngineMode(val label: String) {
    API("原生 API"),
    EXTERNAL_APP("外部应用"),
    DIRECT_UPLOAD("自动上传"),
    ASSISTED_WEB("网页辅助"),
    CUSTOM("自定义"),
}

enum class EngineCategory(val label: String) {
    GENERAL("通用搜索"),
    ANIME("动漫与插画"),
    SOURCE("来源与版权"),
}

enum class EngineBadge(val label: String) {
    AUTOMATIC("自动"),
    ASSISTED("网页辅助"),
    API_KEY("需 API Key"),
    PAID("收费"),
    LOGIN("需登录"),
}

data class CredentialField(
    val id: String,
    val label: String,
    val isSecret: Boolean = true,
)

data class EngineDescriptor(
    val id: String,
    val name: String,
    val summary: String,
    val category: EngineCategory,
    val mode: EngineMode,
    val badges: Set<EngineBadge>,
    val officialUrl: String,
    val termsUrl: String? = null,
    val defaultEnabled: Boolean = false,
    val credentialFields: List<CredentialField> = emptyList(),
)

data class PreparedImage(
    val file: File,
    val contentUri: Uri,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val byteCount: Long,
)

data class SearchRequest(
    val image: PreparedImage,
    val credentials: Map<String, String> = emptyMap(),
    val options: Map<String, String> = emptyMap(),
)

data class NativeResultItem(
    val title: String,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null,
    val sourceUrl: String,
    val similarity: Double? = null,
)

enum class SearchErrorCode {
    INVALID_IMAGE,
    AUTHENTICATION,
    PAYMENT_REQUIRED,
    RATE_LIMITED,
    NETWORK,
    REMOTE_SERVICE,
    UNSAFE_REDIRECT,
    PARSE,
    CANCELLED,
    NOT_AVAILABLE,
}

data class SearchFailure(
    val code: SearchErrorCode,
    val message: String,
    val retryable: Boolean = false,
    val retryAfterSeconds: Long? = null,
)

sealed interface SearchOutcome {
    data class NativeResults(
        val engine: EngineDescriptor,
        val items: List<NativeResultItem>,
    ) : SearchOutcome

    data class WebResult(
        val engine: EngineDescriptor,
        val url: String,
    ) : SearchOutcome

    data class ExternalApp(
        val engine: EngineDescriptor,
        val packageName: String,
        val fallbackUrl: String,
    ) : SearchOutcome

    data class AssistedWeb(
        val engine: EngineDescriptor,
        val url: String,
        val instructions: String = "网页打开后，请再次选择同一张图片。",
    ) : SearchOutcome

    data class AuthRequired(
        val engine: EngineDescriptor,
        val missingFields: List<CredentialField>,
    ) : SearchOutcome

    data class Error(
        val engine: EngineDescriptor?,
        val failure: SearchFailure,
    ) : SearchOutcome
}

interface SearchEngineAdapter {
    val descriptor: EngineDescriptor
    suspend fun search(request: SearchRequest): SearchOutcome
}
