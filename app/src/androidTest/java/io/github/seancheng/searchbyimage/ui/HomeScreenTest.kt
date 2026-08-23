package io.github.seancheng.searchbyimage.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.seancheng.searchbyimage.AppUiState
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
                        onSearch = {},
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
                        onSearch = {},
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
}
