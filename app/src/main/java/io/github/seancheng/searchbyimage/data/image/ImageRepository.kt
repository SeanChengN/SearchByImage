package io.github.seancheng.searchbyimage.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import io.github.seancheng.searchbyimage.domain.PreparedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageRepository(private val context: Context) {
    private val imageDirectory = File(context.cacheDir, "search-images")

    suspend fun prepare(source: Uri, jpegQuality: Int = 90): PreparedImage = withContext(Dispatchers.IO) {
        imageDirectory.mkdirs()
        cleanExpiredFiles()

        val raw = File(imageDirectory, "raw-${UUID.randomUUID()}.img")
        try {
            copyWithLimit(source, raw)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(raw.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法识别图片内容" }
            require(bounds.outWidth <= MAX_SOURCE_DIMENSION && bounds.outHeight <= MAX_SOURCE_DIMENSION) {
                "图片尺寸过大，最长边不能超过 $MAX_SOURCE_DIMENSION 像素"
            }

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = requireNotNull(BitmapFactory.decodeFile(raw.absolutePath, options)) { "图片解码失败" }
            val oriented = applyExifOrientation(decoded, raw)
            if (oriented !== decoded) decoded.recycle()

            val hasAlpha = oriented.hasAlpha()
            val output = File(imageDirectory, "prepared-${UUID.randomUUID()}.${if (hasAlpha) "png" else "jpg"}")
            FileOutputStream(output).use { stream ->
                val format = if (hasAlpha) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                check(oriented.compress(format, jpegQuality.coerceIn(70, 96), stream)) { "无法写入处理后的图片" }
            }
            val width = oriented.width
            val height = oriented.height
            oriented.recycle()
            if (output.length() > MAX_PREPARED_BYTES) {
                output.delete()
                error("处理后的图片仍然过大，请先裁剪后重试")
            }

            val mime = if (hasAlpha) "image/png" else "image/jpeg"
            PreparedImage(
                file = output,
                contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    output,
                ),
                mimeType = mime,
                width = width,
                height = height,
                byteCount = output.length(),
            )
        } finally {
            raw.delete()
        }
    }

    suspend fun cleanAll() = withContext(Dispatchers.IO) {
        imageDirectory.listFiles()?.forEach { it.delete() }
    }

    private fun copyWithLimit(source: Uri, destination: File) {
        val declaredMime = context.contentResolver.getType(source)
        require(
            declaredMime == null ||
                declaredMime.startsWith("image/") ||
                declaredMime == "application/octet-stream"
        ) { "所选文件不是图片" }
        val input = requireNotNull(context.contentResolver.openInputStream(source)) { "无法读取所选图片" }
        input.use { sourceStream ->
            FileOutputStream(destination).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val read = sourceStream.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= MAX_SOURCE_BYTES) { "图片不能超过 ${MAX_SOURCE_BYTES / 1024 / 1024} MB" }
                    output.write(buffer, 0, read)
                }
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / sample > MAX_OUTPUT_DIMENSION || height / sample > MAX_OUTPUT_DIMENSION) {
            sample *= 2
        }
        return sample
    }

    private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cleanExpiredFiles() {
        val cutoff = System.currentTimeMillis() - CACHE_LIFETIME_MS
        imageDirectory.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_SOURCE_BYTES = 25L * 1024 * 1024
        const val MAX_PREPARED_BYTES = 20L * 1024 * 1024
        const val MAX_SOURCE_DIMENSION = 16_384
        const val MAX_OUTPUT_DIMENSION = 4_096
        const val CACHE_LIFETIME_MS = 24L * 60 * 60 * 1_000
    }
}
