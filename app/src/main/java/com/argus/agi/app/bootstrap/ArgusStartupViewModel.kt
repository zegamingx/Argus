package com.argus.agi.app.bootstrap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.argus.agi.app.entry.ArgusApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ArgusStartupStage {
    INSPECTING_DEVICE,
    PREPARING_RUNTIME,
    RUNTIME_PREPARED,
    FAILED
}

data class ArgusStartupUiState(
    val stage: ArgusStartupStage = ArgusStartupStage.INSPECTING_DEVICE,
    val deviceModel: String? = null,
    val errorMessage: String? = null
) {
    val isPreparing: Boolean
        get() = stage == ArgusStartupStage.INSPECTING_DEVICE ||
            stage == ArgusStartupStage.PREPARING_RUNTIME

    val canOpenWorkspace: Boolean
        get() = stage == ArgusStartupStage.RUNTIME_PREPARED
}

class ArgusStartupViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as ArgusApp).container

    private val _uiState = MutableStateFlow(ArgusStartupUiState())
    val uiState: StateFlow<ArgusStartupUiState> = _uiState.asStateFlow()

    private var startupJob: Job? = null

    init {
        prepareRuntime()
    }

    fun retry() {
        prepareRuntime()
    }

    private fun prepareRuntime() {
        if (startupJob?.isActive == true) return

        startupJob = viewModelScope.launch {
            _uiState.value = ArgusStartupUiState(
                stage = ArgusStartupStage.INSPECTING_DEVICE
            )

            try {
                val hardwareInfo = withContext(Dispatchers.IO) {
                    container.hardwareProfile.getHardwareInfo()
                }

                _uiState.value = ArgusStartupUiState(
                    stage = ArgusStartupStage.PREPARING_RUNTIME,
                    deviceModel = hardwareInfo.socModel
                        .takeIf { it.isNotBlank() && it != "Unknown" }
                )

                withContext(Dispatchers.Default) {
                    container.neuralEngine
                }

                _uiState.value = _uiState.value.copy(
                    stage = ArgusStartupStage.RUNTIME_PREPARED,
                    errorMessage = null
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.value = ArgusStartupUiState(
                    stage = ArgusStartupStage.FAILED,
                    errorMessage = "Argus could not prepare its local runtime. Retry, and check available app storage if the problem persists."
                )
            }
        }
    }
}
