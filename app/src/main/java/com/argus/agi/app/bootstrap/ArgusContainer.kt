package com.argus.agi.app.bootstrap

import android.app.Application
import com.argus.agi.application.runtime.ArgusRuntime
import com.argus.agi.application.runtime.CaptureRuntime
import com.argus.agi.application.runtime.DeviceRuntime
import com.argus.agi.application.runtime.DeviceSafetySnapshot
import com.argus.agi.application.runtime.HardwareRuntime
import com.argus.agi.application.runtime.KnowledgeRuntime
import com.argus.agi.application.runtime.TrainingRuntime
import com.argus.agi.application.runtime.TrainingStatusSnapshot
import com.argus.agi.data.acquisition.WebSearchEngine
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.TrainingManager
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.capture.AccessibilityActionRegistry
import com.argus.agi.platform.android.capture.ExcludedAppsManager
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.HardwareProfile

/**
 * Process-scoped composition root for the current single-:app topology.
 *
 * Concrete adapters are constructed here and exposed to Android entry points
 * through narrow application-facing runtime contracts.
 */
class ArgusContainer(application: Application) : ArgusRuntime {

    val neuralEngine: ArgusNeuralEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ArgusNeuralEngine(application)
    }

    val worldModel: WorldModel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WorldModel(application)
    }

    val deviceGuard: DeviceGuard by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DeviceGuard(application)
    }

    val hardwareProfile: HardwareProfile by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HardwareProfile(application)
    }

    val trainingManager: TrainingManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        TrainingManager(application, neuralEngine, deviceGuard, worldModel)
    }

    val excludedAppsManager: ExcludedAppsManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ExcludedAppsManager(application)
    }

    val webSearchEngine: WebSearchEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WebSearchEngine()
    }

    override val accessibility = AccessibilityActionRegistry()

    override val training: TrainingRuntime = object : TrainingRuntime {
        override fun snapshot(): TrainingStatusSnapshot {
            val current = trainingManager.currentTrainingItem.value
            return TrainingStatusSnapshot(
                isPaused = trainingManager.isTrainingPaused.value,
                currentSource = current?.source,
                latestLoss = trainingManager.lossHistory.value.lastOrNull()
            )
        }

        override fun togglePause() {
            trainingManager.togglePauseTraining()
        }

        override fun enqueueData(source: String, title: String, content: String) {
            trainingManager.enqueueData(source, title, content)
        }
    }

    override val device: DeviceRuntime = object : DeviceRuntime {
        override fun snapshot(): DeviceSafetySnapshot {
            val state = deviceGuard.safetyState.value
            return DeviceSafetySnapshot(
                batteryPercent = state.batteryPct,
                temperatureCelsius = state.temperatureCelsius,
                isSafeToTrain = state.isSafeToTrain,
                statusMessage = state.statusMessage
            )
        }
    }

    override val hardware: HardwareRuntime = object : HardwareRuntime {
        override fun primaryAcceleratorLabel(): String {
            return hardwareProfile.getHardwareInfo().primaryAccelerator
        }
    }

    override val knowledge: KnowledgeRuntime = object : KnowledgeRuntime {
        override fun updateScreenContext(packageName: String, visibleText: String) {
            worldModel.updateScreenContext(packageName, visibleText)
        }
    }

    override val capture: CaptureRuntime = object : CaptureRuntime {
        override fun isPackageExcluded(packageName: String): Boolean {
            return excludedAppsManager.isAppExcluded(packageName)
        }
    }
}
