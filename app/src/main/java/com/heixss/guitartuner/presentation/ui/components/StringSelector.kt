package com.heixss.guitartuner.presentation.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heixss.guitartuner.domain.model.GuitarString
import com.heixss.guitartuner.ui.theme.TunerAccent
import com.heixss.guitartuner.ui.theme.TunerInTune
import com.heixss.guitartuner.ui.theme.TunerSurfaceAlt
import com.heixss.guitartuner.ui.theme.TunerTextSecond

@Composable
fun StringSelector(
    selected: GuitarString?,
    autoSelected: GuitarString?,
    onSelect: (GuitarString?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        GuitarString.entries.forEach { string ->
            val isManuallySelected = selected == string
            val isAutoHighlighted  = selected == null && autoSelected == string

            StringButton(
                label       = string.displayName.dropLast(1),
                active      = isManuallySelected,
                highlighted = isAutoHighlighted,
                onClick     = { onSelect(if (isManuallySelected) null else string) },
            )
        }
    }
}

@Composable
private fun StringButton(
    label: String,
    active: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    // Pulsing ring alpha — only animates when highlighted
    val infiniteTransition = rememberInfiniteTransition(label = "string_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue  = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    val ringAlpha  = when { active -> 1f; highlighted -> pulseAlpha; else -> 0f }
    val ringColor  = if (active) TunerAccent else TunerInTune
    val bg         = when { active -> TunerAccent; highlighted -> TunerAccent.copy(alpha = 0.20f); else -> TunerSurfaceAlt }
    val textColor  = when { active -> Color.White; highlighted -> TunerInTune; else -> TunerTextSecond }

    // Fixed outer wrapper (56dp) keeps Row layout stable regardless of ring state
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(56.dp),
    ) {
        // Outer glow ring (drawn below the button, never clips into it)
        if (ringAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .border(2.dp, ringColor.copy(alpha = ringAlpha), CircleShape),
            )
        }

        // Inner button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bg)
                .clickable(onClick = onClick),
        ) {
            Text(
                text       = label,
                color      = textColor,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
        }
    }
}
