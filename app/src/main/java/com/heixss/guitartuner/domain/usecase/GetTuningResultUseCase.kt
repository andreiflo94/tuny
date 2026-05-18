package com.heixss.guitartuner.domain.usecase

import com.heixss.guitartuner.domain.model.GuitarString
import com.heixss.guitartuner.domain.model.TuningResult
import com.heixss.guitartuner.domain.repository.TunerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

class GetTuningResultUseCase(private val repository: TunerRepository) {

    operator fun invoke(): Flow<TuningResult?> =
        repository.frequencyFlow()
            .map { freq -> freq?.let(::toResult) }
            // If the UI thread falls behind (recomposition) drop stale values
            // so the needle always reflects the latest detected pitch.
            .conflate()

    private fun toResult(frequency: Float): TuningResult {
        val midiExact  = 12.0 * log2(frequency / 440.0) + 69.0
        val midiRounded = midiExact.roundToInt()
        val cents      = ((midiExact - midiRounded) * 100).toFloat().coerceIn(-50f, 50f)
        val noteIndex  = ((midiRounded % 12) + 12) % 12
        val octave     = (midiRounded / 12) - 1
        val nearestString = GuitarString.entries.minByOrNull { abs(it.frequency - frequency) }
        return TuningResult(
            frequency        = frequency,
            noteName         = NOTE_NAMES[noteIndex],
            octave           = octave,
            centsDeviation   = cents,
            nearestGuitarString = nearestString,
        )
    }

    companion object {
        private val NOTE_NAMES =
            arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    }
}
