package com.argus.agi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.argus.agi.ui.ArgusViewModel
import com.argus.agi.ui.screens.ChatScreen
import com.argus.agi.ui.screens.NeuralCoreScreen
import com.argus.agi.ui.screens.PerceptionScreen
import com.argus.agi.ui.screens.SkillsAndSettingsScreen
import com.argus.agi.ui.theme.ArgusCyanPrimary
import com.argus.agi.ui.theme.ArgusDarkBg
import com.argus.agi.ui.theme.ArgusSurface
import com.argus.agi.ui.theme.ArgusTextMuted
import com.argus.agi.ui.theme.ArgusTheme

enum class ArgusScreen(val label: String, val icon: ImageVector) {
    CHAT("Terminal", Icons.AutoMirrored.Filled.Chat),
    NEURAL_CORE("Neural Core", Icons.Default.Memory),
    PERCEPTION("Persepsi", Icons.Default.Visibility),
    SETTINGS("Skill & Privasi", Icons.Default.Tune)
}

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

@Composable
fun MainScreen(viewModel: ArgusViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf(ArgusScreen.CHAT) }
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isKeyboardOpen = imeBottom > 0.dp

    BackHandler(enabled = currentScreen != ArgusScreen.CHAT) {
        currentScreen = ArgusScreen.CHAT
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ArgusDarkBg),
        bottomBar = {
            // Smoothly hide NavigationBar when keyboard is active to maximize text input room
            AnimatedVisibility(
                visible = !isKeyboardOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    containerColor = ArgusSurface,
                    tonalElevation = 6.dp
                ) {
                    ArgusScreen.entries.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.label
                                )
                            },
                            label = {
                                Text(
                                    text = screen.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ArgusDarkBg,
                                selectedTextColor = ArgusCyanPrimary,
                                indicatorColor = ArgusCyanPrimary,
                                unselectedIconColor = ArgusTextMuted,
                                unselectedTextColor = ArgusTextMuted
                            ),
                            modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ArgusScreen.CHAT -> ChatScreen(viewModel = viewModel)
                ArgusScreen.NEURAL_CORE -> NeuralCoreScreen(viewModel = viewModel)
                ArgusScreen.PERCEPTION -> PerceptionScreen(viewModel = viewModel)
                ArgusScreen.SETTINGS -> SkillsAndSettingsScreen(viewModel = viewModel)
            }
        }
    }
}
