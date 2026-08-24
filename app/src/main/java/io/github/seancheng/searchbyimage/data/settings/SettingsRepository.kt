package io.github.seancheng.searchbyimage.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings_v3")

internal fun normalizeEngineOrder(ids: List<String>): List<String> {
    val normalized = ids.distinct().toMutableList()
    val apiIndex = normalized.indexOf("saucenao")
    val webIndex = normalized.indexOf("saucenao_web")
    if (apiIndex >= 0 && webIndex >= 0 && webIndex != apiIndex + 1) {
        normalized.removeAt(webIndex)
        normalized.add(normalized.indexOf("saucenao") + 1, "saucenao_web")
    }
    return normalized
}

data class AppSettings(
    val enabledEngineIds: Set<String>,
    val engineOrder: List<String>,
    val defaultEngineId: String,
    val dynamicColor: Boolean,
    val stripMetadata: Boolean,
    val jpegQuality: Int,
    val consumedBackgroundWorkId: String?,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val enabledEngines = stringPreferencesKey("enabled_engine_ids")
        val engineOrder = stringPreferencesKey("engine_order")
        val defaultEngine = stringPreferencesKey("default_engine_id")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val stripMetadata = booleanPreferencesKey("strip_metadata")
        val jpegQuality = intPreferencesKey("jpeg_quality")
        val consumedBackgroundWorkId = stringPreferencesKey("consumed_background_work_id")
    }

    private val defaultEnabled = EngineCatalog.builtIns.filter { it.defaultEnabled }.map { it.id }.toSet()
    private val defaultOrder = EngineCatalog.builtIns.map { it.id }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { preferences ->
        val enabled = preferences[Keys.enabledEngines]
            ?.split(',')
            ?.filter(String::isNotBlank)
            ?.toSet()
            ?: defaultEnabled
        val storedOrder = preferences[Keys.engineOrder]
            ?.split(',')
            ?.filter(String::isNotBlank)
            .orEmpty()
        val completeOrder = normalizeEngineOrder(storedOrder + defaultOrder)
        AppSettings(
            enabledEngineIds = enabled,
            engineOrder = completeOrder,
            defaultEngineId = preferences[Keys.defaultEngine]
                ?: enabled.firstOrNull()
                ?: "trace_moe",
            dynamicColor = preferences[Keys.dynamicColor] ?: true,
            stripMetadata = preferences[Keys.stripMetadata] ?: true,
            jpegQuality = (preferences[Keys.jpegQuality] ?: 90).coerceIn(70, 96),
            consumedBackgroundWorkId = preferences[Keys.consumedBackgroundWorkId],
        )
    }

    suspend fun setEngineEnabled(id: String, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[Keys.enabledEngines]
                ?.split(',')
                ?.filter(String::isNotBlank)
                ?.toMutableSet()
                ?: defaultEnabled.toMutableSet()
            if (enabled) current += id else current -= id
            preferences[Keys.enabledEngines] = current.joinToString(",")
            if (!enabled && preferences[Keys.defaultEngine] == id) {
                preferences[Keys.defaultEngine] = current.firstOrNull() ?: "trace_moe"
            }
        }
    }

    suspend fun setDefaultEngine(id: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.defaultEngine] = id
        }
    }

    suspend fun setEngineOrder(ids: List<String>) {
        context.settingsDataStore.edit { preferences ->
            preferences[Keys.engineOrder] = normalizeEngineOrder(ids).joinToString(",")
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setStripMetadata(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.stripMetadata] = enabled }
    }

    suspend fun setJpegQuality(quality: Int) {
        context.settingsDataStore.edit { it[Keys.jpegQuality] = quality.coerceIn(70, 96) }
    }

    suspend fun markBackgroundResultConsumed(workId: String) {
        context.settingsDataStore.edit { it[Keys.consumedBackgroundWorkId] = workId }
    }
}
