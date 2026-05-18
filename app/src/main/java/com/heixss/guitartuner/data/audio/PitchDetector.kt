package com.heixss.guitartuner.data.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * McLeod Pitch Method (MPM) — Normalized Square Difference Function (NSDF).
 *
 * nsdf(tau) = 2 * acf(tau) / (m_x(0, N-tau) + m_x(tau, N))
 *
 * Values live in [-1, 1]. At the fundamental period the NSDF is close to +1.
 * We pick the FIRST local maximum that exceeds PEAK_RATIO * globalMax.
 * "First" means smallest lag, i.e. highest frequency — this naturally avoids
 * the sub-octave (2T, 3T …) false peaks that plagued the raw-ACF approach.
 */
object PitchDetector {

    // Guitar standard tuning: E2 82 Hz … E4 330 Hz, with ±50 ¢ headroom.
    private const val MIN_FREQUENCY = 70f
    private const val MAX_FREQUENCY = 380f

    // ~0.9 % of full-scale 16-bit.  Quiet acoustic plucks read ~150–400.
    private const val RMS_THRESHOLD = 150f

    // Minimum NSDF value required for ANY peak to be considered.
    private const val MIN_NSDF = 0.30f

    // A peak must be at least this fraction of the global NSDF maximum.
    private const val PEAK_RATIO = 0.80f

    fun detect(buffer: ShortArray, sampleRate: Int): Float? {
        val n = buffer.size

        // ── Noise gate ────────────────────────────────────────────────────
        var sumSq = 0L
        for (s in buffer) sumSq += s.toLong() * s
        if (sqrt(sumSq.toFloat() / n) < RMS_THRESHOLD) return null

        // ── Hann window + normalize to float ─────────────────────────────
        val signal = FloatArray(n) { i ->
            val w = 0.5f * (1f - cos(2f * PI.toFloat() * i / (n - 1)))
            buffer[i] / 32768f * w
        }

        val minLag = (sampleRate / MAX_FREQUENCY).toInt()
        val maxLag = (sampleRate / MIN_FREQUENCY).toInt().coerceAtMost(n / 2 - 1)

        // ── NSDF ──────────────────────────────────────────────────────────
        val nsdf = FloatArray(maxLag + 1)
        computeNsdf(signal, nsdf, minLag, maxLag)

        // ── Global maximum check ──────────────────────────────────────────
        var globalMax = 0f
        for (lag in minLag..maxLag) if (nsdf[lag] > globalMax) globalMax = nsdf[lag]
        if (globalMax < MIN_NSDF) return null

        // ── First local maximum above PEAK_RATIO * globalMax ──────────────
        // Iterating from small lag (high freq) to large lag (low freq) means
        // we hit the true fundamental BEFORE any sub-octave artifact.
        val threshold = globalMax * PEAK_RATIO
        var bestLag = -1
        for (lag in (minLag + 1) until maxLag) {
            val curr = nsdf[lag]
            if (curr > threshold && curr > nsdf[lag - 1] && curr > nsdf[lag + 1]) {
                bestLag = lag
                break
            }
        }
        if (bestLag < 0) return null

        // ── Parabolic interpolation (sub-sample accuracy) ─────────────────
        val y0 = nsdf[bestLag - 1]
        val y1 = nsdf[bestLag]
        val y2 = nsdf[bestLag + 1]
        val denom = 2f * y1 - y0 - y2
        val delta = if (abs(denom) > 1e-10f) (y0 - y2) / (2f * denom) else 0f

        return sampleRate / (bestLag + delta.coerceIn(-1f, 1f))
    }

    /**
     * nsdf[tau] = 2 * acf(tau) / (energy_of_first_(N-tau)_samples +
     *                              energy_of_last_(N-tau)_samples)
     *           = 2 * acf(tau) / (2*m0 - mHead[tau] - mTail[tau])
     */
    private fun computeNsdf(signal: FloatArray, nsdf: FloatArray, minLag: Int, maxLag: Int) {
        val n = signal.size

        var m0 = 0f
        for (s in signal) m0 += s * s

        // Precompute cumulative head/tail energies once — O(maxLag).
        var mHead = 0f
        var mTail = 0f
        // mHead[lag] = sum_{t=0}^{lag-1} signal[t]^2
        // mTail[lag] = sum_{t=n-lag}^{n-1} signal[t]^2
        val headArr = FloatArray(maxLag + 1)
        val tailArr = FloatArray(maxLag + 1)
        for (lag in 1..maxLag) {
            mHead += signal[lag - 1] * signal[lag - 1]
            mTail += signal[n - lag] * signal[n - lag]
            headArr[lag] = mHead
            tailArr[lag] = mTail
        }

        for (lag in minLag..maxLag) {
            var acf = 0f
            for (i in 0 until n - lag) acf += signal[i] * signal[i + lag]
            val denom = 2f * m0 - headArr[lag] - tailArr[lag]
            nsdf[lag] = if (denom > 1e-8f) 2f * acf / denom else 0f
        }
    }
}
