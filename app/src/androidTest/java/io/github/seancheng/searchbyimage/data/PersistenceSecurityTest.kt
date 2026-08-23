package io.github.seancheng.searchbyimage.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.seancheng.searchbyimage.data.db.CustomEngine
import io.github.seancheng.searchbyimage.data.db.CustomEngineRepository
import io.github.seancheng.searchbyimage.data.db.SearchDatabase
import io.github.seancheng.searchbyimage.data.security.CredentialStore
import io.github.seancheng.searchbyimage.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistenceSecurityTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun credentialRoundTripDoesNotStorePlaintext() {
        val fieldId = "instrumentation.secret"
        val plaintext = "not-a-real-api-key"
        val store = CredentialStore(context)
        try {
            store.put(fieldId, plaintext)
            assertEquals(plaintext, store.get(fieldId))
            val encoded = context
                .getSharedPreferences("encrypted_api_credentials_v3", android.content.Context.MODE_PRIVATE)
                .getString(fieldId, null)
            assertNotEquals(plaintext, encoded)
            assertFalse(encoded.orEmpty().contains(plaintext))
        } finally {
            store.remove(fieldId)
        }
    }

    @Test
    fun roomStoresOnlyValidatedCustomEngines() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, SearchDatabase::class.java).build()
        try {
            val repository = CustomEngineRepository(database.customEngineDao())
            repository.save(
                CustomEngine(
                    name = "Fixture",
                    endpoint = "https://example.com/search",
                    staticFields = mapOf("mode" to "image"),
                ),
            )
            assertEquals("Fixture", repository.engines.first().single().name)
            assertTrue(
                runCatching {
                    repository.save(CustomEngine(name = "Unsafe", endpoint = "http://127.0.0.1/search"))
                }.isFailure,
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun datastorePersistsUiAndCompressionSettings() = runBlocking {
        val repository = SettingsRepository(context)
        try {
            repository.setDynamicColor(false)
            repository.setJpegQuality(83)
            val settings = repository.settings.first { !it.dynamicColor && it.jpegQuality == 83 }
            assertFalse(settings.dynamicColor)
            assertEquals(83, settings.jpegQuality)
        } finally {
            repository.setDynamicColor(true)
            repository.setJpegQuality(90)
        }
    }
}
