package com.heixss.guitartuner.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.heixss.guitartuner.domain.model.TuningResult
import com.heixss.guitartuner.ui.theme.TunerAccent
import com.heixss.guitartuner.ui.theme.TunerClose
import com.heixss.guitartuner.ui.theme.TunerGaugeBg
import com.heixss.guitartuner.ui.theme.TunerGaugeTick
import com.heixss.guitartuner.ui.theme.TunerInTune
import com.heixss.guitartuner.ui.theme.TunerOutOfTune
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Arc spans from 210° to 330° (120° sweep, centered on 270° = straight up)
private const val ARC_START = 210f
private const val ARC_SWEEP = 120f
private const val ARC_END   = ARC_START + ARC_SWEEP   // 330°

// Maps cents [-50..+50] to an angle within the arc
private fun centsToAngle(cents: Float): Float =
    ARC_START + (cents + 50f) / 100f * ARC_SWEEP

@Composable
fun TuningGauge(
    result: TuningResult?,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetCents = result?.centsDeviation ?: 0f
    val animatedCents by animateFloatAsState(
        targetValue = targetCents,
        // stiffness=500 → settles in ~170 ms; dampingRatio=0.8 avoids overshoot.
        // This makes the needle track real-time pitch changes without feeling jumpy.
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
        label = "needle",
    )

    val inTune = result != null && abs(result.centsDeviation) <= 5f

    // Idle pulse for the outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isListening && result == null) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.4f),
    ) {
        val cx = size.width / 2f
        val cy = size.height * 0.62f
        val r  = size.width * 0.38f

        drawGaugeBackground(cx, cy, r, pulseAlpha, isListening)
        drawColorZones(cx, cy, r)
        drawTicks(cx, cy, r)
        if (isListening) drawNeedle(cx, cy, r, animatedCents, inTune)
        if (inTune) drawInTuneRing(cx, cy, r)
    }
}

private fun DrawScope.drawGaugeBackground(
    cx: Float, cy: Float, r: Float,
    pulseAlpha: Float, isListening: Boolean,
) {
    val strokeW = 18.dp.toPx()
    val arcRect = Size(r * 2f, r * 2f)
    val arcTopLeft = Offset(cx - r, cy - r)

    // Outer glow ring (pulses when listening)
    if (isListening) {
        drawArc(
            color = TunerAccent.copy(alpha = pulseAlpha * 0.4f),
            startAngle = ARC_START,
            sweepAngle = ARC_SWEEP,
            useCenter = false,
            style = Stroke(width = strokeW * 2.2f, cap = StrokeCap.Round),
            topLeft = arcTopLeft,
            size = arcRect,
        )
    }

    // Background track
    drawArc(
        color = TunerGaugeBg,
        startAngle = ARC_START,
        sweepAngle = ARC_SWEEP,
        useCenter = false,
        style = Stroke(width = strokeW, cap = StrokeCap.Round),
        topLeft = arcTopLeft,
        size = arcRect,
    )
}

private fun DrawScope.drawColorZones(cx: Float, cy: Float, r: Float) {
    val strokeW = 18.dp.toPx()
    val arcRect = Size(r * 2f, r * 2f)
    val arcTopLeft = Offset(cx - r, cy - r)

    data class Zone(val fromCents: Float, val toCents: Float, val color: Color)
    val zones = listOf(
        Zone(-50f, -25f, TunerOutOfTune),
        Zone(-25f, -10f, TunerClose),
        Zone(-10f,  10f, TunerInTune),
        Zone( 10f,  25f, TunerClose),
        Zone( 25f,  50f, TunerOutOfTune),
    )

    for (zone in zones) {
        val start = centsToAngle(zone.fromCents)
        val sweep = centsToAngle(zone.toCents) - start
        drawArc(
            color = zone.color.copy(alpha = 0.35f),
            startAngle = start,
            sweepAngle = sweep,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Butt),
            topLeft = arcTopLeft,
            size = arcRect,
        )
    }
}

private fun DrawScope.drawTicks(cx: Float, cy: Float, r: Float) {
    val majorR  = r + 2.dp.toPx()
    val minorR  = r - 2.dp.toPx()
    val labelR  = r - 10.dp.toPx()
    val majorLen = 10.dp.toPx()
    val minorLen =  5.dp.toPx()

    for (cents in -50..50 step 10) {
        val angleDeg = centsToAngle(cents.toFloat())
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val isMajor = cents % 25 == 0
        val len = if (isMajor) majorLen else minorLen
        val outerR = majorR
        val innerR = outerR - len

        drawLine(
            color = if (isMajor) TunerTextForTick else TunerGaugeTick,
            start = Offset(cx + cosA * outerR, cy + sinA * outerR),
            end   = Offset(cx + cosA * innerR, cy + sinA * innerR),
            strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Center tick (0 cents)
    val rad0 = Math.toRadians(centsToAngle(0f).toDouble())
    drawLine(
        color = TunerInTune.copy(alpha = 0.9f),
        start = Offset(cx + cos(rad0).toFloat() * (majorR + 3.dp.toPx()),
                       cy + sin(rad0).toFloat() * (majorR + 3.dp.toPx())),
        end   = Offset(cx + cos(rad0).toFloat() * (majorR - 14.dp.toPx()),
                       cy + sin(rad0).toFloat() * (majorR - 14.dp.toPx())),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

private val TunerTextForTick = Color(0xFF5C6489)

private fun DrawScope.drawNeedle(
    cx: Float, cy: Float, r: Float,
    cents: Float, inTune: Boolean,
) {
    val needleAngleDeg = centsToAngle(cents.coerceIn(-50f, 50f))
    val rad = Math.toRadians(needleAngleDeg.toDouble())
    val cosA = cos(rad).toFloat()
    val sinA = sin(rad).toFloat()

    val needleColor = when {
        inTune                  -> TunerInTune
        abs(cents) < 15f        -> TunerClose
        else                    -> TunerOutOfTune
    }

    val needleLen = r * 0.82f
    val baseLen   = 18.dp.toPx()
    val tip   = Offset(cx + cosA * needleLen, cy + sinA * needleLen)
    val base  = Offset(cx - cosA * baseLen,   cy - sinA * baseLen)

    // Glow (thick, low alpha)
    drawLine(
        color = needleColor.copy(alpha = 0.18f),
        start = base, end = tip,
        strokeWidth = 14.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Mid glow
    drawLine(
        color = needleColor.copy(alpha = 0.35f),
        start = base, end = tip,
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Core
    drawLine(
        color = needleColor,
        start = base, end = tip,
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Pivot dot
    drawCircle(color = needleColor, radius = 5.dp.toPx(), center = Offset(cx, cy))
    drawCircle(color = needleColor.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(cx, cy))
}

private fun DrawScope.drawInTuneRing(cx: Float, cy: Float, r: Float) {
    // Thin full-circle glow when perfectly in tune
    drawCircle(
        color = TunerInTune.copy(alpha = 0.12f),
        radius = r + 6.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 12.dp.toPx()),
    )
    drawCircle(
        color = TunerInTune.copy(alpha = 0.55f),
        radius = r + 6.dp.toPx(),
        center = Offset(cx, cy),
        style = Stroke(width = 2.dp.toPx()),
    )
}
