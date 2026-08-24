package io.github.seancheng.searchbyimage.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.seancheng.searchbyimage.AppUiState
import io.github.seancheng.searchbyimage.EngineItem
import io.github.seancheng.searchbyimage.data.db.CustomEngine
import io.github.seancheng.searchbyimage.domain.EngineCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnginesScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDefault: (String) -> Unit,
    onCredentials: () -> Unit,
    onAddCustom: () -> Unit,
    onEditCustom: (CustomEngine) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var expandedCategoryNames by rememberSaveable { mutableStateOf("") }
    val expandedCategories = expandedCategoryNames
        .split(',')
        .filter(String::isNotBlank)
        .toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索引擎") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCredentials) { Icon(Icons.Default.Key, contentDescription = "API 凭据") }
                    IconButton(onClick = onAddCustom) { Icon(Icons.Default.Add, contentDescription = "添加自定义引擎") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("engine-list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("每次只会向你选中的一个服务发送图片。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onCredentials) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("API 凭据")
                    }
                    OutlinedButton(onClick = onAddCustom) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("自定义引擎")
                    }
                }
            }
            EngineCategory.entries.forEach { category ->
                val engines = state.engines.filter { it.descriptor.category == category }
                if (engines.isEmpty()) return@forEach
                val expanded = category.name in expandedCategories
                item {
                    EngineCategoryHeader(
                        category = category,
                        enabledCount = engines.count(EngineItem::enabled),
                        totalCount = engines.size,
                        expanded = expanded,
                        onToggle = {
                            expandedCategoryNames = if (expanded) {
                                (expandedCategories - category.name).sorted().joinToString(",")
                            } else {
                                (expandedCategories + category.name).sorted().joinToString(",")
                            }
                        },
                    )
                }
                if (expanded) {
                    items(engines, key = { it.descriptor.id }) { item ->
                        EngineManageCard(
                            item = item,
                            isDefault = state.settings.defaultEngineId == item.descriptor.id,
                            onToggle = { onToggle(item.descriptor.id, it) },
                            onDefault = { onDefault(item.descriptor.id) },
                            onEdit = item.customEngine?.let { engine -> { onEditCustom(engine) } },
                            onOpenUrl = onOpenUrl,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineCategoryHeader(
    category: EngineCategory,
    enabledCount: Int,
    totalCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(category.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$enabledCount / $totalCount 已启用",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "折叠${category.label}" else "展开${category.label}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EngineManageCard(
    item: EngineItem,
    isDefault: Boolean,
    onToggle: (Boolean) -> Unit,
    onDefault: () -> Unit,
    onEdit: (() -> Unit)?,
    onOpenUrl: (String) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDefault) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EngineBrandMark(item.descriptor, size = 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.descriptor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(item.descriptor.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (onEdit != null) {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "编辑") }
                } else {
                    Switch(checked = item.enabled, onCheckedChange = onToggle)
                }
            }
            BadgeRow(item.descriptor.badges)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isDefault) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isDefault) "默认引擎" else "设为默认", modifier = Modifier.clickable(enabled = item.enabled, onClick = onDefault))
                Spacer(Modifier.weight(1f))
                Text(item.descriptor.mode.label, style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenUrl(item.descriptor.officialUrl) }) { Text("官网") }
                item.descriptor.termsUrl?.let { termsUrl ->
                    TextButton(onClick = { onOpenUrl(termsUrl) }) { Text("服务条款") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomEngineScreen(
    existing: CustomEngine?,
    onBack: () -> Unit,
    onSave: (CustomEngine) -> Unit,
    onDelete: (CustomEngine) -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var endpoint by rememberSaveable(existing?.id) { mutableStateOf(existing?.endpoint.orEmpty()) }
    var fileField by rememberSaveable(existing?.id) { mutableStateOf(existing?.fileField ?: "file") }
    var resultField by rememberSaveable(existing?.id) { mutableStateOf(existing?.resultUrlJsonField ?: "url") }
    var staticFields by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.staticFields?.entries?.joinToString("\n") { "${it.key}=${it.value}" }.orEmpty())
    }
    var enabled by rememberSaveable(existing?.id) { mutableStateOf(existing?.enabled ?: true) }
    var showDelete by remember { mutableStateOf(false) }

    val parsedStaticFields = staticFields.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) null else line.substring(0, separator).trim() to line.substring(separator + 1).trim()
        }
        .take(20)
        .toMap()
    val canSave = name.trim().length in 2..40 && endpoint.startsWith("https://") && fileField.isNotBlank() && resultField.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "添加自定义引擎" else "编辑自定义引擎") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("只允许 HTTPS multipart POST；不支持脚本、自定义请求头或私网地址。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(40) },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("HTTPS 端点") },
                placeholder = { Text("https://example.com/search") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fileField,
                    onValueChange = { fileField = it.take(64) },
                    label = { Text("文件字段") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = resultField,
                    onValueChange = { resultField = it.take(64) },
                    label = { Text("结果 JSON 路径") },
                    placeholder = { Text("data.url") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = staticFields,
                onValueChange = { staticFields = it },
                label = { Text("静态表单字段（每行 key=value）") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("启用此引擎", fontWeight = FontWeight.Medium)
                    Text("保存后会出现在引擎选择器中", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    onSave(
                        CustomEngine(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            endpoint = endpoint.trim(),
                            fileField = fileField.trim(),
                            staticFields = parsedStaticFields,
                            resultUrlJsonField = resultField.trim(),
                            enabled = enabled,
                            sortOrder = existing?.sortOrder ?: 0,
                        ),
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存引擎")
            }
            existing?.let {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("移除自定义引擎", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("移除 ${existing.name}？") },
            text = { Text("此操作只删除新版应用中的这条自定义配置。") },
            confirmButton = {
                TextButton(onClick = { onDelete(existing) }) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("取消") }
            },
        )
    }
}
