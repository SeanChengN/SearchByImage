package io.github.seancheng.searchbyimage.ui

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.github.seancheng.searchbyimage.MainActivity
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import io.github.seancheng.searchbyimage.domain.NativeResultItem
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.ui.theme.SearchByImageTheme
import org.junit.Rule
import org.junit.Test

class ResultsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun nativeResultShowsPreviewAndUsefulDetails() {
        val traceMoe = requireNotNull(EngineCatalog.find("trace_moe"))
        val outcome = SearchOutcome.NativeResults(
            traceMoe,
            listOf(
                NativeResultItem(
                    title = "测试动画",
                    subtitle = "第 3 集 · 02:05",
                    thumbnailUrl = "https://cdn.example.com/frame.jpg",
                    sourceUrl = "https://anilist.co/anime/1",
                    similarity = 93.4,
                ),
            ),
        )
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                SearchByImageTheme(darkTheme = false, dynamicColor = false) {
                    ResultsScreen(outcome = outcome, onBack = {}, onOpenUrl = {})
                }
            }
        }

        composeRule.onNodeWithContentDescription("测试动画 匹配画面").assertIsDisplayed()
        composeRule.onNodeWithText("第 3 集 · 02:05").assertIsDisplayed()
        composeRule.onNodeWithText("相似度 93.4%").assertIsDisplayed()
    }
}
