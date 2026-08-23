package io.github.seancheng.searchbyimage.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.seancheng.searchbyimage.R
import io.github.seancheng.searchbyimage.SearchByImageApplication
import io.github.seancheng.searchbyimage.domain.PreparedImage
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import java.io.File

class BackgroundSearchWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val engineId = inputData.getString(KEY_ENGINE_ID) ?: return Result.failure()
        val path = inputData.getString(KEY_IMAGE_PATH) ?: return Result.failure()
        val allowedDirectory = File(applicationContext.cacheDir, "search-images").canonicalFile
        val file = runCatching { File(path).canonicalFile }.getOrNull()
            ?: return Result.failure(errorData("待搜索图片路径无效", engineId))
        if (
            !file.isFile ||
            !file.path.startsWith(allowedDirectory.path + File.separator, ignoreCase = true)
        ) {
            return Result.failure(errorData("待搜索图片已失效", engineId))
        }
        setForeground(createForegroundInfo("正在搜索图片…"))

        val image = PreparedImage(
            file = file,
            contentUri = FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                file,
            ),
            mimeType = inputData.getString(KEY_MIME_TYPE) ?: "image/jpeg",
            width = inputData.getInt(KEY_WIDTH, 0),
            height = inputData.getInt(KEY_HEIGHT, 0),
            byteCount = file.length(),
        )
        val container = (applicationContext as SearchByImageApplication).container
        return when (val outcome = container.searchCoordinator.search(engineId, image)) {
            is SearchOutcome.NativeResults -> Result.success(
                Data.Builder()
                    .putString(KEY_RESULT_KIND, "native")
                    .putString(KEY_RESULT_ENGINE_ID, engineId)
                    .putInt(KEY_RESULT_COUNT, outcome.items.size)
                    .putString(KEY_RESULT_URL, outcome.items.firstOrNull()?.sourceUrl)
                    .build(),
            )
            is SearchOutcome.WebResult -> Result.success(resultData("web", outcome.url, engineId))
            is SearchOutcome.AssistedWeb -> Result.success(resultData("assisted", outcome.url, engineId))
            is SearchOutcome.ExternalApp -> Result.success(resultData("external", outcome.fallbackUrl, engineId))
            is SearchOutcome.AuthRequired -> Result.failure(errorData("缺少 ${outcome.engine.name} 的 API 凭据", engineId))
            is SearchOutcome.Error -> {
                if (outcome.failure.retryable && runAttemptCount < 2) Result.retry()
                else Result.failure(errorData(outcome.failure.message, engineId))
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo("准备后台搜索…")

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification: Notification = NotificationCompat.Builder(
            applicationContext,
            SearchByImageApplication.SEARCH_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_app_icon)
            .setContentTitle("Search By Image")
            .setContentText(message)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .addAction(0, "取消", cancelIntent)
            .build()
        return ForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun resultData(kind: String, url: String, engineId: String) = Data.Builder()
        .putString(KEY_RESULT_KIND, kind)
        .putString(KEY_RESULT_ENGINE_ID, engineId)
        .putString(KEY_RESULT_URL, url)
        .build()

    private fun errorData(message: String, engineId: String) = Data.Builder()
        .putString(KEY_RESULT_ENGINE_ID, engineId)
        .putString(KEY_ERROR, message)
        .build()

    companion object {
        const val KEY_ENGINE_ID = "engine_id"
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_MIME_TYPE = "mime_type"
        const val KEY_WIDTH = "width"
        const val KEY_HEIGHT = "height"
        const val KEY_RESULT_KIND = "result_kind"
        const val KEY_RESULT_ENGINE_ID = "result_engine_id"
        const val KEY_RESULT_COUNT = "result_count"
        const val KEY_RESULT_URL = "result_url"
        const val KEY_ERROR = "error"
        const val UNIQUE_WORK_NAME = "single_image_search"
        private const val NOTIFICATION_ID = 34001
    }
}
