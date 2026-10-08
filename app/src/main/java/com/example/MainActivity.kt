package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.BrowserScreen
import com.example.ui.theme.DevChromeTheme
import com.example.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
    private val browserViewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by browserViewModel.settings.collectAsState()

            DevChromeTheme(darkTheme = settings.appDarkTheme) {
                BrowserScreen(viewModel = browserViewModel)
            }
        }
    }
}
