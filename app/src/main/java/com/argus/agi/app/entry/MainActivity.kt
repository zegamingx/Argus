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
import androidx.core.content.ContextCompat
import com.argus.agi.app.navigation.MainScreen
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.design.ArgusTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ArgusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ArgusTheme {
                // Runtime Permissions Launcher
                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions evaluated */ }

                LaunchedEffect(Unit) {
                    val neededPermissions = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        neededPermissions.add(Manifest.permission.CAMERA)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (neededPermissions.isNotEmpty()) {
                        permissionsLauncher.launch(neededPermissions.toTypedArray())
                    }
                }

                MainScreen(viewModel = viewModel)
            }
        }
    }
}
