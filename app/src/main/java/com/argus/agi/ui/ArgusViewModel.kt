package com.argus.agi.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.argus.agi.feature.chat.presentation.ChatMessage
import com.argus.agi.feature.learning.presentation.LearningBenchmarkUiState
import com.argus.agi.feature.learning.presentation.LearningHardwareUiState
import com.argus.agi.feature.learning.presentation.LearningSafetyUiState
import com.argus.agi.feature.learning.presentation.LearningUiState
import com.argus.agi.feature.learning.presentation.TrainingQueueItemUiState
import com.argus.agi.feature.perception.presentation.PerceptionHardwareUiState
import com.argus.agi.feature.perception.presentation.PerceptionUiState
import com.argus.agi.feature.settings.presentation.InstalledAppUiItem
import com.argus.agi.feature.settings.presentation.SearchResultUiItem
import com.argus.agi.feature.settings.presentation.SettingsUiState
import com.argus.agi.feature.settings.presentation.WorldEntityUiItem
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.TrainingManager
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.HardwareProfile
import com.argus.agi.platform.android.capture.ExcludedAppsManager
import com.argus.agi.data.acquisition.WebSearchEngine
import com.argus.agi.application.runtime.AccessibilityRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArgusViewModel(
    application: Application,
    private val neuralEngine: ArgusNeuralEngine,
    private val worldModel: WorldModel,
    private val trainingManager: TrainingManager,
    private val deviceGuard: DeviceGuard,
    private val hardwareProfile: HardwareProfile,
    private val excludedAppsManager: ExcludedAppsManager,
    private val webSearchEngine: WebSearchEngine,
    private val accessibilityRuntime: AccessibilityRuntime
) : AndroidViewModel(application) {

    // UI States
    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val initialHardwareInfo = hardwareProfile.getHardwareInfo()

    private val _learningState = MutableStateFlow(
        LearningUiState(
            safetyState = deviceGuard.safetyState.value.toLearningUiState(),
            hardwareInfo = LearningHardwareUiState(
                primaryAccelerator = initialHardwareInfo.primaryAccelerator
            )
        )
    )
    val learningState: StateFlow<LearningUiState> = _learningState.asStateFlow()

    private val _perceptionState = MutableStateFlow(
        PerceptionUiState(
            hardwareInfo = PerceptionHardwareUiState(
                socManufacturer = initialHardwareInfo.socManufacturer,
                socModel = initialHardwareInfo.socModel,
                primaryAccelerator = initialHardwareInfo.primaryAccelerator,
                npuApuDetected = initialHardwareInfo.npuApuDetected,
                vulkanSupported = initialHardwareInfo.vulkanSupported,
                vulkanVersionCode = initialHardwareInfo.vulkanVersionCode,
                cpuCores = initialHardwareInfo.cpuCores,
                maxHeapMb = initialHardwareInfo.maxHeapMb
            ),
            currentContextSummary = worldModel.currentContextSummary.value,
            isAccessibilityConnected = accessibilityRuntime.isConnected.value
        )
    )
    val perceptionState: StateFlow<PerceptionUiState> = _perceptionState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    init {
        // Seed initial greeting message
        _chatState.value = ChatUiState(messages = listOf(
            ChatMessage(
                isUser = false,
                text = "A.R.G.U.S Neural Core is active. Inference and model updates run locally on this device without a remote AI API.",
                thinkReasoning = "Model initialization: d_model=${neuralEngine.dModel}, heads=${neuralEngine.nHeads}, LoRA rank=${neuralEngine.loraRank}. Parameter count is derived from the active weight matrices."
            )
        ))

        loadInstalledApps()
        observeWorldEntities()
        observeLearningSources()
        observePerceptionSources()
    }

    fun getTotalParameters(): Long = neuralEngine.countTotalParameters()

    fun getAdapterSizeBytes(): Long = neuralEngine.calculateAdapterSizeBytes()

    fun toggleTrainingPause() {
        trainingManager.togglePauseTraining()
    }

    fun runBenchmark() {
        if (_learningState.value.isBenchmarking) return
        _learningState.update { it.copy(isBenchmarking = true) }
        viewModelScope.launch {
            val result = neuralEngine.runBenchmark(iterations = 12)
            _learningState.update {
                it.copy(
                    benchmark = LearningBenchmarkUiState(
                        tokensPerSecond = result.tokensPerSecond,
                        latencyMsPerToken = result.latencyMsPerToken,
                        memoryUsageMb = result.memoryUsageMb
                    ),
                    isBenchmarking = false
                )
            }
        }
    }

    fun sendMessage(text: String, attachedUri: Uri? = null, isImage: Boolean = false) {
        if (text.isBlank() && attachedUri == null) return

        var attachedFileName: String? = null
        var attachedImage: Uri? = null

        if (attachedUri != null) {
            if (isImage) {
                attachedImage = attachedUri
            } else {
                attachedFileName = "file_${System.currentTimeMillis()}"
            }
        }

        val userMsg = ChatMessage(
            isUser = true,
            text = text,
            attachedFileName = attachedFileName,
            attachedImageUri = attachedImage
        )
        _chatState.value = _chatState.value.copy(messages = _chatState.value.messages + userMsg)

        _chatState.value = _chatState.value.copy(isGenerating = true)
        viewModelScope.launch {
            // Context injection from World Model
            val worldSummary = worldModel.currentContextSummary.value
            val promptWithContext = "Context: $worldSummary\nQuery: $text"

            // 1. Math/Logic fast path evaluation
            val mathResult = tryEvaluateMath(text)

            // 2. Real neural inference
            val rawOutput = if (mathResult != null) {
                mathResult
            } else {
                neuralEngine.generate(promptWithContext, maxNewTokens = 60)
            }

            // 3. Self-Reflection
            val reflection = worldModel.reflectOnResponse(text, rawOutput)

            var finalAnswer = rawOutput
            val thinkingSb = StringBuilder()
            thinkingSb.append("Self-Reflection: ").append(reflection.reasoningNote)
            thinkingSb.append(" | Confidence: ").append(String.format("%.0f%%", reflection.confidenceScore * 100))

            // 4. Retrieve evidence for unresolved questions without treating it as training data.
            if (!reflection.isConfident || reflection.suggestedAction == "AUTO_LEARN_QUERY") {
                thinkingSb.append("\n-> Retrieving web sources for this response. Results are not automatically retained or used for training.")

                val webItems = webSearchEngine.search(text)
                if (webItems.isNotEmpty()) {
                    finalAnswer = "I am researching this topic using on-device web retrieval:\n\n" +
                            webItems.take(2).joinToString("\n\n") { "• **${it.title}**\n${it.snippet}" } +
                            "\n\n*These results are shown for this response only. They have not been added to the training queue.*"
                } else {
                    finalAnswer = "The local model could not resolve this topic, and web retrieval returned no results. No training candidate has been created. Personal learning remains unavailable until the explicit consent-and-review intake workflow is implemented."
                }
            }

            val assistantMsg = ChatMessage(
                isUser = false,
                text = finalAnswer,
                thinkReasoning = thinkingSb.toString()
            )
            _chatState.value = _chatState.value.copy(messages = _chatState.value.messages + assistantMsg)
            _chatState.value = _chatState.value.copy(isGenerating = false)
        }
    }

    fun importFileContent(uri: Uri) {
        sendMessage(
            text = "A document was selected. The current prototype does not yet route file content through the personal-experience intake and inference workflows. The document has not been durably retained or added to training.",
            attachedUri = uri
        )
    }

    fun importImageContent(uri: Uri) {
        sendMessage(
            text = "An image was selected. The current prototype does not yet connect image pixels to a vision-inference or personal-experience intake workflow. The image has not been durably retained or added to training.",
            attachedUri = uri,
            isImage = true
        )
    }

    fun updateCameraFrame(luminance: Float) {
        _perceptionState.update { it.copy(cameraLuminance = luminance) }
        worldModel.updateCameraContext(
            lightLevel = luminance,
            visualSummary = "Camera active. Luminance: ${String.format("%.1f", luminance)} lx"
        )
    }

    fun searchWebDirectly(query: String) {
        viewModelScope.launch {
            val results = webSearchEngine.search(query)
            // Retrieval results remain display evidence; they are not training inputs.
            _settingsState.value = _settingsState.value.copy(
                searchResults = results.map { item ->
                    SearchResultUiItem(title = item.title, snippet = item.snippet)
                }
            )
        }
    }

    fun executeActionClick(targetText: String): Boolean {
        return accessibilityRuntime.performClickAction(targetText)
    }

    fun toggleAppExclusion(packageName: String) {
        excludedAppsManager.toggleAppExclusion(packageName)
        loadInstalledApps()
    }

    fun clearChat() {
        _chatState.value = ChatUiState(
            messages = listOf(
                ChatMessage(
                    isUser = false,
                    text = "Conversation history cleared. Episodic memory and model weights remain stored on this device.",
                    thinkReasoning = "Message history reset. The on-device model retains its current learned parameters."
                )
            )
        )
    }

    fun clearTrainingQueue() {
        trainingManager.clearQueue()
    }

    fun clearCompletedTraining() {
        trainingManager.clearCompleted()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = excludedAppsManager.getInstalledApps()
            _settingsState.value = _settingsState.value.copy(
                installedApps = apps.map { app ->
                    InstalledAppUiItem(
                        appName = app.appName,
                        packageName = app.packageName,
                        isExcluded = app.isExcluded
                    )
                }
            )
        }
    }

    private fun observeWorldEntities() {
        viewModelScope.launch {
            worldModel.entitiesFlow.collect { entities ->
                _settingsState.value = _settingsState.value.copy(
                    worldEntities = entities.map { entity ->
                        WorldEntityUiItem(
                            key = entity.key,
                            value = entity.value,
                            source = entity.source
                        )
                    }
                )
            }
        }
    }

    private fun observeLearningSources() {
        viewModelScope.launch {
            trainingManager.isTrainingPaused.collect { value ->
                _learningState.update { it.copy(isPaused = value) }
            }
        }
        viewModelScope.launch {
            trainingManager.currentTrainingItem.collect { item ->
                _learningState.update { it.copy(currentTrainingItem = item?.toLearningUiState()) }
            }
        }
        viewModelScope.launch {
            trainingManager.nextQueue.collect { items ->
                _learningState.update { it.copy(nextQueue = items.map { item -> item.toLearningUiState() }) }
            }
        }
        viewModelScope.launch {
            trainingManager.completedQueue.collect { items ->
                _learningState.update { it.copy(completedQueue = items.map { item -> item.toLearningUiState() }) }
            }
        }
        viewModelScope.launch {
            trainingManager.lossHistory.collect { values ->
                _learningState.update { it.copy(lossHistory = values) }
            }
        }
        viewModelScope.launch {
            trainingManager.totalStepsExecuted.collect { value ->
                _learningState.update { it.copy(totalStepsExecuted = value) }
            }
        }
        viewModelScope.launch {
            deviceGuard.safetyState.collect { value ->
                _learningState.update { it.copy(safetyState = value.toLearningUiState()) }
            }
        }
    }

    private fun observePerceptionSources() {
        viewModelScope.launch {
            worldModel.currentContextSummary.collect { value ->
                _perceptionState.update { it.copy(currentContextSummary = value) }
            }
        }
        viewModelScope.launch {
            accessibilityRuntime.isConnected.collect { value ->
                _perceptionState.update { it.copy(isAccessibilityConnected = value) }
            }
        }
    }

    private fun tryEvaluateMath(text: String): String? {
        val clean = text.trim().lowercase().removePrefix("calculate").removePrefix("compute").removePrefix("what is").trim()
        val mathRegex = Regex("^([0-9.]+)\\s*([+\\-*/^])\\s*([0-9.]+)$")
        val match = mathRegex.find(clean) ?: return null

        val (sNum1, op, sNum2) = match.destructured
        val n1 = sNum1.toDoubleOrNull() ?: return null
        val n2 = sNum2.toDoubleOrNull() ?: return null

        val res = when (op) {
            "+" -> n1 + n2
            "-" -> n1 - n2
            "*" -> n1 * n2
            "/" -> if (n2 != 0.0) n1 / n2 else return "Division by zero is undefined."
            "^" -> Math.pow(n1, n2)
            else -> return null
        }
        return "Mathematical reasoning result: $n1 $op $n2 = $res"
    }
}

private fun com.argus.agi.model.TrainingData.toLearningUiState(): TrainingQueueItemUiState {
    return TrainingQueueItemUiState(
        id = id,
        source = source,
        title = title,
        content = content,
        finalLoss = finalLoss,
        stepDurationMs = stepDurationMs
    )
}

private fun com.argus.agi.platform.android.device.DeviceSafetyState.toLearningUiState(): LearningSafetyUiState {
    return LearningSafetyUiState(
        isSafeToTrain = isSafeToTrain,
        statusMessage = statusMessage
    )
}
