package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.fx.OGFog
import com.solidkey.painpoints.fx.OGFogSpec
import com.solidkey.painpoints.fx.OGFogView

/**
 * 🌫️ Drifting fog / smoke / mist / steam — OGFogView fills its bounds with soft radial-gradient puffs
 * that drift + wander (fBm) and wrap toroidally for an endless seamless cloud. Dogfoods
 * com.solidkey.painpoints.fx.OGFog: no per-pixel noise, no runtime shader, 60fps, identical on Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FogScreen() {
    var spec by remember { mutableStateOf(OGFog.preset("mist") ?: OGFogSpec()) }
    var playing by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("🌫️ Drifting haze · OGFogView + OGFogSpec")
        Caption(
            "Soft radial-gradient puffs drift (linear) and wander (fractal noise), overlapping into a " +
                "cloudy mass that wraps toroidally for a seamless endless drift. Deterministic from a seed.",
        )

        // A night scene with a warm lamp so the haze reads against it.
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0E1630), Color(0xFF243055)))),
        ) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(28.dp).size(56.dp)
                    .clip(CircleShape).background(Color(0xFFFFC661)),
            )
            OGFogView(spec, Modifier.fillMaxSize(), playing = playing)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGFog.presets.forEach { (name, preset) ->
                FilterChip(spec.name == preset.name, { spec = preset }, { Text(name) })
            }
        }

        LabeledSlider("density", spec.density.toFloat(), 4f, 40f) { spec = spec.copy(density = it.toInt()) }
        LabeledSlider("opacity", spec.opacity, 0.1f, 0.9f) { spec = spec.copy(opacity = it) }
        LabeledSlider("scale", spec.scale, 0.2f, 1f) { spec = spec.copy(scale = it) }
        LabeledSlider("drift", spec.driftX, -0.1f, 0.1f) { spec = spec.copy(driftX = it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(playing, { playing = it })
            Text(
                if (playing) "  playing" else "  paused",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        Caption(
            "Every field — color / density / opacity / scale / drift / turbulence / wander / octaves / " +
                "softness / seed — is a plain knob, and the spec serializes to a .fog JSON a model can author " +
                "(OGFog.fogPrompt). Four presets: fog / smoke / mist / steam.",
        )
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

@Composable
private fun LabeledSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label  ${(value * 100).toInt() / 100f}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.width(150.dp),
        )
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}
