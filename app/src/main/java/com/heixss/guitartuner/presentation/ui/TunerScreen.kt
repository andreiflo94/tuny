package com.heixss.guitartuner.presentation.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heixss.guitartuner.domain.model.TuningResult
import com.heixss.guitartuner.presentation.TunerIntent
import com.heixss.guitartuner.presentation.TunerUiState
import com.heixss.guitartuner.presentation.TunerViewModel
import com.heixss.guitartuner.presentation.ui.components.StringSelector
import com.heixss.guitartuner.presentation.ui.components.TuningGauge
import com.heixss.guitartuner.ui.theme.TunerAccent
import com.heixss.guitartuner.ui.theme.TunerBackground
import com.heixss.guitartuner.ui.theme.TunerClose
import com.heixss.guitartuner.ui.theme.TunerInTune
import com.heixss.guitartuner.ui.theme.TunerOutOfTune
import com.heixss.guitartuner.ui.theme.TunerSurface
import com.heixss.guitartuner.ui.theme.TunerSurfaceAlt
import com.heixss.guitartuner.ui.theme.TunerTextPrimary
import com.heixss.guitartuner.ui.theme.TunerTextSecond
import kotlin.math.abs

@Composable
fun TunerScreen(vm: TunerViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.handleIntent(TunerIntent.StartTuning)
        else vm.handleIntent(TunerIntent.PermissionDenied)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F1020), TunerBackground),
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Title row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "TUNY",
                    color = TunerTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                Text(
                    text = "Standard",
                    color = TunerTextSecond,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Gauge
            TuningGauge(
                result = if (state.signalDetected) state.tuningResult else null,
                isListening = state.isListening,
            )

            Spacer(Modifier.height(8.dp))

            // Note display
            NoteDisplay(state)

            Spacer(Modifier.weight(1f))

            // String selector
            val autoDetected = if (state.signalDetected) state.tuningResult?.nearestGuitarString else null
            val lockedString = state.selectedString
            val selectorLabel = when {
                lockedString != null ->
                    "LOCKED  ·  ${lockedString.displayName}"
                autoDetected != null ->
                    "${ordinal(autoDetected.stringNumber)} STRING  ·  AUTO"
                else -> "STRING"
            }
            AnimatedContent(
                targetState = selectorLabel,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "string_label",
            ) { label ->
                Text(
                    text = label,
                    color = if (autoDetected != null || state.selectedString != null)
                        TunerAccent.copy(alpha = 0.9f) else TunerTextSecond,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))
            StringSelector(
                selected = state.selectedString,
                autoSelected = state.tuningResult?.nearestGuitarString,
                onSelect = { vm.handleIntent(TunerIntent.SelectString(it)) },
            )

            Spacer(Modifier.height(28.dp))

            // Start / Stop button
            TunerFab(
                isListening = state.isListening,
                onClick = {
                    if (state.isListening) vm.handleIntent(TunerIntent.StopTuning)
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
            )

            if (state.permissionDenied) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Microphone permission required",
                    color = TunerOutOfTune.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun NoteDisplay(state: TunerUiState) {
    val result = state.tuningResult
    val inTune = result != null && abs(result.centsDeviation) <= 5f

    val noteColor by animateColorAsState(
        targetValue = when {
            !state.signalDetected -> TunerTextSecond
            inTune                -> TunerInTune
            result != null && abs(result.centsDeviation) < 15f -> TunerClose
            else                  -> TunerOutOfTune
        },
        animationSpec = tween(300),
        label = "noteColor",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Note name + octave
        AnimatedContent(
            targetState = if (state.signalDetected) result?.let { "${it.noteName}${it.octave}" } ?: "--" else "--",
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "note",
        ) { noteText ->
            Text(
                text = buildAnnotatedString {
                    val note   = noteText.dropLast(1).takeIf { noteText.length > 1 } ?: noteText
                    val octave = noteText.last().takeIf { it.isDigit() }?.toString() ?: ""
                    append(note)
                    if (octave.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontSize = 28.sp,
                                baselineShift = BaselineShift.Superscript,
                                color = noteColor.copy(alpha = 0.7f),
                            )
                        ) { append(octave) }
                    }
                },
                color = noteColor,
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Cents deviation
        AnimatedContent(
            targetState = if (state.signalDetected && result != null) {
                val c = result.centsDeviation.toInt()
                when {
                    c == 0  -> "in tune"
                    c > 0   -> "+${c}¢ sharp"
                    else    -> "${c}¢ flat"
                }
            } else {
                if (state.isListening) "listening…" else "tap to tune"
            },
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
            label = "cents_label",
        ) { label ->
            Text(
                text = label,
                color = if (state.signalDetected) noteColor.copy(alpha = 0.8f) else TunerTextSecond,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Frequency + detected string on same row
        if (state.signalDetected && result != null) {
            val gs = result.nearestGuitarString
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "%.1f Hz".format(result.frequency),
                    color = TunerTextSecond,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                )
                if (gs != null) {
                    Text(text = "·", color = TunerTextSecond, fontSize = 12.sp)
                    Text(
                        text = "${ordinal(gs.stringNumber)} string",
                        color = TunerAccent.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun ordinal(n: Int) = when (n) {
    1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"; else -> "${n}th"
}

@Composable
private fun TunerFab(isListening: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.05f else 1f,
        animationSpec = tween(200),
        label = "fab_scale",
    )
    val bg = if (isListening) TunerAccent else TunerSurfaceAlt
    val borderGlow = if (isListening) TunerAccent.copy(alpha = 0.4f) else Color.Transparent

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            .size(64.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (isListening) Modifier.background(
                    Brush.radialGradient(
                        listOf(TunerAccent.copy(0.3f), Color.Transparent)
                    )
                ) else Modifier
            ),
    ) {
        IconButton(onClick = onClick) {
            Text(
                text = if (isListening) "■" else "●",
                color = Color.White,
                fontSize = if (isListening) 20.sp else 24.sp,
            )
        }
    }
}
