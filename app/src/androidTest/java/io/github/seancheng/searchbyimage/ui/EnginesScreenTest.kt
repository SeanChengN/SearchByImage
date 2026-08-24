package io.github.seancheng.searchbyimage.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.seancheng.searchbyimage.AppUiState
import io.github.seancheng.searchbyimage.EngineItem
import io.github.seancheng.searchbyimage.MainActivity
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.ui.theme.SearchByImageTheme
import org.junit.Rule
import org.junit.Test

class EnginesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun categoriesStartCollapsedAndCanBeExpandedIndependently() {
        val engines = EngineCatalog.builtIns.map { EngineItem(it, enabled = it.defaultEnabled) }
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = false, dynamicColor = false) {
                    EnginesScreen(
                        state = AppUiState(engines = engines),
                        onBack = {},
                        onToggle = { _, _ -> },
                        onDefault = {},
                        onCredentials = {},
                        onAddCustom = {},
                        onEditCustom = {},
                        onOpenUrl = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("通用搜索").assertIsDisplayed()
        composeRule.onNodeWithText("动漫与插画").assertIsDisplayed()
        composeRule.onNodeWithText("来源与版权").assertIsDisplayed()
        composeRule.onNodeWithText("SauceNAO").assertDoesNotExist()

        composeRule.onNodeWithContentDescription("展开动漫与插画").performClick()
        val engineList = composeRule.onNodeWithTag("engine-list")
        engineList.performScrollToNode(hasText("SauceNAO"))
        composeRule.onNodeWithText("SauceNAO").assertIsDisplayed()
        engineList.performScrollToNode(hasText("动漫与插画"))
        composeRule.onNodeWithContentDescription("折叠动漫与插画").performClick()
        composeRule.onNodeWithText("SauceNAO").assertDoesNotExist()
    }
}
