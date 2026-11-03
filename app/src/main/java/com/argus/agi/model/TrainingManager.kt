package com.argus.agi.model

import android.content.Context
import com.argus.agi.sensor.DeviceGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

enum class TrainingStatus {
    QUEUED,
    TRAINING,
    COMPLETED,
    PAUSED
}

data class TrainingData(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TrainingStatus = TrainingStatus.QUEUED,
    val finalLoss: Float? = null,
    val stepDurationMs: Long = 0L
)

class TrainingManager(
    private val context: Context,
    private val neuralEngine: ArgusNeuralEngine,
    private val deviceGuard: DeviceGuard,
    private val worldModel: WorldModel
) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val pendingQueue = ConcurrentLinkedQueue<TrainingData>()

    private val _currentTrainingItem = MutableStateFlow<TrainingData?>(null)
    val currentTrainingItem: StateFlow<TrainingData?> = _currentTrainingItem.asStateFlow()

    private val _nextQueue = MutableStateFlow<List<TrainingData>>(emptyList())
    val nextQueue: StateFlow<List<TrainingData>> = _nextQueue.asStateFlow()

    private val _completedQueue = MutableStateFlow<List<TrainingData>>(emptyList())
    val completedQueue: StateFlow<List<TrainingData>> = _completedQueue.asStateFlow()

    private val _lossHistory = MutableStateFlow<List<Float>>(emptyList())
    val lossHistory: StateFlow<List<Float>> = _lossHistory.asStateFlow()

    private val _isTrainingPaused = MutableStateFlow(false)
    val isTrainingPaused: StateFlow<Boolean> = _isTrainingPaused.asStateFlow()

    private val _totalStepsExecuted = MutableStateFlow(0)
    val totalStepsExecuted: StateFlow<Int> = _totalStepsExecuted.asStateFlow()

    private val completedList = mutableListOf<TrainingData>()

    init {
        // Start continuous autonomous background training loop
        startTrainingWorker()
    }

    fun pauseTraining() {
        _isTrainingPaused.value = true
    }

    fun resumeTraining() {
        _isTrainingPaused.value = false
    }

    fun togglePauseTraining() {
        _isTrainingPaused.value = !_isTrainingPaused.value
    }

    fun clearQueue() {
        pendingQueue.clear()
        refreshNextQueue()
    }

    fun clearCompleted() {
        completedList.clear()
        _completedQueue.value = emptyList()
    }

    /**
     * Enqueues a new data stimulus into the training queue.
     */
    fun enqueueData(source: String, title: String, content: String) {
        if (content.isBlank()) return
        val item = TrainingData(
            source = source,
            title = title,
            content = content.trim(),
            status = TrainingStatus.QUEUED
        )
        pendingQueue.add(item)
        refreshNextQueue()
    }

    private fun refreshNextQueue() {
        _nextQueue.value = pendingQueue.toList()
    }

    private fun startTrainingWorker() {
        scope.launch {
            while (isActive) {
                // Check if user paused training
                if (_isTrainingPaused.value) {
                    delay(1000)
                    continue
                }

                // Check thermal and battery condition
                if (!deviceGuard.isSafeToTrain()) {
                    delay(3000)
                    continue
                }

                // Check if there is data in queue
                val item = pendingQueue.poll()
                if (item == null) {
                    _currentTrainingItem.value = null
                    delay(1500)
                    continue
                }

                refreshNextQueue()

                // Mark current training
                val activeItem = item.copy(status = TrainingStatus.TRAINING)
                _currentTrainingItem.value = activeItem

                // Execute real neural training step
                val startTime = System.currentTimeMillis()
                val stepLoss = neuralEngine.trainStep(item.content)
                val duration = System.currentTimeMillis() - startTime

                val finishedItem = activeItem.copy(
                    status = TrainingStatus.COMPLETED,
                    finalLoss = stepLoss,
                    stepDurationMs = duration
                )

                // Record real loss history
                val updatedLossList = _lossHistory.value.toMutableList()
                updatedLossList.add(stepLoss)
                if (updatedLossList.size > 50) {
                    updatedLossList.removeAt(0)
                }
                _lossHistory.value = updatedLossList

                // Record completed item
                completedList.add(0, finishedItem)
                if (completedList.size > 30) {
                    completedList.removeAt(completedList.size - 1)
                }
                _completedQueue.value = completedList.toList()

                _totalStepsExecuted.value += 1
                _currentTrainingItem.value = null

                // Throttle slightly to keep CPU thermals cool and prevent battery spikes
                delay(800)
            }
        }
    }
}
