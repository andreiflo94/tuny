package com.heixss.guitartuner.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.heixss.guitartuner.data.repository.TunerRepositoryImpl
import com.heixss.guitartuner.domain.usecase.GetTuningResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val useCase = GetTuningResultUseCase(TunerRepositoryImpl(application))

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var listeningJob: Job? = null
    private var signalHoldJob: Job? = null

    fun handleIntent(intent: TunerIntent) {
        when (intent) {
            TunerIntent.StartTuning     -> startTuning()
            TunerIntent.StopTuning      -> stopTuning()
            TunerIntent.PermissionDenied -> _uiState.update {
                it.copy(permissionDenied = true, isListening = false)
            }
            is TunerIntent.SelectString -> _uiState.update { it.copy(selectedString = intent.string) }
        }
    }

    private fun startTuning() {
        if (listeningJob?.isActive == true) return
        _uiState.update { it.copy(isListening = true, permissionDenied = false) }
        listeningJob = viewModelScope.launch {
            useCase().collect { result ->
                if (result != null) {
                    // Fresh pitch — cancel any pending hide and update display immediately.
                    signalHoldJob?.cancel()
                    _uiState.update { state ->
                        state.copy(tuningResult = result, signalDetected = true)
                    }
                } else {
                    // No signal — always restart the hold timer so the last reading
                    // stays visible for SIGNAL_HOLD_MS before the display clears.
                    // (Reading signalDetected inside update {} avoids any TOCTOU issue.)
                    signalHoldJob?.cancel()
                    signalHoldJob = launch {
                        delay(SIGNAL_HOLD_MS)
                        _uiState.update { it.copy(signalDetected = false) }
                    }
                }
            }
        }
    }

    private fun stopTuning() {
        signalHoldJob?.cancel()
        listeningJob?.cancel()
        signalHoldJob = null
        listeningJob  = null
        _uiState.update { it.copy(isListening = false, tuningResult = null, signalDetected = false) }
    }

    override fun onCleared() {
        super.onCleared()
        signalHoldJob?.cancel()
        listeningJob?.cancel()
    }

    companion object {
        private const val SIGNAL_HOLD_MS = 2200L
    }
}
