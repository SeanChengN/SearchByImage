package io.github.seancheng.searchbyimage.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRepositoryTest {
    @Test
    fun oldSavedOrderMovesSauceNaoWebNextToApiMode() {
        assertEquals(
            listOf("trace_moe", "saucenao", "saucenao_web", "iqdb"),
            normalizeEngineOrder(listOf("trace_moe", "saucenao", "iqdb", "saucenao_web")),
        )
    }

    @Test
    fun normalizationKeepsOtherOrderAndRemovesDuplicates() {
        assertEquals(
            listOf("yandex", "iqdb"),
            normalizeEngineOrder(listOf("yandex", "iqdb", "yandex")),
        )
    }
}
