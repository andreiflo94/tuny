package com.heixss.guitartuner.data.repository

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.heixss.guitartuner.data.audio.PitchDetector
import com.heixss.guitartuner.domain.repository.TunerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

class TunerRepositoryImpl(context: Context) : TunerRepository {

    private val appContext = context.applicationContext
    private val sampleRate = 44100

    // Analysis window: 4096 samples for autocorrelation accuracy down to ~70 Hz.
    private val windowSamples = 4096

    // Hop: read this many new samples before running the detector.
    // 1024 / 44100 ≈ 23 ms → needle updates ~43 times per second.
    private val hopSamples = 1024

    override fun frequencyFlow(): Flow<Float?> = flow {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, windowSamples * 2),
        )
        record.startRecording()

        // Sliding analysis window.  New hops are appended to the right;
        // old samples shift out to the left.
        val window = ShortArray(windowSamples)
        val hop    = ShortArray(hopSamples)
        var filled = 0  // how many samples are valid in `window` so far

        try {
            while (currentCoroutineContext().isActive) {
                val read = record.read(hop, 0, hopSamples)
                if (read <= 0) continue

                // Shift window left by `read` samples and append the new hop.
                val shift = read.coerceAtMost(windowSamples)
                System.arraycopy(window, shift, window, 0, windowSamples - shift)
                System.arraycopy(hop, 0, window, windowSamples - shift, shift)

                filled = (filled + shift).coerceAtMost(windowSamples)

                // Don't run the detector until the window is fully populated.
                if (filled >= windowSamples) {
                    emit(PitchDetector.detect(window, sampleRate))
                }
            }
        } finally {
            record.stop()
            record.release()
        }
    }.flowOn(Dispatchers.IO)
}
