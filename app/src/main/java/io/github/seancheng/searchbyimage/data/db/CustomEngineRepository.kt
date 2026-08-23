package io.github.seancheng.searchbyimage.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

data class CustomEngine(
    val id: Long = 0,
    val name: String,
    val endpoint: String,
    val fileField: String = "file",
    val staticFields: Map<String, String> = emptyMap(),
    val resultUrlJsonField: String = "url",
    val enabled: Boolean = true,
    val sortOrder: Int = 0,
) {
    val stableId: String get() = "custom:$id"
}

class CustomEngineRepository(private val dao: CustomEngineDao) {
    val engines: Flow<List<CustomEngine>> = dao.observeAll().map { entities ->
        entities.map(CustomEngineEntity::toModel)
    }

    suspend fun find(id: Long): CustomEngine? = dao.find(id)?.toModel()

    suspend fun save(engine: CustomEngine): Long {
        val endpointValidation = EndpointValidator.validate(engine.endpoint)
        require(endpointValidation.isSuccess) { endpointValidation.exceptionOrNull()?.message ?: "端点无效" }
        require(engine.name.trim().length in 2..40) { "名称应为 2–40 个字符" }
        require(engine.fileField.matches(Regex("[A-Za-z0-9_.-]{1,64}"))) { "文件字段名无效" }
        require(engine.resultUrlJsonField.matches(Regex("[A-Za-z0-9_.-]{1,64}"))) { "结果字段名无效" }
        require(engine.staticFields.size <= 20) { "静态字段最多 20 个" }
        engine.staticFields.forEach { (key, value) ->
            require(key.matches(Regex("[A-Za-z0-9_.-]{1,64}"))) { "静态字段名无效" }
            require(value.length <= 512) { "静态字段值最多 512 个字符" }
        }
        return dao.save(engine.toEntity())
    }

    suspend fun delete(engine: CustomEngine) = dao.delete(engine.toEntity())
}

private fun CustomEngineEntity.toModel() = CustomEngine(
    id = id,
    name = name,
    endpoint = endpoint,
    fileField = fileField,
    staticFields = JSONObject(staticFieldsJson).let { json ->
        json.keys().asSequence().associateWith { key -> json.optString(key) }
    },
    resultUrlJsonField = resultUrlJsonField,
    enabled = enabled,
    sortOrder = sortOrder,
)

private fun CustomEngine.toEntity() = CustomEngineEntity(
    id = id,
    name = name.trim(),
    endpoint = endpoint.trim(),
    fileField = fileField.trim(),
    staticFieldsJson = JSONObject(staticFields).toString(),
    resultUrlJsonField = resultUrlJsonField.trim(),
    enabled = enabled,
    sortOrder = sortOrder,
)
