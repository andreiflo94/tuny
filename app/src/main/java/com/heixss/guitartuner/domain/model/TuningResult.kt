package com.heixss.guitartuner.domain.model

data class TuningResult(
    val frequency: Float,
    val noteName: String,
    val octave: Int,
    val centsDeviation: Float,
    val nearestGuitarString: GuitarString?,
)
