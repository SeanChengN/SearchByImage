package io.github.seancheng.searchbyimage

import android.content.Context
import androidx.room.Room
import io.github.seancheng.searchbyimage.data.db.CustomEngineRepository
import io.github.seancheng.searchbyimage.data.db.SearchDatabase
import io.github.seancheng.searchbyimage.data.image.ImageRepository
import io.github.seancheng.searchbyimage.data.network.SearchCoordinator
import io.github.seancheng.searchbyimage.data.security.CredentialStore
import io.github.seancheng.searchbyimage.data.settings.SettingsRepository

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val database: SearchDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SearchDatabase::class.java,
            "search-by-image-v3.db",
        ).build()
    }
    val customEngineRepository by lazy { CustomEngineRepository(database.customEngineDao()) }
    val settingsRepository by lazy { SettingsRepository(applicationContext) }
    val credentialStore by lazy { CredentialStore(applicationContext) }
    val imageRepository by lazy { ImageRepository(applicationContext) }
    val searchCoordinator by lazy { SearchCoordinator(credentialStore, customEngineRepository) }
}
