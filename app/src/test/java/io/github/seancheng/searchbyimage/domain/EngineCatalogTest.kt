package io.github.seancheng.searchbyimage.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineCatalogTest {
    @Test
    fun identifiersAreUniqueAndStable() {
        assertTrue(EngineCatalog.VERSION > 0)
        val ids = EngineCatalog.builtIns.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        assertTrue(ids.containsAll(listOf("google_lens", "trace_moe", "saucenao", "lenso", "tineye")))
    }

    @Test
    fun everyOfficialAddressUsesHttps() {
        assertTrue(EngineCatalog.builtIns.all { it.officialUrl.startsWith("https://") })
    }

    @Test
    fun atLeastOneDefaultEngineNeedsNoCredential() {
        assertTrue(EngineCatalog.builtIns.any { it.defaultEnabled && it.credentialFields.isEmpty() })
    }

    @Test
    fun unusableWebEnginesAreNotBuiltIn() {
        assertNull(EngineCatalog.find("bing"))
        assertNull(EngineCatalog.find("so360"))
    }

    @Test
    fun optionalWebEnginesUseTheirOfficialHttpsHomepages() {
        assertEquals("https://3d.iqdb.org/", requireNotNull(EngineCatalog.find("iqdb_3d")).officialUrl)
        assertEquals("https://saucenao.com/", requireNotNull(EngineCatalog.find("saucenao_web")).officialUrl)
    }

    @Test
    fun enginesAreGroupedByTheirPrimarySearchPurpose() {
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("trace_moe")).category)
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("saucenao")).category)
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("saucenao_web")).category)
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("iqdb")).category)
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("iqdb_3d")).category)
        assertEquals(EngineCategory.ANIME, requireNotNull(EngineCatalog.find("ascii2d")).category)
        assertEquals(EngineCategory.SOURCE, requireNotNull(EngineCatalog.find("lenso")).category)
        assertEquals(EngineCategory.SOURCE, requireNotNull(EngineCatalog.find("tineye")).category)
    }

    @Test
    fun sauceNaoApiAndWebModesAreAdjacent() {
        val ids = EngineCatalog.builtIns.map { it.id }
        assertEquals(ids.indexOf("saucenao") + 1, ids.indexOf("saucenao_web"))
    }
}
