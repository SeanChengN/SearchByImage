package io.github.seancheng.searchbyimage

import io.github.seancheng.searchbyimage.domain.CredentialIds
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineGuidanceTest {
    @Test
    fun missingCredentialIsReportedBeforeSearching() {
        val sauceNao = requireNotNull(EngineCatalog.find("saucenao"))
        val guidance = requireNotNull(engineGuidanceFor(sauceNao, emptySet()))

        assertEquals(EngineGuidanceAction.CONFIGURE_CREDENTIALS, guidance.action)
        assertEquals("配置凭据", guidance.primaryActionLabel)
    }

    @Test
    fun configuredApiEngineCanSearch() {
        val sauceNao = requireNotNull(EngineCatalog.find("saucenao"))
        val guidance = requireNotNull(
            engineGuidanceFor(sauceNao, setOf(CredentialIds.SAUCENAO_API_KEY)),
        )

        assertEquals(EngineGuidanceAction.SEARCH, guidance.action)
        assertTrue("SauceNAO" in guidance.primaryActionLabel)
    }

    @Test
    fun externalAndWebEnginesHaveDirectActions() {
        val lens = requireNotNull(engineGuidanceFor(EngineCatalog.find("google_lens"), emptySet()))
        val yandex = requireNotNull(engineGuidanceFor(EngineCatalog.find("yandex"), emptySet()))

        assertEquals(EngineGuidanceAction.OPEN_EXTERNAL, lens.action)
        assertEquals(EngineGuidanceAction.OPEN_WEB, yandex.action)
        assertEquals("打开 Yandex Images", yandex.primaryActionLabel)
        assertTrue("重新选择原图" in yandex.detail)
    }

    @Test
    fun desktopOnlyChineseEnginesGiveAccurateMobileInstructions() {
        val baidu = requireNotNull(engineGuidanceFor(EngineCatalog.find("baidu"), emptySet()))
        val sogou = requireNotNull(engineGuidanceFor(EngineCatalog.find("sogou"), emptySet()))

        assertTrue("桌面式布局" in baidu.detail)
        assertTrue("桌面式布局" in sogou.detail)
    }

    @Test
    fun optionalWebEnginesExplainThatTheImageMustBeSelectedAgain() {
        val iqdb3d = requireNotNull(engineGuidanceFor(EngineCatalog.find("iqdb_3d"), emptySet()))
        val sauceWeb = requireNotNull(engineGuidanceFor(EngineCatalog.find("saucenao_web"), emptySet()))

        assertEquals("打开 3D IQDB", iqdb3d.primaryActionLabel)
        assertTrue("选择同一张原图" in iqdb3d.detail)
        assertEquals("打开 SauceNAO 网页版", sauceWeb.primaryActionLabel)
        assertTrue("无需 API Key" in sauceWeb.detail)
    }
}
