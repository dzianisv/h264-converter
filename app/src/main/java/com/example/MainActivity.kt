package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.MainScreen
import com.example.ui.theme.Convert264Theme
import com.example.viewmodel.ConverterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ConverterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            Convert264Theme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type?.startsWith("video/") == true) {
            // Video shared via Android Share Sheet (ACTION_SEND)
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            val finalUri = streamUri ?: intent.clipData?.getItemAt(0)?.uri ?: intent.data
            finalUri?.let { viewModel.loadVideo(it) }
        } else if (Intent.ACTION_VIEW == action && (type?.startsWith("video/") == true || intent.data != null)) {
            // Video opened via File Manager or external app (ACTION_VIEW)
            intent.data?.let { viewModel.loadVideo(it) }
        }
    }
}
