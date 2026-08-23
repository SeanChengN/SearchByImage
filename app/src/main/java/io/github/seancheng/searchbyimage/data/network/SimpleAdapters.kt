package io.github.seancheng.searchbyimage.data.network

import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.SearchEngineAdapter
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.domain.SearchRequest

class AssistedWebAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome = SearchOutcome.AssistedWeb(
        engine = descriptor,
        url = descriptor.officialUrl,
    )
}

class GoogleLensAdapter(
    override val descriptor: EngineDescriptor,
) : SearchEngineAdapter {
    override suspend fun search(request: SearchRequest): SearchOutcome = SearchOutcome.ExternalApp(
        engine = descriptor,
        packageName = GOOGLE_APP_PACKAGE,
        fallbackUrl = descriptor.officialUrl,
    )

    private companion object {
        const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
    }
}
