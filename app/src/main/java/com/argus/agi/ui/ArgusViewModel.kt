package com.argus.agi.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.argus.agi.feature.chat.presentation.ChatMessage
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.TrainingManager
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.HardwareProfile
import com.argus.agi.model.BenchmarkResult
import com.argus.agi.model.TrainingData
import com.argus.agi.model.WorldEntity
import com.argus.agi.platform.android.device.DeviceSafetyState
import com.argus.agi.platform.android.capture.ExcludedAppsManager
import com.argus.agi.platform.android.capture.InstalledAppItem
import com.argus.agi.data.acquisition.SearchResultItem
import com.argus.agi.platform.android.device.SystemHardwareInfo
import com.argus.agi.data.acquisition.WebSearchEngine
import com.argus.agi.application.runtime.AccessibilityRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _hardwareInfo = MutableStateFlow(hardwareProfile.getHardwareInfo())
    val hardwareInfo: StateFlow<SystemHardwareInfo> = _hardwareInfo.asStateFlow()

    val safetyState: StateFlow<DeviceSafetyState> = deviceGuard.safetyState
    val currentTrainingItem: StateFlow<TrainingData?> = trainingManager.currentTrainingItem
    val nextQueue: StateFlow<List<TrainingData>> = trainingManager.nextQueue
    val completedQueue: StateFlow<List<TrainingData>> = trainingManager.completedQueue
    val lossHistory: StateFlow<List<Float>> = trainingManager.lossHistory
    val isTrainingPaused: StateFlow<Boolean> = trainingManager.isTrainingPaused
    val totalStepsExecuted: StateFlow<Int> = trainingManager.totalStepsExecuted
    val worldEntities: StateFlow<List<WorldEntity>> = worldModel.entitiesFlow
    val currentContextSummary: StateFlow<String> = worldModel.currentContextSummary

    private val _benchmarkState = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkState: StateFlow<BenchmarkResult?> = _benchmarkState.asStateFlow()

    private val _isBenchmarking = MutableStateFlow(false)
    val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _cameraLuminance = MutableStateFlow(0f)
    val cameraLuminance: StateFlow<Float> = _cameraLuminance.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val searchResults: StateFlow<List<SearchResultItem>> = _searchResults.asStateFlow()
    val isAccessibilityConnected: StateFlow<Boolean> = accessibilityRuntime.isConnected

    init {
        // Seed initial greeting message
        _messages.value = listOf(
            ChatMessage(
                isUser = false,
                text = "A.R.G.U.S Neural Core is active. Inference and model updates run locally on this device without a remote AI API.",
                thinkReasoning = "Model initialization: d_model=${neuralEngine.dModel}, heads=${neuralEngine.nHeads}, LoRA rank=${neuralEngine.loraRank}. Parameter count is derived from the active weight matrices."
            )
        )

        loadInstalledApps()
    }

    fun getTotalParameters(): Long = neuralEngine.countTotalParameters()

    fun getAdapterSizeBytes(): Long = neuralEngine.calculateAdapterSizeBytes()

    fun toggleTrainingPause() {
        trainingManager.togglePauseTraining()
    }

    fun runBenchmark() {
        if (_isBenchmarking.value) return
        _isBenchmarking.value = true
        viewModelScope.launch {
            val res = neuralEngine.runBenchmark(iterations = 12)
            _benchmarkState.value = res
            _isBenchmarking.value = false
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
        _messages.value = _messages.value + userMsg

        _isGenerating.value = true
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

            // 4. Turn unresolved questions into user-reviewable learning candidates.
            if (!reflection.isConfident || reflection.suggestedAction == "AUTO_LEARN_QUERY") {
                thinkingSb.append("\n-> Starting on-device web retrieval and adding results to the learning queue.")

                // Perform real web search to gather true information
                val webItems = webSearchEngine.search(text)
                if (webItems.isNotEmpty()) {
                    val learnedText = webItems.joinToString("\n") { "${it.title}: ${it.snippet}" }
                    // Enqueue to training manager immediately!
                    trainingManager.enqueueData(
                        source = "Chat Inquiry Learner",
                        title = "Question: ${text.take(30)}",
                        content = "Query: $text. Retrieved knowledge: $learnedText"
                    )

                    finalAnswer = "I am researching this topic using on-device web retrieval:\n\n" +
                            webItems.take(2).joinToString("\n\n") { "• **${it.title}**\n${it.snippet}" } +
                            "\n\n*These results have been added to the local learning queue for review.*"
                } else {
                    // Enqueue the query itself to be learned
                    trainingManager.enqueueData(
                        source = "Chat Unresolved",
                        title = "Inquiry: ${text.take(30)}",
                        content = text
                    )
                    finalAnswer = "This topic is new to the model. The question has been added to the local learning queue for evaluation."
                }
            }

            val assistantMsg = ChatMessage(
                isUser = false,
                text = finalAnswer,
                thinkReasoning = thinkingSb.toString()
            )
            _messages.value = _messages.value + assistantMsg
            _isGenerating.value = false
        }
    }

    fun importFileContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = getApplication<Application>().contentResolver
                cr.openInputStream(uri)?.use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream))
                    val content = reader.readText()
                    val fileName = "Document (${content.length} characters)"

                    withContext(Dispatchers.Main) {
                        sendMessage("Analyzing the imported document...", uri, isImage = false)
                    }

                    // Enqueue directly into on-device training
                    trainingManager.enqueueData(
                        source = "File Import",
                        title = fileName,
                        content = content.take(1500)
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun importImageContent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cr = getApplication<Application>().contentResolver
                cr.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val w = bitmap.width
                        val h = bitmap.height
                        val desc = "Image $w x $h px imported for on-device visual perception."

                        withContext(Dispatchers.Main) {
                            sendMessage(desc, uri, isImage = true)
                        }

                        // Enqueue visual token stimulus into training
                        trainingManager.enqueueData(
                            source = "Image Vision Import",
                            title = "Visual $w x $h",
                            content = "Visual perception frame. Resolution: $w x $h. Local visual analysis."
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun updateCameraFrame(luminance: Float) {
        _cameraLuminance.value = luminance
        worldModel.updateCameraContext(
            lightLevel = luminance,
            visualSummary = "Camera active. Luminance: ${String.format("%.1f", luminance)} lx"
        )
    }

    fun searchWebDirectly(query: String) {
        viewModelScope.launch {
            val results = webSearchEngine.search(query)
            _searchResults.value = results
            if (results.isNotEmpty()) {
                val textToTrain = results.joinToString(" ") { it.snippet }
                trainingManager.enqueueData(
                    source = "Web Search Tool",
                    title = "Web: $query",
                    content = textToTrain
                )
            }
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
        _messages.value = listOf(
            ChatMessage(
                isUser = false,
                text = "Conversation history cleared. Episodic memory and model weights remain stored on this device.",
                thinkReasoning = "Message history reset. The on-device model retains its current learned parameters."
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
            _installedApps.value = apps
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
