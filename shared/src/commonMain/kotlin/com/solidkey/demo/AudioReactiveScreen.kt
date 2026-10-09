package com.solidkey.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.audio.reactive.OGAudioReactives
import com.solidkey.painpoints.audio.reactive.OGSyntheticAudioSource
import com.solidkey.painpoints.audio.reactive.ogAudioReactive
import com.solidkey.painpoints.audio.reactive.rememberOGAudioReactive
import com.solidkey.painpoints.fx.ogGlow

private val BASS = Color(0xFFFF5252)
private val MID = Color(0xFF69F0AE)
private val TREBLE = Color(0xFF40C4FF)
private val LEVEL = Color(0xFFE040FB)

/**
 * 🎵 Audio-reactive vectors — the live visualizer dogfood for com.solidkey.painpoints.audio.reactive.
 * A pure-Kotlin FFT turns PCM into bass/mid/treble/level + a beat flag; those drive a spectrum, a
 * pulsing orb (bass), a ring (treble) and a beat flash. The audio here is a deterministic synthetic
 * beat (no mic needed, so it is reproducible); on device you plug your own OGAudioSource.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AudioReactiveScreen() {
    var bpm by remember { mutableStateOf(120) }
    var profile by remember { mutableStateOf("snappy") }

    val source = remember(bpm) { OGSyntheticAudioSource(bpm = bpm.toFloat()) }
    val spec = remember(profile) { OGAudioReactives.preset(profile) ?: error("preset") }
    val bands = rememberOGAudioReactive(source, spec)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("🎵 Audio-reactive vectors · OGAudioAnalyzer + Modifier.ogAudioReactive")
        Caption(
            "A pure-Kotlin FFT folds PCM into four bands + a beat. Bind any band to any composable " +
                "(here: a spectrum, a bass-pulsing orb, a treble ring, a beat flash). 60fps, zero-dep.",
        )

        // Live spectrum — read straight from the bands State inside the draw phase (no recomposition).
        Box(Modifier.fillMaxWidth().height(170.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(Color(0xFF0B1018))) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                val b = bands.value
                val entries = listOf(b.bass, b.mid, b.treble, b.level)
                val colors = listOf(BASS, MID, TREBLE, LEVEL)
                val n = entries.size
                val gap = 18.dp.toPx()
                val barW = (size.width - gap * (n - 1)) / n
                val r = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                entries.forEachIndexed { i, v ->
                    val x = i * (barW + gap)
                    // track
                    drawRoundRect(Color(0xFF1C2838), Offset(x, 0f), Size(barW, size.height), r)
                    // fill from the bottom
                    val h = size.height * v.coerceIn(0f, 1f)
                    drawRoundRect(colors[i], Offset(x, size.height - h), Size(barW, h), r)
                }
                // beat flash in the corner
                if (b.beat) drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(size.width - 6.dp.toPx(), 6.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BandKey("bass", BASS); BandKey("mid", MID); BandKey("treble", TREBLE); BandKey("level", LEVEL)
            Text("● beat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.8f))
        }

        // The reactive stage: a treble ring around a bass-pulsing, glowing orb.
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(Color(0xFF0B1018)),
            contentAlignment = Alignment.Center,
        ) {
            // Treble ring (reacts to the high band).
            Box(
                Modifier.size(180.dp)
                    .ogAudioReactive({ bands.value.treble }, scale = 1f..1.35f, alpha = 0.25f..0.7f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color.Transparent, TREBLE.copy(alpha = 0.0f), TREBLE.copy(alpha = 0.5f)))),
            )
            // Bass orb (reacts to the low band), with an additive glow halo.
            Box(
                Modifier.size(110.dp)
                    .ogAudioReactive({ bands.value.bass }, scale = 1f..1.5f)
                    .ogGlow(color = Color(0xFF7C4DFF), radius = 1.5f, intensity = 0.8f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFE1BEE7), Color(0xFF7C4DFF)))),
            )
        }

        // Controls — prove it is reacting (change the beat tempo and the response profile).
        SectionTitle("🎛️ Source & response")
        LabeledValue("tempo (synthetic)", "$bpm bpm")
        Slider(
            value = bpm.toFloat(), onValueChange = { bpm = it.toInt() },
            valueRange = 60f..180f, modifier = Modifier.fillMaxWidth(),
        )
        Caption("Response profile (attack/release smoothing):")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGAudioReactives.presets.keys.forEach { name ->
                FilterChip(profile == name, { profile = name }, { Text(name) })
            }
        }
        Caption(
            "The audio is a built-in deterministic OGSyntheticAudioSource. On device, feed real sound " +
                "by implementing OGAudioSource over a mic/music tap (Android Visualizer or AudioRecord, " +
                "iOS AVAudioEngine). See docs/AUDIO_REACTIVE.md.",
        )
    }
}

@Composable
private fun BandKey(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(" $label", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), modifier = Modifier.width(180.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}
