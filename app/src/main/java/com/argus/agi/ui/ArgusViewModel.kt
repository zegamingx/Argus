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
import com.argus.agi.feature.learning.presentation.DatasetPurposeUi
import com.argus.agi.feature.learning.presentation.DatasetImportPreviewUiState
import com.argus.agi.feature.learning.presentation.LocalCorpusUiState
import com.argus.agi.feature.perception.presentation.PerceptionHardwareUiState
import com.argus.agi.feature.perception.presentation.PerceptionUiState
import com.argus.agi.feature.settings.presentation.InstalledAppUiItem
import com.argus.agi.feature.settings.presentation.SearchResultUiItem
import com.argus.agi.feature.settings.presentation.SettingsUiState
import com.argus.agi.feature.settings.presentation.WorldEntityUiItem
import com.argus.agi.model.ArgusNeuralEngine
import com.argus.agi.model.WorldModel
import com.argus.agi.platform.android.device.DeviceGuard
import com.argus.agi.platform.android.device.SystemHardwareInfo
import com.argus.agi.platform.android.capture.ExcludedAppsManager
import com.argus.agi.data.acquisition.WebSearchEngine
import com.argus.agi.application.runtime.AccessibilityRuntime
import com.argus.agi.platform.android.storage.CorpusImportPreview
import com.argus.agi.core.training.DatasetPurpose
import com.argus.agi.core.training.LocalCorpusSummary
import com.argus.agi.platform.android.storage.LocalDatasetImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val deviceGuard: DeviceGuard,
    private val initialHardwareInfo: SystemHardwareInfo,
    private val excludedAppsManager: ExcludedAppsManager,
    private val webSearchEngine: WebSearchEngine,
    private val accessibilityRuntime: AccessibilityRuntime,
    private val localDatasetImporter: LocalDatasetImporter
) : AndroidViewModel(application) {

    // UI States
    private val _chatState = MutableStateFlow(ChatUiState())
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

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
        observeLearningSafety()
        observePerceptionSources()
        loadLocalCorpora()
    }

    fun getTotalParameters(): Long = neuralEngine.countTotalParameters()

    fun getAdapterSizeBytes(): Long = neuralEngine.calculateAdapterSizeBytes()

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

    fun setDatasetPurpose(purpose: DatasetPurposeUi, enabled: Boolean) {
        _learningState.update { state ->
            val currentPurposes = state.datasetImport.selectedPurposes
            val updatedPurposes = if (enabled) {
                currentPurposes + purpose
            } else {
                currentPurposes - purpose
            }
            state.copy(
                datasetImport = state.datasetImport.copy(
                    selectedPurposes = updatedPurposes,
                    errorMessage = null
                )
            )
        }
    }

    fun stageLocalDataset(uris: List<Uri>) {
        if (uris.isEmpty()) return
        stageDataset { purposes ->
            localDatasetImporter.stage(uris, purposes)
        }
    }

    fun stageBundledArgusId() {
        stageDataset { purposes ->
            localDatasetImporter.stageBundledArgusId(purposes)
        }
    }

    fun stageBundledDigiTransId() {
        stageDataset { purposes ->
            localDatasetImporter.stageBundledDigiTransId(purposes)
        }
    }

    private fun stageDataset(
        importAction: suspend (Set<DatasetPurpose>) -> CorpusImportPreview
    ) {
        val currentImportState = _learningState.value.datasetImport
        if (currentImportState.isBusy) return
        if (currentImportState.selectedPurposes.isEmpty()) {
            _learningState.update { state ->
                state.copy(
                    datasetImport = state.datasetImport.copy(
                        errorMessage = "Select at least one purpose before importing."
                    )
                )
            }
            return
        }

        val purposes = currentImportState.selectedPurposes.map { purpose ->
            when (purpose) {
                DatasetPurposeUi.MODEL_TRAINING -> DatasetPurpose.MODEL_TRAINING
                DatasetPurposeUi.TOKENIZER_FREQUENCY_FITTING -> DatasetPurpose.TOKENIZER_FREQUENCY_FITTING
                DatasetPurposeUi.ALIGNED_TOKENIZER_DEVELOPMENT -> DatasetPurpose.ALIGNED_TOKENIZER_DEVELOPMENT
            }
        }.toSet()

        _learningState.update { state ->
            state.copy(
                datasetImport = state.datasetImport.copy(
                    isBusy = true,
                    errorMessage = null,
                    statusMessage = "Streaming selected files and preparing a local preview.",
                    stagedPreview = null
                )
            )
        }

        viewModelScope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    importAction(purposes)
                }
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            statusMessage = "Preview ready. Review the counts and warnings before committing the corpus.",
                            stagedPreview = preview.toLearningUiState(),
                            errorMessage = null
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            statusMessage = "The local import did not create a corpus revision.",
                            errorMessage = exception.message ?: "The selected files could not be imported."
                        )
                    )
                }
            }
        }
    }

    fun commitStagedLocalDataset() {
        val importState = _learningState.value.datasetImport
        val preview = importState.stagedPreview ?: return
        if (importState.isBusy) return

        _learningState.update { state ->
            state.copy(
                datasetImport = state.datasetImport.copy(
                    isBusy = true,
                    errorMessage = null,
                    statusMessage = "Committing the corpus revision to app-private storage."
                )
            )
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    localDatasetImporter.commit(preview.stageId)
                }
                val committedCorpora = withContext(Dispatchers.IO) {
                    localDatasetImporter.listCommitted().map { it.toLearningUiState() }
                }
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            statusMessage = "Corpus revision committed locally. No training session was started.",
                            stagedPreview = null,
                            committedCorpora = committedCorpora,
                            errorMessage = null
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            statusMessage = "The corpus revision was not confirmed as committed.",
                            errorMessage = exception.message ?: "The staged corpus could not be committed."
                        )
                    )
                }
            }
        }
    }

    fun discardStagedLocalDataset() {
        val importState = _learningState.value.datasetImport
        val preview = importState.stagedPreview ?: return
        if (importState.isBusy) return

        _learningState.update { state ->
            state.copy(
                datasetImport = state.datasetImport.copy(
                    isBusy = true,
                    errorMessage = null,
                    statusMessage = "Discarding staged corpus data."
                )
            )
        }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    localDatasetImporter.discard(preview.stageId)
                }
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            statusMessage = "The staged import was discarded.",
                            stagedPreview = null,
                            errorMessage = null
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            isBusy = false,
                            errorMessage = exception.message ?: "The staged import could not be discarded."
                        )
                    )
                }
            }
        }
    }

    private fun loadLocalCorpora() {
        viewModelScope.launch {
            try {
                val corpora = withContext(Dispatchers.IO) {
                    localDatasetImporter.listCommitted().map { it.toLearningUiState() }
                }
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(committedCorpora = corpora)
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _learningState.update { state ->
                    state.copy(
                        datasetImport = state.datasetImport.copy(
                            errorMessage = "Existing local corpus manifests could not be read."
                        )
                    )
                }
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

    private fun observeLearningSafety() {
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


private fun CorpusImportPreview.toLearningUiState(): DatasetImportPreviewUiState {
    return DatasetImportPreviewUiState(
        stageId = stageId,
        corpusId = corpusId,
        selectedSourceCount = selectedSourceCount,
        parsedDocumentCount = parsedDocumentCount,
        acceptedRecordCount = acceptedRecordCount,
        duplicateRecordCount = duplicateRecordCount,
        rejectedRecordCount = rejectedRecordCount,
        skippedFileCount = skippedFileCount,
        importedBytes = importedBytes,
        trainRecordCount = trainRecordCount,
        validationRecordCount = validationRecordCount,
        testRecordCount = testRecordCount,
        warnings = warnings
    )
}

private fun LocalCorpusSummary.toLearningUiState(): LocalCorpusUiState {
    return LocalCorpusUiState(
        corpusId = corpusId,
        revision = revision,
        importedAtEpochMillis = importedAtEpochMillis,
        recordCount = recordCount,
        trainRecordCount = trainRecordCount,
        validationRecordCount = validationRecordCount,
        testRecordCount = testRecordCount,
        lastAdmissionDecision = lastAdmissionDecision,
        purposeLabels = purposes.map { purpose ->
            when (purpose) {
                DatasetPurpose.MODEL_TRAINING -> "Model training"
                DatasetPurpose.TOKENIZER_FREQUENCY_FITTING -> "Tokenizer frequency fitting"
                DatasetPurpose.ALIGNED_TOKENIZER_DEVELOPMENT -> "Aligned tokenizer development"
            }
        }.sorted()
    )
}

private fun com.argus.agi.platform.android.device.DeviceSafetyState.toLearningUiState(): LearningSafetyUiState {
    return LearningSafetyUiState(
        isSafeToTrain = isSafeToTrain,
        statusMessage = statusMessage
    )
}
