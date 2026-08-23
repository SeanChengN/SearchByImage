package io.github.seancheng.searchbyimage

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

class SearchByImageApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        publishDynamicShortcut()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            SEARCH_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun publishDynamicShortcut() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "$packageName.action.NEW_SEARCH"
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val shortcut = ShortcutInfoCompat.Builder(this, NEW_SEARCH_SHORTCUT_ID)
            .setShortLabel("选择图片搜索")
            .setLongLabel("选择一张图片开始搜索")
            .setIcon(IconCompat.createWithResource(this, R.drawable.ic_app_icon))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }

    companion object {
        const val SEARCH_CHANNEL_ID = "background_image_search"
        const val NEW_SEARCH_SHORTCUT_ID = "new_image_search"
    }
}
