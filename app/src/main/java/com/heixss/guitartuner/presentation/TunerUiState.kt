package com.heixss.guitartuner.presentation

import com.heixss.guitartuner.domain.model.GuitarString
import com.heixss.guitartuner.domain.model.TuningResult

data class TunerUiState(
    val isListening: Boolean = false,
    val permissionDenied: Boolean = false,
    val tuningResult: TuningResult? = null,
    val signalDetected: Boolean = false,
    val selectedString: GuitarString? = null,
)
