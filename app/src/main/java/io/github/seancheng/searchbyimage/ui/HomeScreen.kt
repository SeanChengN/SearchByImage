package io.github.seancheng.searchbyimage.ui

import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.seancheng.searchbyimage.AppUiState
import io.github.seancheng.searchbyimage.EngineGuidance
import io.github.seancheng.searchbyimage.EngineGuidanceAction
import io.github.seancheng.searchbyimage.EngineItem
import io.github.seancheng.searchbyimage.domain.EngineBadge
import io.github.seancheng.searchbyimage.domain.PreparedImage
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.ui.theme.IndigoGlass
import io.github.seancheng.searchbyimage.ui.theme.PrismViolet
import io.github.seancheng.searchbyimage.ui.theme.ScanCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: AppUiState,
    onPickImage: () -> Unit,
    onCrop: () -> Unit,
    onSelectEngine: (String) -> Unit,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
    onContinueInBackground: () -> Unit,
    onOpenOutcome: (SearchOutcome) -> Unit,
    onOpenEngines: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showEngines by remember { mutableStateOf(false) }
    val enabledEngines = state.engines.filter { it.enabled }
    val selectedEngine = state.engines.firstOrNull { it.descriptor.id == state.selectedEngineId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Search By Image", fontWeight = FontWeight.Black)
                        Text(
                            "一次只把图片交给一个服务",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenEngines) {
                        Icon(Icons.Default.Tune, contentDescription = "管理搜索引擎")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val wide = maxWidth >= 840.dp
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    PreviewPanel(
                        image = state.image,
                        searching = state.isSearching,
                        preparing = state.isPreparingImage,
                        onPickImage = onPickImage,
                        onCrop = onCrop,
                        modifier = Modifier.weight(1.2f),
                    )
                    SearchControls(
                        state = state,
                        selectedEngine = selectedEngine,
                        onChooseEngine = { showEngines = true },
                        onPrimaryAction = onPrimaryAction,
                        onCancel = onCancel,
                        onContinueInBackground = onContinueInBackground,
                        onOpenOutcome = onOpenOutcome,
                        modifier = Modifier
                            .weight(0.8f)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    PreviewPanel(
                        image = state.image,
                        searching = state.isSearching,
                        preparing = state.isPreparingImage,
                        onPickImage = onPickImage,
                        onCrop = onCrop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SearchControls(
                        state = state,
                        selectedEngine = selectedEngine,
                        onChooseEngine = { showEngines = true },
                        onPrimaryAction = onPrimaryAction,
                        onCancel = onCancel,
                        onContinueInBackground = onContinueInBackground,
                        onOpenOutcome = onOpenOutcome,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showEngines) {
        ModalBottomSheet(onDismissRequest = { showEngines = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
            ) {
                Text(
                    "这张图片要交给谁？",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                )
                if (enabledEngines.isEmpty()) {
                    Text("请先在引擎管理中启用至少一个服务。", modifier = Modifier.padding(24.dp))
                }
                enabledEngines.forEach { engine ->
                    EngineChoiceRow(
                        item = engine,
                        selected = engine.descriptor.id == state.selectedEngineId,
                        onClick = {
                            onSelectEngine(engine.descriptor.id)
                            showEngines = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    image: PreparedImage?,
    searching: Boolean,
    preparing: Boolean,
    onPickImage: () -> Unit,
    onCrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("检索光窗", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            if (image != null) {
                IconButton(onClick = onCrop, enabled = !searching) {
                    Icon(Icons.Default.Crop, contentDescription = "裁剪图片")
                }
                IconButton(onClick = onPickImage, enabled = !searching) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "更换图片")
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = IndigoGlass),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 580.dp)
                .aspectRatio(1.05f),
        ) {
            Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                when {
                    preparing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ScanCyan)
                            Spacer(Modifier.height(16.dp))
                            Text("正在生成安全上传副本…", color = Color.White)
                        }
                    }
                    image == null -> EmptyPreview(onPickImage)
                    else -> ImagePreview(image, searching)
                }
            }
        }
        if (image != null) {
            Text(
                "${image.width} × ${image.height} · ${formatBytes(image.byteCount)} · 已移除原始 EXIF",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyPreview(onPickImage: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        EmptyPreviewScan()
        Spacer(Modifier.height(14.dp))
        Text("从一张图片开始", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Text("支持系统分享和照片选择器", color = Color(0xFFBDC6E8))
        Spacer(Modifier.height(22.dp))
        Button(onClick = onPickImage) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("选择图片")
        }
    }
}

@Composable
private fun EmptyPreviewScan() {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val transition = rememberInfiniteTransition(label = "empty preview")
    val scanProgress by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty scan position",
    )
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "empty focus pulse",
    )
    val displayedScan = if (animationsEnabled) scanProgress else 0.5f
    val displayedPulse = if (animationsEnabled) pulseProgress else 0.5f

    Box(
        modifier = Modifier
            .size(148.dp)
            .semantics { contentDescription = "等待选择图片的扫描预览" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 8.dp.toPx()
            val corner = 28.dp.toPx()
            val stroke = 2.dp.toPx()
            val left = inset
            val top = inset
            val right = size.width - inset
            val bottom = size.height - inset
            val frameColor = ScanCyan.copy(alpha = 0.68f)

            drawLine(frameColor, Offset(left, top + corner), Offset(left, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, top), Offset(left + corner, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(right - corner, top), Offset(right, top), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(right, top), Offset(right, top + corner), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, bottom - corner), Offset(left, bottom), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(left, bottom), Offset(left + corner, bottom), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(right - corner, bottom), Offset(right, bottom), stroke, StrokeCap.Round)
            drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - corner), stroke, StrokeCap.Round)

            drawCircle(
                color = PrismViolet.copy(alpha = 0.14f + displayedPulse * 0.12f),
                radius = 34.dp.toPx() + displayedPulse * 5.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
            val y = top + (bottom - top) * displayedScan
            drawLine(ScanCyan.copy(alpha = 0.22f), Offset(left, y), Offset(right, y), 12.dp.toPx())
            drawLine(ScanCyan.copy(alpha = 0.86f), Offset(left, y), Offset(right, y), 1.5.dp.toPx())
        }
        Icon(
            Icons.Default.ImageSearch,
            contentDescription = null,
            tint = ScanCyan,
            modifier = Modifier.size(54.dp),
        )
    }
}

@Composable
private fun ImagePreview(image: PreparedImage, searching: Boolean) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, image.file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(image.file.absolutePath)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(Color(0xFF050918))
            .border(1.dp, Color(0xFF3B4773), shape),
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = "待搜索图片",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        ScanOverlay(searching)
    }
}

@Composable
private fun ScanOverlay(searching: Boolean) {
    val animationsEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan position",
    )
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = if (searching) "正在扫描图片" else "图片预览边界" },
    ) {
        val corner = 28.dp.toPx()
        val stroke = 3.dp.toPx()
        val color = ScanCyan.copy(alpha = 0.9f)
        drawLine(color, Offset(0f, corner), Offset(0f, 0f), stroke, StrokeCap.Square)
        drawLine(color, Offset(0f, 0f), Offset(corner, 0f), stroke, StrokeCap.Square)
        drawLine(color, Offset(size.width - corner, 0f), Offset(size.width, 0f), stroke, StrokeCap.Square)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, corner), stroke, StrokeCap.Square)
        drawLine(color, Offset(0f, size.height - corner), Offset(0f, size.height), stroke, StrokeCap.Square)
        drawLine(color, Offset(0f, size.height), Offset(corner, size.height), stroke, StrokeCap.Square)
        drawLine(color, Offset(size.width - corner, size.height), Offset(size.width, size.height), stroke, StrokeCap.Square)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - corner), stroke, StrokeCap.Square)
        if (searching) {
            val y = size.height * if (animationsEnabled) progress else 0.5f
            drawLine(ScanCyan, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            drawLine(ScanCyan.copy(alpha = 0.18f), Offset(0f, y), Offset(size.width, y), 16.dp.toPx())
        }
    }
}

@Composable
private fun SearchControls(
    state: AppUiState,
    selectedEngine: EngineItem?,
    onChooseEngine: () -> Unit,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit,
    onContinueInBackground: () -> Unit,
    onOpenOutcome: (SearchOutcome) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("选择检索路径", style = MaterialTheme.typography.headlineSmall)
        Text(
            "自动引擎直接返回结果；网页辅助会打开第三方官网，并要求你重新选择图片。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !state.isSearching, onClick = onChooseEngine),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EngineBrandMark(selectedEngine?.descriptor)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(selectedEngine?.descriptor?.name ?: "没有已启用引擎", fontWeight = FontWeight.Bold)
                    Text(
                        selectedEngine?.descriptor?.summary ?: "请前往引擎管理启用服务",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "选择引擎")
            }
        }
        state.guidance?.let { EngineGuidanceCard(it) }

        if (state.isSearching) {
            Button(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.isBackgroundSearching) "取消后台搜索" else "取消搜索")
            }
            if (!state.isBackgroundSearching) {
                OutlinedButton(onClick = onContinueInBackground, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Schedule, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("转到后台继续")
                }
            }
        } else {
            val guidance = state.guidance
            val canRunPrimaryAction = when (guidance?.action) {
                EngineGuidanceAction.CONFIGURE_CREDENTIALS -> selectedEngine != null
                null -> false
                else -> state.image != null && selectedEngine != null && !state.isPreparingImage
            }
            Button(
                onClick = onPrimaryAction,
                enabled = canRunPrimaryAction,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Icon(
                    when (guidance?.action) {
                        EngineGuidanceAction.CONFIGURE_CREDENTIALS -> Icons.Default.Key
                        EngineGuidanceAction.OPEN_EXTERNAL,
                        EngineGuidanceAction.OPEN_WEB,
                        -> Icons.AutoMirrored.Filled.OpenInNew
                        else -> Icons.Default.ImageSearch
                    },
                    contentDescription = null,
                )
                Spacer(Modifier.width(10.dp))
                Text(guidance?.primaryActionLabel ?: "选择搜索引擎")
            }
        }

        state.outcome?.let { OutcomeCard(it, onOpenOutcome) }
    }
}

@Composable
private fun EngineGuidanceCard(guidance: EngineGuidance) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                guidance.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                guidance.detail,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun OutcomeCard(outcome: SearchOutcome, onOpen: (SearchOutcome) -> Unit) {
    val (title, detail, action) = when (outcome) {
        is SearchOutcome.NativeResults -> Triple(
            "找到 ${outcome.items.size} 条结果",
            "结果已在应用内整理，不会加载第三方网页脚本。",
            "查看结果",
        )
        is SearchOutcome.WebResult -> Triple("上传完成", "结果将在安全浏览器标签页中打开。", "打开结果")
        is SearchOutcome.ExternalApp -> Triple("交给 ${outcome.engine.name}", "图片将通过系统 URI 授权发送给目标应用。", "继续")
        is SearchOutcome.AssistedWeb -> Triple("需要网页辅助", outcome.instructions, "打开官网")
        is SearchOutcome.AuthRequired -> Triple("需要 API 凭据", "填写后凭据会由 Android Keystore 加密保存。", "配置凭据")
        is SearchOutcome.Error -> Triple("搜索没有完成", outcome.failure.message, if (outcome.failure.retryable) "重试" else "知道了")
    }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                FilledTonalButton(onClick = { onOpen(outcome) }) {
                    Text(action)
                }
            }
        }
    }
}

@Composable
private fun EngineChoiceRow(item: EngineItem, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EngineBrandMark(item.descriptor, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.descriptor.name, fontWeight = FontWeight.Bold)
            Text(item.descriptor.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Box(Modifier.size(12.dp).background(PrismViolet, RoundedCornerShape(50)))
        }
    }
}

@Composable
internal fun BadgeRow(badges: Set<EngineBadge>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.forEach { badge ->
            Text(
                badge.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
