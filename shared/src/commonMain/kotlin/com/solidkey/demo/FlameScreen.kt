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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.solidkey.painpoints.fx.OGFlame
import com.solidkey.painpoints.fx.OGFlameView

/**
 * 🔥 **Living fire — OGFlameView flickering flame.** A body of upward-licking tongues rooted along a
 * baseline, each a GPU-filled tapered path with a hot→cool gradient; their centrelines wave with fractal
 * noise and their heights flicker over time. No fluid sim, no per-pixel shader — pure + deterministic
 * from a seed, so it is frame-identical on Android & iOS and bakes into an export byte-for-byte. The fire
 * sibling of OGFog (smoke) and the particle EMBERS preset (sparks). Drive a live wind to bend the whole
 * flame downwind.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlameScreen() {
    var spec by remember { mutableStateOf(OGFlame.CAMPFIRE) }
    var playing by remember { mutableStateOf(true) }
    var wind by remember { mutableStateOf(0f) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlameSectionTitle("🔥 Flickering fire · OGFlameView(spec)")
        FlameCaption(
            "A handful of tapered tongues whose centrelines wave (fractal noise) and whose heights flicker " +
                "over time, filled with a hot→cool vertical gradient. The noise is sampled a few times per " +
                "tongue, never per pixel — no fluid sim, no shader. Deterministic from a seed → identical on " +
                "both platforms and in a baked export.",
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGFlame.presets.forEach { (name, preset) ->
                FilterChip(spec.name == preset.name, { spec = preset }, { Text(name.replaceFirstChar { it.uppercase() }) })
            }
        }

        // A dark night backdrop so the fire reads against it.
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF0A0710), Color(0xFF1E130A)))),
        ) {
            OGFlameView(spec, Modifier.fillMaxSize(), playing = playing, windOverride = wind)
        }

        FlameSlider("tongues", spec.tongues.toFloat(), 1f, 40f) { spec = spec.copy(tongues = it.toInt()) }
        FlameSlider("height", spec.height, 0.1f, 1.2f) { spec = spec.copy(height = it) }
        FlameSlider("flicker", spec.flicker, 0f, 1f) { spec = spec.copy(flicker = it) }
        FlameSlider("sway", spec.sway, 0f, 0.3f) { spec = spec.copy(sway = it) }
        FlameSlider("wind (live breeze)", wind, -0.6f, 0.6f) { wind = it }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(playing, { playing = it })
            Text(
                if (playing) "  burning" else "  frozen",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        FlameCaption(
            "Four presets: candle / campfire / torch / inferno. The wind slider overrides the spec's wind " +
                "live — drive it from the same breeze that pushes your snow/leaves and the fire reacts to the " +
                "weather. The whole fire serialises to a .flame JSON (OGFlame.flamePrompt).",
        )
    }
}

@Composable
private fun FlameSectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun FlameCaption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}

@Composable
private fun FlameSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label  ${(value * 100).toInt() / 100f}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.width(170.dp),
        )
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}
