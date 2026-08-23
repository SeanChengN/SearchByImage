package io.github.seancheng.searchbyimage.domain

import org.junit.Assert.assertEquals
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
}
