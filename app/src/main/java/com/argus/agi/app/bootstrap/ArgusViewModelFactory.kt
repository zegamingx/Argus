package com.argus.agi.app.bootstrap

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.argus.agi.ui.ArgusViewModel

class ArgusViewModelFactory(
    private val application: Application,
    private val container: ArgusContainer
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ArgusViewModel::class.java)) {
            throw IllegalArgumentException("Unsupported ViewModel: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return ArgusViewModel(
            application = application,
            neuralEngine = container.neuralEngine,
            worldModel = container.worldModel,
            trainingManager = container.trainingManager,
            deviceGuard = container.deviceGuard,
            hardwareProfile = container.hardwareProfile,
            excludedAppsManager = container.excludedAppsManager,
            webSearchEngine = container.webSearchEngine,
            accessibilityRuntime = container.accessibility
        ) as T
    }
}
