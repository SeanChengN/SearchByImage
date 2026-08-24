package io.github.seancheng.searchbyimage

import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.domain.EngineMode

enum class EngineGuidanceAction {
    SEARCH,
    OPEN_EXTERNAL,
    OPEN_WEB,
    CONFIGURE_CREDENTIALS,
}

data class EngineGuidance(
    val title: String,
    val detail: String,
    val primaryActionLabel: String,
    val action: EngineGuidanceAction,
)

fun engineGuidanceFor(
    engine: EngineDescriptor?,
    configuredCredentialIds: Set<String>,
): EngineGuidance? {
    engine ?: return null
    val missingCredentials = engine.credentialFields.filter { it.id !in configuredCredentialIds }
    if (missingCredentials.isNotEmpty()) {
        return EngineGuidance(
            title = "先配置 ${missingCredentials.joinToString { it.label }}",
            detail = "凭据只会加密保存在这台设备上，配置完成后才会向 ${engine.name} 发送图片。",
            primaryActionLabel = "配置凭据",
            action = EngineGuidanceAction.CONFIGURE_CREDENTIALS,
        )
    }

    return when (engine.mode) {
        EngineMode.EXTERNAL_APP -> EngineGuidance(
            title = "将图片交给 ${engine.name}",
            detail = "点击后会直接交接当前处理后的图片；目标应用不支持时会提示你重新选择。",
            primaryActionLabel = "用 ${engine.name} 搜索",
            action = EngineGuidanceAction.OPEN_EXTERNAL,
        )

        EngineMode.ASSISTED_WEB -> {
            assistedWebGuidance(engine)
        }

        EngineMode.API,
        EngineMode.DIRECT_UPLOAD,
        EngineMode.CUSTOM,
        -> EngineGuidance(
            title = "将图片上传到 ${engine.name}",
            detail = "只发送当前处理后的这一张图片，不会同时请求其他搜索引擎。",
            primaryActionLabel = "用 ${engine.name} 搜索",
            action = EngineGuidanceAction.SEARCH,
        )
    }
}

private fun assistedWebGuidance(engine: EngineDescriptor): EngineGuidance = when (engine.id) {
    "baidu" -> desktopWebGuidance(
        engine = engine,
        detail = "百度官方识图页采用桌面式布局。在页面中点击“本地上传”，再选择同一张原图。",
    )

    "sogou" -> desktopWebGuidance(
        engine = engine,
        detail = "搜狗官方识图页采用桌面式布局。在搜索框中点击相机图标和“上传图片”，再选择同一张原图。",
    )

    "iqdb_3d" -> EngineGuidance(
        title = "在 3D IQDB 中重新选择图片",
        detail = "将打开 3D IQDB 官网。点击文件选择按钮，再选择同一张原图。",
        primaryActionLabel = "打开 3D IQDB",
        action = EngineGuidanceAction.OPEN_WEB,
    )

    "saucenao_web" -> EngineGuidance(
        title = "在 SauceNAO 网页中重新选择图片",
        detail = "此入口无需 API Key。打开官网后点击“Select Image”，再选择同一张原图。",
        primaryActionLabel = "打开 SauceNAO 网页版",
        action = EngineGuidanceAction.OPEN_WEB,
    )

    else -> EngineGuidance(
        title = "需要在网页中重新选择图片",
        detail = "将打开 ${engine.name} 官网。浏览器不能代替你选择文件，进入后请重新选择原图。",
        primaryActionLabel = "打开 ${engine.name}",
        action = EngineGuidanceAction.OPEN_WEB,
    )
}

private fun desktopWebGuidance(
    engine: EngineDescriptor,
    detail: String,
) = EngineGuidance(
    title = "打开 ${engine.name} 桌面版识图页",
    detail = detail,
    primaryActionLabel = "打开 ${engine.name}",
    action = EngineGuidanceAction.OPEN_WEB,
)
