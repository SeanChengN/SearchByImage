package io.github.seancheng.searchbyimage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.seancheng.searchbyimage.AppUiState
import io.github.seancheng.searchbyimage.BuildConfig
import io.github.seancheng.searchbyimage.domain.EngineCatalog
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val values = remember { mutableStateMapOf<String, String>() }
    val engines = EngineCatalog.builtIns.filter { it.credentialFields.isNotEmpty() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API 凭据") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("凭据使用 Android Keystore 加密，只保存在此设备中；不会备份、写入日志或提交到仓库。")
                    }
                }
            }
            items(engines, key = { it.id }) { engine ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(engine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(engine.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (engine.credentialFields.all { it.id in state.configuredCredentials }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "已配置", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        engine.credentialFields.forEach { field ->
                            val configured = field.id in state.configuredCredentials
                            OutlinedTextField(
                                value = values[field.id].orEmpty(),
                                onValueChange = { values[field.id] = it },
                                label = { Text(field.label) },
                                placeholder = { Text(if (configured) "已配置；输入新值可替换" else "尚未配置") },
                                visualTransformation = if (field.isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (field.isSecret) KeyboardType.Password else KeyboardType.Text,
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        onSave(field.id, values[field.id].orEmpty())
                                        values[field.id] = ""
                                    },
                                    enabled = values[field.id].orEmpty().isNotBlank(),
                                ) { Text(if (configured) "替换" else "保存") }
                                if (configured) {
                                    OutlinedButton(onClick = { onSave(field.id, "") }) { Text("移除") }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "SauceNAO、Lenso.ai 与 TinEye 的密钥均由你向对应服务申请；应用不会预置或代管密钥。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onJpegQuality: (Int) -> Unit,
    onAbout: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SettingCard(
                    title = "动态配色",
                    detail = "Android 12 及以上跟随系统壁纸颜色；关闭后使用检索光窗配色。",
                ) {
                    Switch(checked = state.settings.dynamicColor, onCheckedChange = onDynamicColor)
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("JPEG 上传质量", fontWeight = FontWeight.Bold)
                        Text(
                            "${state.settings.jpegQuality}% · 图片最长边会限制在 4096 像素",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = state.settings.jpegQuality.toFloat(),
                            onValueChange = { onJpegQuality(it.roundToInt()) },
                            valueRange = 70f..96f,
                            steps = 25,
                        )
                    }
                }
            }
            item {
                SettingCard(
                    title = "上传副本隐私",
                    detail = "始终重新编码图片并移除原始 EXIF、定位和设备信息。",
                ) {
                    Switch(checked = true, onCheckedChange = null)
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("只有点击“转到后台继续”时才会请求通知权限。普通前台搜索不使用通知。")
                    }
                }
            }
            item {
                FilledTonalButton(onClick = onAbout, modifier = Modifier.fillMaxWidth()) {
                    Text("关于与第三方说明")
                }
            }
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    detail: String,
    trailing: @Composable () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onOpenUrl: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Search By Image", style = MaterialTheme.typography.headlineSmall)
                Text("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · io.github.seancheng.searchbyimage")
                Spacer(Modifier.height(8.dp))
                Text("面向 Android 10–17 的独立重写版本。不会读取旧包名应用的数据。")
            }
            item {
                LinkCard("上游项目", "RikkaW/SearchByImage", "https://github.com/RikkaW/SearchByImage", onOpenUrl)
            }
            item {
                LinkCard(
                    "引擎候选目录",
                    "dessant/search-by-image wiki；本应用没有复制其 GPL JavaScript。",
                    "https://github.com/dessant/search-by-image/wiki/Search-engines",
                    onOpenUrl,
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("数据说明", fontWeight = FontWeight.Bold)
                        Text("图片只发送给你本次明确选择的搜索服务。API Key 仅保存在 Android Keystore；应用不包含分析、广告、Firebase 或自有后端。")
                    }
                }
            }
            item {
                Text(
                    "主要依赖：AndroidX、Jetpack Compose、Navigation 3、Room、DataStore、WorkManager、OkHttp、CanHub Android Image Cropper。完整版本与许可证见仓库 THIRD_PARTY_NOTICES.md。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LinkCard(title: String, detail: String, url: String, onOpenUrl: (String) -> Unit) {
    Card(onClick = { onOpenUrl(url) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
