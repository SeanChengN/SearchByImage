package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.data.db.CustomEngineRepository
import io.github.seancheng.searchbyimage.data.security.CredentialStore
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.domain.PreparedImage
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.domain.SearchRequest

class SearchCoordinator(
    private val credentialStore: CredentialStore,
    private val customEngineRepository: CustomEngineRepository,
) {
    private val builtInAdapters = EngineCatalog.builtIns.associate { descriptor ->
        descriptor.id to when (descriptor.id) {
            "google_lens" -> GoogleLensAdapter(descriptor)
            "trace_moe" -> TraceMoeAdapter(descriptor)
            "saucenao" -> SauceNaoAdapter(descriptor)
            "lenso" -> LensoAdapter(descriptor)
            "tineye" -> TinEyeAdapter(descriptor)
            else -> AssistedWebAdapter(descriptor)
        }
    }

    suspend fun search(
        engineId: String,
        image: PreparedImage,
        options: Map<String, String> = emptyMap(),
    ): SearchOutcome {
        val adapter = if (engineId.startsWith(CUSTOM_PREFIX)) {
            val id = engineId.removePrefix(CUSTOM_PREFIX).toLongOrNull()
                ?: return invalidEngine()
            customEngineRepository.find(id)?.let(::CustomEngineAdapter) ?: return invalidEngine()
        } else {
            builtInAdapters[engineId] ?: return invalidEngine()
        }
        val credentials = credentialStore.valuesFor(adapter.descriptor.credentialFields)
        val missing = adapter.descriptor.credentialFields.filter { it.id !in credentials }
        if (missing.isNotEmpty()) return SearchOutcome.AuthRequired(adapter.descriptor, missing)
        return adapter.search(SearchRequest(image, credentials, options))
    }

    private fun invalidEngine() = SearchOutcome.Error(
        engine = null,
        failure = SearchFailure(SearchErrorCode.NOT_AVAILABLE, "所选搜索引擎不存在或已被移除"),
    )

    private companion object {
        const val CUSTOM_PREFIX = "custom:"
    }
}
