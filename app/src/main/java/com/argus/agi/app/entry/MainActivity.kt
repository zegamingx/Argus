package com.argus.agi.app.entry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.argus.agi.app.bootstrap.ArgusStartupViewModel
import com.argus.agi.app.bootstrap.ArgusViewModelFactory
import com.argus.agi.app.navigation.ArgusBootScreen
import com.argus.agi.app.navigation.MainScreen
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.design.ArgusTheme

class MainActivity : ComponentActivity() {

    private val startupViewModel: ArgusStartupViewModel by viewModels()

    private val viewModel: ArgusViewModel by viewModels {
        val argusApplication = application as ArgusApp
        ArgusViewModelFactory(argusApplication, argusApplication.container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArgusTheme {
                val startupState by startupViewModel.uiState.collectAsState()

                if (startupState.canOpenWorkspace) {
                    MainScreen(viewModel = viewModel)
                } else {
                    ArgusBootScreen(
                        state = startupState,
                        onRetry = startupViewModel::retry
                    )
                }
            }
        }
    }
}
