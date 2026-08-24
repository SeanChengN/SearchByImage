package io.github.seancheng.searchbyimage.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.seancheng.searchbyimage.R
import io.github.seancheng.searchbyimage.domain.EngineDescriptor
import io.github.seancheng.searchbyimage.ui.theme.SearchBlue

@Composable
internal fun EngineBrandMark(
    engine: EngineDescriptor?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val logo = engine?.id?.let(::engineLogoResource)
    val rasterLogo = engine?.id?.let(::engineRasterLogo)
    val bitmap = remember(rasterLogo) {
        rasterLogo?.let { encoded ->
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (logo == null && bitmap == null) SearchBlue else Color.White)
            .border(
                width = if (logo == null && bitmap == null) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "${engine?.name} Logo",
                modifier = Modifier.size(size * 0.65f),
            )
        } else if (logo == null) {
            Icon(
                Icons.Default.ImageSearch,
                contentDescription = "${engine?.name ?: "搜索引擎"} 通用图标",
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f),
            )
        } else {
            Icon(
                painter = painterResource(logo),
                contentDescription = "${engine.name} Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(size * 0.61f),
            )
        }
    }
}

@DrawableRes
private fun engineLogoResource(engineId: String): Int? = when (engineId) {
    "google_lens" -> R.drawable.ic_engine_google_lens
    "baidu" -> R.drawable.ic_engine_baidu
    "sogou" -> R.drawable.ic_engine_sogou
    "trace_moe" -> R.drawable.ic_engine_trace_moe
    "yandex" -> R.drawable.ic_engine_yandex
    else -> null
}

private fun engineRasterLogo(engineId: String): String? = when (engineId) {
    "tineye" -> TINEYE_LOGO
    "saucenao" -> SAUCENAO_LOGO
    "saucenao_web" -> SAUCENAO_LOGO
    "lenso" -> LENSO_LOGO
    "ascii2d" -> ASCII2D_LOGO
    else -> null
}

// Official site favicons, converted once to 32 px PNGs and bundled with the APK.
private const val TINEYE_LOGO = "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"
private const val SAUCENAO_LOGO = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAEySURBVFhH7ZILbsQgDAVztBwtN8vRUlw0let9Dh+JRJVqaVaExWYMbCWuFzkK27Xv+3Ucx2PYfr8EbPLJsP2kwHmeP5YrsPoWNpYC7o8liH20QHwT7s66yPJt7PcpaAG+CZfQRZbPvKtXBTCOpgRvo3US1OGuCTaM+xSqQCQKEBTKmMjTAlkn2UmMrndoAbBkFTY/s05wL9DqDCY6h3sBsGIjYetVHUGfQHYSMQY6hz4BaJ3EQOfwL/BH3kCr8xgDJ3EvkHVOp5D933ES9wJWRIXNz6wTaIFW57Gz0fUOLWBJKmxerYeJvCpAB5jGQr13mp0EAnGfQhVgQxbyTTDfS5bPvKunBTCFVueRLN/Gfp+CFlhFU4C7XgVvw8ZS4Kn4EIh3thr3pqrAi3wL2M9LbPsXZ9j6EFG4jj0AAAAASUVORK5CYII="
private const val LENSO_LOGO = "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"
private const val ASCII2D_LOGO = "iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAIDSURBVFhH7ZbLyzlhHMWV/CHyd4iNja2VtZ2lopBrycKGBQkh2QihKK8UkYVryf2WlJTbBtmen3l4Re9Mdj+bOXWa5pnmez49Tc8ZDr4rMwvAArAAtADH4xHz+RydTgflcvnpYrGIn58fJBIJhMNhRmcyGVQqFaxWq8dERtEDUOH5fB6BQABms/lprVYLlUoFuVwOsVjMaIVCAZvNRiA+iB5gOp0im83C5/O9ARgMBgKh0+mg1+vJ/evzX3u9XqRSKYxGo8dERtEDjMdjpNNpMoguwO/3I5fLoVAooFQq/XGr1cJkMsHhcHhMZBQ9wH6/JxDUoNdv4Nftdpvs0mKxwHK5/OPNZkPCr9frYyKj6AH+o1gAFoAFYAHuAEwnH9UHsVgM0WiUtvWYHAqFEAwG4XQ6YbVaYbFYyBHucrkQiUTQ7XZJ+k13AKazX6lUQiaTQSqV0rYek0UiEYRCIfh8Png8HrhcLjgcDgQCASQSCYF46A4wHA6RTCbh8XjeAKjWU6vV5Pq6/skmkwlGo5G8p9FonqbW7XY7qtUqSb/pDtDv98lWu93ut0EOh4NAxeNx2tb75F6vR4ppu92ScprNZqjX61iv1yT9pjvAbrcjEI1G4+0bqNVqaDabGAwGtK33yVTw+XzG5XIhzUj9aVHhp9OJpN90B/iiWAAWgACYv2eI/wHf/8lYc4cxWgAAAABJRU5ErkJggg=="
