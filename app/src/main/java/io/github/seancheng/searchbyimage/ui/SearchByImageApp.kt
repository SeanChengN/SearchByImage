package io.github.seancheng.searchbyimage.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.seancheng.searchbyimage.AppViewModel
import io.github.seancheng.searchbyimage.data.db.CustomEngine
import io.github.seancheng.searchbyimage.data.network.safeResultUrl
import io.github.seancheng.searchbyimage.domain.SearchOutcome
import io.github.seancheng.searchbyimage.ui.theme.SearchByImageTheme

private data object HomeRoute
private data object EnginesRoute
private data object CredentialsRoute
private data object SettingsRoute
private data object ResultsRoute
private data object AboutRoute
private data class CustomEngineRoute(val id: Long? = null)
private data class CropRoute(val uri: Uri, val mimeType: String)

@Composable
fun SearchByImageApp(
    viewModel: AppViewModel,
    pickerRequest: Int,
) {
    val state by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.prepareImage(uri)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.continueInBackground()
        } else {
            viewModel.showMessage("未授予通知权限，搜索仍保留在前台")
        }
    }

    fun launchPicker() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun launchCrop() {
        val image = state.image ?: return
        backStack.add(CropRoute(image.contentUri, image.mimeType))
    }

    fun continueInBackground() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.continueInBackground()
        }
    }

    LaunchedEffect(pickerRequest) {
        if (pickerRequest > 0) launchPicker()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    SearchByImageTheme(
        darkTheme = isSystemInDarkTheme(),
        dynamicColor = state.settings.dynamicColor,
    ) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<HomeRoute> {
                        HomeScreen(
                            state = state,
                            onPickImage = ::launchPicker,
                            onCrop = ::launchCrop,
                            onSelectEngine = viewModel::selectEngine,
                            onSearch = viewModel::search,
                            onCancel = viewModel::cancelSearch,
                            onContinueInBackground = ::continueInBackground,
                            onOpenOutcome = { outcome ->
                                when (outcome) {
                                    is SearchOutcome.NativeResults -> backStack.add(ResultsRoute)
                                    is SearchOutcome.AuthRequired -> backStack.add(CredentialsRoute)
                                    else -> openOutcome(context, outcome, state.image?.contentUri, state.image?.mimeType)
                                }
                            },
                            onOpenEngines = dropUnlessResumed { backStack.add(EnginesRoute) },
                            onOpenSettings = dropUnlessResumed { backStack.add(SettingsRoute) },
                        )
                    }
                    entry<CropRoute> { route ->
                        CropScreen(
                            sourceUri = route.uri,
                            sourceMimeType = route.mimeType,
                            jpegQuality = state.settings.jpegQuality,
                            onBack = { backStack.removeLastOrNull() },
                            onCropped = { uri ->
                                backStack.removeLastOrNull()
                                viewModel.prepareImage(uri)
                            },
                        )
                    }
                    entry<EnginesRoute> {
                        EnginesScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onToggle = viewModel::setEngineEnabled,
                            onDefault = viewModel::setDefaultEngine,
                            onCredentials = { backStack.add(CredentialsRoute) },
                            onAddCustom = { backStack.add(CustomEngineRoute()) },
                            onEditCustom = { backStack.add(CustomEngineRoute(it.id)) },
                            onOpenUrl = { openSecureUrl(context, it) },
                        )
                    }
                    entry<CredentialsRoute> {
                        CredentialsScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onSave = viewModel::saveCredential,
                        )
                    }
                    entry<SettingsRoute> {
                        SettingsScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onDynamicColor = viewModel::setDynamicColor,
                            onJpegQuality = viewModel::setJpegQuality,
                            onAbout = { backStack.add(AboutRoute) },
                        )
                    }
                    entry<ResultsRoute> {
                        ResultsScreen(
                            outcome = state.outcome,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenUrl = { openSecureUrl(context, it) },
                        )
                    }
                    entry<AboutRoute> {
                        AboutScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenUrl = { openSecureUrl(context, it) },
                        )
                    }
                    entry<CustomEngineRoute> { route ->
                        val existing = state.engines.firstOrNull { it.customEngine?.id == route.id }?.customEngine
                        CustomEngineScreen(
                            existing = existing,
                            onBack = { backStack.removeLastOrNull() },
                            onSave = { engine ->
                                viewModel.saveCustomEngine(engine)
                                backStack.removeLastOrNull()
                            },
                            onDelete = { engine ->
                                viewModel.deleteCustomEngine(engine)
                                backStack.removeLastOrNull()
                            },
                        )
                    }
                },
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun openOutcome(
    context: Context,
    outcome: SearchOutcome,
    imageUri: Uri?,
    mimeType: String?,
) {
    when (outcome) {
        is SearchOutcome.WebResult -> openSecureUrl(context, outcome.url)
        is SearchOutcome.AssistedWeb -> openSecureUrl(context, outcome.url)
        is SearchOutcome.ExternalApp -> {
            if (imageUri == null) return
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                clipData = ClipData.newUri(context.contentResolver, "search image", imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage(outcome.packageName)
            }
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                openSecureUrl(context, outcome.fallbackUrl)
            }
        }
        else -> Unit
    }
}

internal fun openSecureUrl(context: Context, rawUrl: String) {
    val url = safeResultUrl(rawUrl) ?: return
    runCatching {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, url.toUri())
    }.onFailure {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }
}
