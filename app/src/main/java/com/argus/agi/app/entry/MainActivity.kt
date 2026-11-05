package com.argus.agi.app.entry

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.argus.agi.app.bootstrap.ArgusStartupStage
import com.argus.agi.app.bootstrap.ArgusStartupViewModel
import com.argus.agi.app.bootstrap.ArgusViewModelFactory
import com.argus.agi.app.navigation.ArgusBootScreen
import com.argus.agi.app.navigation.MainScreen
import com.argus.agi.platform.android.lifecycle.ArgusForegroundService
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
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permission state is observed by the owning feature */ }

                LaunchedEffect(startupState.stage) {
                    if (startupState.stage == ArgusStartupStage.RUNTIME_PREPARED) {
                        ArgusForegroundService.startService(this@MainActivity)

                        val neededPermissions = mutableListOf<String>()
                        if (
                            ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            neededPermissions.add(Manifest.permission.CAMERA)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (
                                ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        if (neededPermissions.isNotEmpty()) {
                            permissionsLauncher.launch(neededPermissions.toTypedArray())
                        }
                    }
                }

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
