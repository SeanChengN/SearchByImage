package io.github.seancheng.searchbyimage

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.seancheng.searchbyimage.data.db.CustomEngine
import io.github.seancheng.searchbyimage.data.settings.AppSettings
import io.github.seancheng.searchbyimage.domain.EngineBadge
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.domain.EngineCategory
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.EngineMode
import io.github.seancheng.searchbyimage.domain.PreparedImage
import io.github.seancheng.searchbyimage.domain.NativeResultItem
import io.github.seancheng.searchbyimage.domain.SearchErrorCode
import io.github.seancheng.searchbyimage.domain.SearchFailure
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.worker.BackgroundSearchWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EngineItem(
    val descriptor: EngineDescriptor,
    val enabled: Boolean,
    val customEngine: CustomEngine? = null,
)

data class AppUiState(
    val settings: AppSettings = AppSettings(
        enabledEngineIds = EngineCatalog.builtIns.filter { it.defaultEnabled }.map { it.id }.toSet(),
        engineOrder = EngineCatalog.builtIns.map { it.id },
        defaultEngineId = "trace_moe",
        dynamicColor = true,
        stripMetadata = true,
        jpegQuality = 90,
        consumedBackgroundWorkId = null,
    ),
    val engines: List<EngineItem> = emptyList(),
    val selectedEngineId: String = "trace_moe",
    val image: PreparedImage? = null,
    val isPreparingImage: Boolean = false,
    val isSearching: Boolean = false,
    val isBackgroundSearching: Boolean = false,
    val outcome: SearchOutcome? = null,
    val message: String? = null,
    val configuredCredentials: Set<String> = emptySet(),
)

class AppViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settings,
                container.customEngineRepository.engines,
            ) { settings, customEngines -> settings to customEngines }
                .collect { (settings, customEngines) ->
                    val builtInsById = EngineCatalog.builtIns.associateBy { it.id }
                    val orderedBuiltIns = settings.engineOrder.mapNotNull(builtInsById::get) +
                        EngineCatalog.builtIns.filter { it.id !in settings.engineOrder }
                    val engineItems = orderedBuiltIns.map { descriptor ->
                        EngineItem(descriptor, descriptor.id in settings.enabledEngineIds)
                    } + customEngines.map { engine ->
                        EngineItem(engine.toDescriptor(), engine.enabled, engine)
                    }
                    val enabledIds = engineItems.filter { it.enabled }.map { it.descriptor.id }
                    _uiState.update { current ->
                        current.copy(
                            settings = settings,
                            engines = engineItems,
                            selectedEngineId = current.selectedEngineId.takeIf { it in enabledIds }
                                ?: settings.defaultEngineId.takeIf { it in enabledIds }
                                ?: enabledIds.firstOrNull()
                                ?: current.selectedEngineId,
                            configuredCredentials = configuredCredentialIds(),
                        )
                    }
                }
        }
        observeBackgroundWork()
    }

    fun prepareImage(uri: Uri) {
        searchJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingImage = true, outcome = null, message = null) }
            runCatching { container.imageRepository.prepare(uri, _uiState.value.settings.jpegQuality) }
                .onSuccess { image ->
                    _uiState.update { it.copy(image = image, isPreparingImage = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isPreparingImage = false,
                            message = error.message ?: "无法处理所选图片",
                        )
                    }
                }
        }
    }

    fun selectEngine(id: String) {
        if (_uiState.value.engines.any { it.descriptor.id == id && it.enabled }) {
            _uiState.update { it.copy(selectedEngineId = id, outcome = null) }
        }
    }

    fun search() {
        if (_uiState.value.isBackgroundSearching) {
            showMessage("已有一项后台搜索正在进行，请先取消或等待完成")
            return
        }
        val image = _uiState.value.image ?: run {
            _uiState.update { it.copy(message = "请先选择一张图片") }
            return
        }
        val engineId = _uiState.value.selectedEngineId
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, outcome = null, message = null) }
            try {
                val outcome = container.searchCoordinator.search(engineId, image)
                _uiState.update { it.copy(isSearching = false, outcome = outcome) }
            } catch (cancelled: CancellationException) {
                _uiState.update { it.copy(isSearching = false) }
                throw cancelled
            }
        }
    }

    fun cancelSearch() {
        if (_uiState.value.isBackgroundSearching) {
            WorkManager.getInstance(getApplication<Application>())
                .cancelUniqueWork(BackgroundSearchWorker.UNIQUE_WORK_NAME)
            _uiState.update { it.copy(message = "正在取消后台搜索") }
            return
        }
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = false, message = "搜索已取消") }
    }

    fun continueInBackground() {
        val state = _uiState.value
        if (!state.isSearching || state.isBackgroundSearching) {
            showMessage("当前没有可转入后台的前台搜索")
            return
        }
        val image = state.image ?: return
        searchJob?.cancel()
        val input = Data.Builder()
            .putString(BackgroundSearchWorker.KEY_ENGINE_ID, state.selectedEngineId)
            .putString(BackgroundSearchWorker.KEY_IMAGE_PATH, image.file.absolutePath)
            .putString(BackgroundSearchWorker.KEY_MIME_TYPE, image.mimeType)
            .putInt(BackgroundSearchWorker.KEY_WIDTH, image.width)
            .putInt(BackgroundSearchWorker.KEY_HEIGHT, image.height)
            .build()
        val request = OneTimeWorkRequestBuilder<BackgroundSearchWorker>()
            .setInputData(input)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(BackgroundSearchWorker.UNIQUE_WORK_NAME)
            .build()
        WorkManager.getInstance(getApplication()).enqueueUniqueWork(
            BackgroundSearchWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        _uiState.update {
            it.copy(
                isSearching = true,
                isBackgroundSearching = true,
                message = "已转到后台搜索，可从通知栏取消",
            )
        }
    }

    private fun observeBackgroundWork() {
        viewModelScope.launch {
            combine(
                container.settingsRepository.settings,
                WorkManager.getInstance(getApplication<Application>())
                    .getWorkInfosForUniqueWorkFlow(BackgroundSearchWorker.UNIQUE_WORK_NAME),
            ) { settings, workInfos -> settings to workInfos.lastOrNull() }
                .collect { (settings, work) ->
                    work ?: return@collect
                    when (work.state) {
                        WorkInfo.State.ENQUEUED,
                        WorkInfo.State.RUNNING,
                        WorkInfo.State.BLOCKED -> _uiState.update {
                            it.copy(isSearching = true, isBackgroundSearching = true)
                        }

                        WorkInfo.State.SUCCEEDED,
                        WorkInfo.State.FAILED,
                        WorkInfo.State.CANCELLED -> {
                            if (settings.consumedBackgroundWorkId != work.id.toString()) {
                                restoreBackgroundOutcome(work)
                            }
                        }
                    }
                }
        }
    }

    private suspend fun restoreBackgroundOutcome(work: WorkInfo) {
        val workId = work.id.toString()
        val engineId = work.outputData.getString(BackgroundSearchWorker.KEY_RESULT_ENGINE_ID)
        val descriptor = engineId?.let { id ->
            EngineCatalog.find(id) ?: _uiState.value.engines.firstOrNull { it.descriptor.id == id }?.descriptor
        }
        val outcome = when (work.state) {
            WorkInfo.State.SUCCEEDED -> {
                val url = work.outputData.getString(BackgroundSearchWorker.KEY_RESULT_URL)
                when (work.outputData.getString(BackgroundSearchWorker.KEY_RESULT_KIND)) {
                    "native" -> if (descriptor != null) {
                        val count = work.outputData.getInt(BackgroundSearchWorker.KEY_RESULT_COUNT, 1)
                        SearchOutcome.NativeResults(
                            descriptor,
                            if (url != null) listOf(
                                NativeResultItem(
                                    title = "后台搜索返回 $count 条结果",
                                    subtitle = "打开首条结果；完整结果需在前台搜索时查看",
                                    sourceUrl = url,
                                ),
                            ) else emptyList(),
                        )
                    } else null
                    "assisted" -> if (descriptor != null && url != null) SearchOutcome.AssistedWeb(descriptor, url) else null
                    "web", "external" -> if (descriptor != null && url != null) SearchOutcome.WebResult(descriptor, url) else null
                    else -> null
                }
            }
            WorkInfo.State.FAILED -> SearchOutcome.Error(
                descriptor,
                SearchFailure(
                    SearchErrorCode.REMOTE_SERVICE,
                    work.outputData.getString(BackgroundSearchWorker.KEY_ERROR) ?: "后台搜索未完成",
                ),
            )
            else -> null
        }
        _uiState.update {
            it.copy(
                isSearching = false,
                isBackgroundSearching = false,
                outcome = outcome ?: it.outcome,
                message = if (work.state == WorkInfo.State.CANCELLED) "后台搜索已取消" else it.message,
            )
        }
        container.settingsRepository.markBackgroundResultConsumed(workId)
    }

    fun setEngineEnabled(id: String, enabled: Boolean) {
        if (!enabled && _uiState.value.engines.count { it.enabled } <= 1) {
            showMessage("至少保留一个搜索引擎")
            return
        }
        viewModelScope.launch { container.settingsRepository.setEngineEnabled(id, enabled) }
    }

    fun setDefaultEngine(id: String) {
        viewModelScope.launch {
            container.settingsRepository.setDefaultEngine(id)
            selectEngine(id)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setDynamicColor(enabled) }
    }

    fun setStripMetadata(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setStripMetadata(enabled) }
    }

    fun setJpegQuality(quality: Int) {
        viewModelScope.launch { container.settingsRepository.setJpegQuality(quality) }
    }

    fun saveCredential(id: String, value: String) {
        container.credentialStore.put(id, value)
        _uiState.update {
            it.copy(
                configuredCredentials = configuredCredentialIds(),
                message = if (value.isBlank()) "凭据已移除" else "凭据已安全保存",
            )
        }
    }

    fun credentialValue(id: String): String? = container.credentialStore.get(id)

    fun saveCustomEngine(engine: CustomEngine) {
        val existing = _uiState.value.engines.firstOrNull { it.customEngine?.id == engine.id }
        if (existing?.enabled == true && !engine.enabled && _uiState.value.engines.count { it.enabled } <= 1) {
            showMessage("至少保留一个搜索引擎")
            return
        }
        viewModelScope.launch {
            runCatching { container.customEngineRepository.save(engine) }
                .onSuccess { _uiState.update { it.copy(message = "自定义引擎已保存") } }
                .onFailure { error -> _uiState.update { it.copy(message = error.message ?: "无法保存自定义引擎") } }
        }
    }

    fun deleteCustomEngine(engine: CustomEngine) {
        val existing = _uiState.value.engines.firstOrNull { it.customEngine?.id == engine.id }
        if (existing?.enabled == true && _uiState.value.engines.count { it.enabled } <= 1) {
            showMessage("至少保留一个搜索引擎")
            return
        }
        viewModelScope.launch {
            container.customEngineRepository.delete(engine)
            _uiState.update { it.copy(message = "自定义引擎已移除") }
        }
    }

    fun clearOutcome() = _uiState.update { it.copy(outcome = null) }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun showMessage(message: String) = _uiState.update { it.copy(message = message) }

    private fun configuredCredentialIds(): Set<String> = EngineCatalog.builtIns
        .flatMap { it.credentialFields }
        .map { it.id }
        .filterTo(mutableSetOf(), container.credentialStore::has)

    class Factory(
        private val application: Application,
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppViewModel(application, container) as T
        }
    }
}

private fun CustomEngine.toDescriptor() = EngineDescriptor(
    id = stableId,
    name = name,
    summary = "自定义 HTTPS multipart 搜索端点",
    category = EngineCategory.GENERAL,
    mode = EngineMode.CUSTOM,
    badges = setOf(EngineBadge.AUTOMATIC),
    officialUrl = endpoint,
)
