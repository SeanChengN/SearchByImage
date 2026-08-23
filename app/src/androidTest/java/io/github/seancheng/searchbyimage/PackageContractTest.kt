package io.github.seancheng.searchbyimage

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageContractTest {
    @Test
    fun usesNewApplicationIdAndNoStoragePermissions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue(context.packageName.startsWith("io.github.seancheng.searchbyimage"))
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toSet()
        assertFalse(Manifest.permission.READ_EXTERNAL_STORAGE in requested)
        assertFalse(Manifest.permission.WRITE_EXTERNAL_STORAGE in requested)
        assertTrue(Manifest.permission.INTERNET in requested)
    }
}
