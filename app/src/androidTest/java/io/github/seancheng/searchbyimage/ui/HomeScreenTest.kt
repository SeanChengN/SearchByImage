package io.github.seancheng.searchbyimage.ui

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.seancheng.searchbyimage.AppUiState
import io.github.seancheng.searchbyimage.EngineItem
import io.github.seancheng.searchbyimage.engineGuidanceFor
import io.github.seancheng.searchbyimage.MainActivity
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.ui.theme.SearchByImageTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun emptyStateExplainsFirstAction() {
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = false, dynamicColor = false) {
                    HomeScreen(
                        state = AppUiState(),
                        onPickImage = {},
                        onCrop = {},
                        onSelectEngine = {},
                        onPrimaryAction = {},
                        onCancel = {},
                        onContinueInBackground = {},
                        onOpenOutcome = {},
                        onOpenEngines = {},
                        onOpenSettings = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("从一张图片开始").assertIsDisplayed()
        composeRule.onNodeWithText("选择图片").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("等待选择图片的扫描预览").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择引擎").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun authOutcomeExplainsTheNextStep() {
        val sauce = requireNotNull(EngineCatalog.find("saucenao"))
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = true, dynamicColor = false) {
                    HomeScreen(
                        state = AppUiState(
                            outcome = SearchOutcome.AuthRequired(sauce, sauce.credentialFields),
                        ),
                        onPickImage = {},
                        onCrop = {},
                        onSelectEngine = {},
                        onPrimaryAction = {},
                        onCancel = {},
                        onContinueInBackground = {},
                        onOpenOutcome = {},
                        onOpenEngines = {},
                        onOpenSettings = {},
                    )
                }
            }
        }
        composeRule.onNodeWithText("需要 API 凭据").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("配置凭据").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun assistedEngineGuidanceIsVisibleBeforeSearch() {
        val yandex = requireNotNull(EngineCatalog.find("yandex"))
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = false, dynamicColor = false) {
                    HomeScreen(
                        state = AppUiState(
                            engines = listOf(EngineItem(yandex, enabled = true)),
                            selectedEngineId = yandex.id,
                            guidance = engineGuidanceFor(yandex, emptySet()),
                        ),
                        onPickImage = {},
                        onCrop = {},
                        onSelectEngine = {},
                        onPrimaryAction = {},
                        onCancel = {},
                        onContinueInBackground = {},
                        onOpenOutcome = {},
                        onOpenEngines = {},
                        onOpenSettings = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("需要在网页中重新选择图片").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("打开 Yandex Images").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("网页辅助").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Yandex Images Logo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun vectorAndRasterEnginesUseTheirBundledLogos() {
        val lens = requireNotNull(EngineCatalog.find("google_lens"))
        val tineye = requireNotNull(EngineCatalog.find("tineye"))
        val sauceWeb = requireNotNull(EngineCatalog.find("saucenao_web"))
        val iqdb3d = requireNotNull(EngineCatalog.find("iqdb_3d"))
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = false, dynamicColor = false) {
                    Column {
                        EngineBrandMark(lens)
                        EngineBrandMark(tineye)
                        EngineBrandMark(sauceWeb)
                        EngineBrandMark(iqdb3d)
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Google Lens Logo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("TinEye Logo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("SauceNAO 网页版 Logo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("3D IQDB 通用图标").assertIsDisplayed()
    }
}
