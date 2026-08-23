package io.github.seancheng.searchbyimage.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    sourceUri: Uri,
    sourceMimeType: String,
    jpegQuality: Int,
    onBack: () -> Unit,
    onCropped: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val outputFormat = if (sourceMimeType == "image/png") {
        Bitmap.CompressFormat.PNG
    } else {
        Bitmap.CompressFormat.JPEG
    }
    var cropView by remember(sourceUri) { mutableStateOf<CropImageView?>(null) }
    var isLoading by remember(sourceUri) { mutableStateOf(true) }
    var isCropping by remember(sourceUri) { mutableStateOf(false) }
    var pendingOutputUri by remember(sourceUri) { mutableStateOf<Uri?>(null) }
    var errorMessage by remember(sourceUri) { mutableStateOf<String?>(null) }

    DisposableEffect(sourceUri) {
        onDispose { cropView?.clearImage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("裁剪图片") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isCropping) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { cropView?.rotateImage(-90) },
                        enabled = !isLoading && !isCropping,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "向左旋转")
                    }
                    IconButton(
                        onClick = { cropView?.rotateImage(90) },
                        enabled = !isLoading && !isCropping,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "向右旋转")
                    }
                    IconButton(
                        onClick = {
                            val view = cropView ?: return@IconButton
                            runCatching {
                                createCropOutputUri(
                                    context = context,
                                    extension = if (outputFormat == Bitmap.CompressFormat.PNG) "png" else "jpg",
                                )
                            }.onSuccess { outputUri ->
                                pendingOutputUri = outputUri
                                errorMessage = null
                                isCropping = true
                                view.croppedImageAsync(
                                    outputFormat,
                                    jpegQuality.coerceIn(70, 96),
                                    0,
                                    0,
                                    CropImageView.RequestSizeOptions.NONE,
                                    outputUri,
                                )
                            }.onFailure { error ->
                                errorMessage = error.message ?: "无法创建裁剪文件"
                            }
                        },
                        enabled = !isLoading && !isCropping,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "完成裁剪")
                    }
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    CropImageView(viewContext).apply {
                        setImageCropOptions(
                            CropImageOptions(
                                guidelines = CropImageView.Guidelines.ON,
                                fixAspectRatio = false,
                                autoZoomEnabled = true,
                                multiTouchEnabled = true,
                                showProgressBar = true,
                            ),
                        )
                        setOnSetImageUriCompleteListener { _, _, error ->
                            isLoading = false
                            if (error != null) errorMessage = error.message ?: "无法载入图片"
                        }
                        setOnCropImageCompleteListener { _, result ->
                            isCropping = false
                            if (result.isSuccessful) {
                                val outputUri = result.uriContent ?: pendingOutputUri
                                if (outputUri != null) {
                                    onCropped(outputUri)
                                } else {
                                    errorMessage = "裁剪结果缺少输出地址"
                                }
                            } else {
                                errorMessage = result.error?.message ?: "裁剪失败"
                            }
                        }
                        cropView = this
                        setImageUriAsync(sourceUri)
                    }
                },
            )

            if (isLoading || isCropping) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }
        }
    }
}

private fun createCropOutputUri(context: android.content.Context, extension: String): Uri {
    val directory = File(context.cacheDir, "search-images")
    check(directory.isDirectory || directory.mkdirs()) { "无法创建图片缓存目录" }
    val output = File(directory, "crop-${UUID.randomUUID()}.$extension")
    check(output.createNewFile()) { "无法创建裁剪输出文件" }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        output,
    )
}
