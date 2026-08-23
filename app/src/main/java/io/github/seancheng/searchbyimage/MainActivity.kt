package io.github.seancheng.searchbyimage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import io.github.seancheng.searchbyimage.ui.SearchByImageApp

class MainActivity : ComponentActivity() {
    private val applicationContainer get() = (application as SearchByImageApplication).container
    private val viewModel: AppViewModel by viewModels {
        AppViewModel.Factory(application, applicationContainer)
    }
    private var pickerRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        processIntent(intent)
        setContent {
            SearchByImageApp(
                viewModel = viewModel,
                pickerRequest = pickerRequest,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null) viewModel.prepareImage(uri)
            }
            "$packageName.action.NEW_SEARCH" -> pickerRequest++
        }
    }
}
