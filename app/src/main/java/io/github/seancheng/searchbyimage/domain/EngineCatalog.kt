package io.github.seancheng.searchbyimage.domain

object CredentialIds {
    const val SAUCENAO_API_KEY = "saucenao.api_key"
    const val LENSO_API_KEY = "lenso.api_key"
    const val TINEYE_API_KEY = "tineye.api_key"
}

object EngineCatalog {
    const val VERSION = 1

    val builtIns: List<EngineDescriptor> = listOf(
        EngineDescriptor(
            id = "google_lens",
            name = "Google Lens",
            summary = "交给 Google 应用识别；不可用时打开官网。",
            category = EngineCategory.GENERAL,
            mode = EngineMode.EXTERNAL_APP,
            badges = setOf(EngineBadge.AUTOMATIC),
            officialUrl = "https://lens.google.com/",
            termsUrl = "https://policies.google.com/terms",
            defaultEnabled = true,
        ),
        EngineDescriptor(
            id = "trace_moe",
            name = "trace.moe",
            summary = "定位动画截图的作品、集数和时间点。",
            category = EngineCategory.ANIME,
            mode = EngineMode.API,
            badges = setOf(EngineBadge.AUTOMATIC),
            officialUrl = "https://trace.moe/",
            termsUrl = "https://trace.moe/terms",
            defaultEnabled = true,
        ),
        EngineDescriptor(
            id = "saucenao",
            name = "SauceNAO",
            summary = "使用个人 API Key 返回插画与动漫来源。",
            category = EngineCategory.SOURCE,
            mode = EngineMode.API,
            badges = setOf(EngineBadge.AUTOMATIC, EngineBadge.API_KEY),
            officialUrl = "https://saucenao.com/",
            termsUrl = "https://saucenao.com/terms.php",
            credentialFields = listOf(CredentialField(CredentialIds.SAUCENAO_API_KEY, "API Key")),
        ),
        EngineDescriptor(
            id = "lenso",
            name = "Lenso.ai",
            summary = "使用开发者订阅搜索重复、地标和相似图片。",
            category = EngineCategory.SOURCE,
            mode = EngineMode.API,
            badges = setOf(EngineBadge.AUTOMATIC, EngineBadge.API_KEY, EngineBadge.PAID),
            officialUrl = "https://lenso.ai/en",
            termsUrl = "https://lenso.ai/en/terms",
            credentialFields = listOf(CredentialField(CredentialIds.LENSO_API_KEY, "Authorization Token")),
        ),
        EngineDescriptor(
            id = "tineye",
            name = "TinEye",
            summary = "使用个人商业 API Key 查找图片出现过的网页。",
            category = EngineCategory.SOURCE,
            mode = EngineMode.API,
            badges = setOf(EngineBadge.AUTOMATIC, EngineBadge.API_KEY, EngineBadge.PAID),
            officialUrl = "https://tineye.com/",
            termsUrl = "https://tineye.com/terms",
            credentialFields = listOf(CredentialField(CredentialIds.TINEYE_API_KEY, "API Key")),
        ),
        assisted(
            id = "bing",
            name = "Bing Visual Search",
            summary = "打开 Bing 移动页面并手动选择图片。",
            category = EngineCategory.GENERAL,
            url = "https://www.bing.com/visualsearch",
            defaultEnabled = true,
        ),
        assisted(
            id = "yandex",
            name = "Yandex Images",
            summary = "打开 Yandex 图片搜索并手动上传。",
            category = EngineCategory.GENERAL,
            url = "https://yandex.com/images/",
            defaultEnabled = true,
        ),
        assisted(
            id = "baidu",
            name = "百度识图",
            summary = "打开百度识图并手动选择图片。",
            category = EngineCategory.GENERAL,
            url = "https://graph.baidu.com/pcpage/index",
            defaultEnabled = true,
        ),
        assisted(
            id = "sogou",
            name = "搜狗识图",
            summary = "打开搜狗图片搜索并手动选择图片。",
            category = EngineCategory.GENERAL,
            url = "https://pic.sogou.com/ris",
        ),
        assisted(
            id = "so360",
            name = "360 识图",
            summary = "打开 360 图片搜索并手动选择图片。",
            category = EngineCategory.GENERAL,
            url = "https://st.so.com/",
        ),
        assisted(
            id = "iqdb",
            name = "IQDB",
            summary = "面向动漫与插画图库的相似图片搜索。",
            category = EngineCategory.ANIME,
            url = "https://iqdb.org/",
            defaultEnabled = true,
        ),
        assisted(
            id = "ascii2d",
            name = "Ascii2D",
            summary = "按颜色或特征搜索插画来源。",
            category = EngineCategory.ANIME,
            url = "https://ascii2d.net/",
        ),
    )

    fun find(id: String): EngineDescriptor? = builtIns.firstOrNull { it.id == id }

    private fun assisted(
        id: String,
        name: String,
        summary: String,
        category: EngineCategory,
        url: String,
        defaultEnabled: Boolean = false,
    ) = EngineDescriptor(
        id = id,
        name = name,
        summary = summary,
        category = category,
        mode = EngineMode.ASSISTED_WEB,
        badges = setOf(EngineBadge.ASSISTED),
        officialUrl = url,
        defaultEnabled = defaultEnabled,
    )
}
