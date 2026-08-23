package io.github.seancheng.searchbyimage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    outcome: SearchOutcome?,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val results = (outcome as? SearchOutcome.NativeResults)?.items.orEmpty()
    val engineName = (outcome as? SearchOutcome.NativeResults)?.engine?.name ?: "搜索"
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$engineName 结果") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (results.isEmpty()) {
                item { Text("没有可显示的原生结果。") }
            }
            itemsIndexed(results, key = { index, item -> "${item.sourceUrl}:$index" }) { index, item ->
                Card(onClick = { onOpenUrl(item.sourceUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            (index + 1).toString().padStart(2, '0'),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            item.subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(
                                runCatching { URI(item.sourceUrl).host }.getOrNull() ?: item.sourceUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            item.similarity?.let { Text("相似度 %.1f%%".format(it), color = MaterialTheme.colorScheme.primary) }
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "在浏览器中打开")
                    }
                }
            }
        }
    }
}
