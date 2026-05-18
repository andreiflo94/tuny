package com.heixss.guitartuner.presentation

import com.heixss.guitartuner.domain.model.GuitarString

sealed interface TunerIntent {
    data object StartTuning : TunerIntent
    data object StopTuning : TunerIntent
    data object PermissionDenied : TunerIntent
    data class SelectString(val string: GuitarString?) : TunerIntent
}
