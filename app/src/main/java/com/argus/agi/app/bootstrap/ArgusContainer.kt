package com.argus.agi.app.bootstrap

import android.app.Application
import com.argus.agi.application.learning.LocalCorpusImportService
import com.argus.agi.application.runtime.ArgusRuntime
import com.argus.agi.application.runtime.CaptureRuntime
import com.argus.agi.application.runtime.DeviceRuntime
import com.argus.agi.application.runtime.DeviceSafetySnapshot
import com.argus.agi.application.runtime.HardwareRuntime
import com.argus.agi.application.runtime.TrainingRuntime
import com.argus.agi.application.runtime.TrainingStatusSnapshot
import com.argus.agi.data.acquisition.WebSearchEngine
import com.argus.agi.data.corpus.LocalCorpusRepository
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.capture.AccessibilityActionRegistry
import com.argus.agi.platform.android.capture.ExcludedAppsManager
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.HardwareProfile
import com.argus.agi.platform.android.device.SystemHardwareInfo
import com.argus.agi.platform.android.storage.LocalDatasetImporter

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

    @Volatile
    private var cachedHardwareInfo: SystemHardwareInfo? = null

    @Synchronized
    fun getHardwareProfileSnapshot(): SystemHardwareInfo {
        val cached = cachedHardwareInfo
        if (cached != null) return cached

        val detected = hardwareProfile.getHardwareInfo()
        cachedHardwareInfo = detected
        return detected
    }

    val excludedAppsManager: ExcludedAppsManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ExcludedAppsManager(application)
    }

    val webSearchEngine: WebSearchEngine by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WebSearchEngine()
    }

    val localCorpusRepository: LocalCorpusRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LocalCorpusRepository(application.filesDir)
    }

    val localDatasetImporter: LocalDatasetImporter by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        LocalDatasetImporter(application, localCorpusRepository)
    }

    val localCorpusImportService: LocalCorpusImportService by lazy(
        LazyThreadSafetyMode.SYNCHRONIZED
    ) {
        LocalCorpusImportService(
            stager = localDatasetImporter,
            store = localCorpusRepository
        )
    }

    override val accessibility = AccessibilityActionRegistry()

    override val training: TrainingRuntime = object : TrainingRuntime {
        override fun snapshot(): TrainingStatusSnapshot {
            return TrainingStatusSnapshot(
                isWorkflowAvailable = false,
                statusMessage = "Learning is disabled until approved candidate manifests and session admission are implemented."
            )
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
            return getHardwareProfileSnapshot().primaryAccelerator
        }
    }

    override val capture: CaptureRuntime = object : CaptureRuntime {
        override fun isPackageExcluded(packageName: String): Boolean {
            return excludedAppsManager.isAppExcluded(packageName)
        }

        override fun updateTransientScreenContext(packageName: String, visibleText: String) {
            worldModel.updateScreenContext(packageName, visibleText)
        }
    }
}
