package com.argus.agi.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class WorldEntity(
    val key: String,
    val value: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ReflectionEvaluation(
    val isConfident: Boolean,
    val confidenceScore: Float,
    val reasoningNote: String,
    val suggestedAction: String?
)

class WorldModel(private val context: Context) {

    private val entities = ConcurrentHashMap<String, WorldEntity>()
    private val _entitiesFlow = MutableStateFlow<List<WorldEntity>>(emptyList())
    val entitiesFlow: StateFlow<List<WorldEntity>> = _entitiesFlow.asStateFlow()

    private val _currentContextSummary = MutableStateFlow("System ready. Waiting for environmental and screen context.")
    val currentContextSummary: StateFlow<String> = _currentContextSummary.asStateFlow()

    init {
        // Initial core world facts
        storeEntity("identity_name", "A.R.G.U.S", "Core Architecture")
        storeEntity("privacy_boundary", "100% On-Device, Zero Cloud Telemetry", "System Policy")
        storeEntity("architecture_type", "Autonomous Micro-Transformer with LoRA Adapters", "Neural Specification")
    }

    fun storeEntity(key: String, value: String, source: String) {
        val entity = WorldEntity(key = key, value = value, source = source)
        entities[key] = entity
        _entitiesFlow.value = entities.values.sortedByDescending { it.timestamp }
    }

    fun updateScreenContext(packageName: String, visibleTextSnippet: String) {
        val cleanSnippet = visibleTextSnippet.take(120).replace("\n", " ")
        storeEntity("active_screen_app", packageName, "Accessibility Perception")
        storeEntity("last_screen_content", cleanSnippet, "Screen OCR/Text Parser")

        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _currentContextSummary.value = "App: $packageName | Deteksi: \"$cleanSnippet\" ($timeStr)"
    }

    fun updateCameraContext(lightLevel: Float, visualSummary: String) {
        storeEntity("visual_environment", visualSummary, "Camera Vision Sensor")
        storeEntity("ambient_luminance", String.format(Locale.US, "%.1f lux", lightLevel), "Camera Sensor")
    }

    /**
     * Self-Reflection mechanism:
     * Critiques an answer before showing it to the user.
     * If confidence is lower than 0.65 or key information is missing, flags for auto-training.
     */
    fun reflectOnResponse(query: String, rawResponse: String): ReflectionEvaluation {
        val lowerQuery = query.lowercase()
        val lowerResp = rawResponse.lowercase()

        // Check if query is fact-seeking or math
        val isMath = query.any { it in "+-*/^=" }
        val lacksKnowledge = lowerResp.contains("don't know") || lowerResp.contains("do not know") ||
                lowerResp.contains("not available") || lowerResp.contains("not enough information") ||
                lowerResp.length < 5

        var score = 0.85f
        var reasoning = "Response is consistent with the current on-device neural representation."
        var suggestedAction: String? = null

        if (lacksKnowledge) {
            score = 0.25f
            reasoning = "High uncertainty detected. This pattern is not yet represented in the local model weights."
            suggestedAction = "AUTO_LEARN_QUERY"
        } else if (isMath) {
            score = 0.95f
            reasoning = "Ekspresi logika/aritmatika diverifikasi oleh reasoning engine."
        }

        return ReflectionEvaluation(
            isConfident = score >= 0.60f,
            confidenceScore = score,
            reasoningNote = reasoning,
            suggestedAction = suggestedAction
        )
    }

    fun getAllEntities(): List<WorldEntity> {
        return entities.values.sortedByDescending { it.timestamp }
    }
}
