package io.github.seancheng.searchbyimage.data.image

import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageRepositoryTest {
    @Test
    fun preparesImageAndRemovesExifMetadata() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "search-images").apply { mkdirs() }
        val input = File(directory, "instrumentation-input.jpg")
        val bitmap = Bitmap.createBitmap(64, 32, Bitmap.Config.ARGB_8888)
        FileOutputStream(input).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        ExifInterface(input).apply {
            setAttribute(ExifInterface.TAG_MAKE, "PRIVATE-CAMERA")
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", input)

        val prepared = ImageRepository(context).prepare(uri)

        assertEquals(32, prepared.width)
        assertEquals(64, prepared.height)
        assertTrue(prepared.file.isFile)
        assertNull(ExifInterface(prepared.file).getAttribute(ExifInterface.TAG_MAKE))
    }

    @Test
    fun acceptsTransparentPngWithoutFilenameExtensionAndCleansExpiredCache() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val directory = File(context.cacheDir, "search-images").apply { mkdirs() }
        val stale = File(directory, "prepared-stale.jpg").apply {
            writeBytes(byteArrayOf(1, 2, 3))
            setLastModified(1L)
        }
        val input = File(directory, "instrumentation-input-without-extension")
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.TRANSPARENT)
        }
        FileOutputStream(input).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", input)

        val prepared = ImageRepository(context).prepare(uri)

        assertEquals("image/png", prepared.mimeType)
        assertEquals(24, prepared.width)
        assertTrue(prepared.file.isFile)
        assertTrue(!stale.exists())
    }
}
