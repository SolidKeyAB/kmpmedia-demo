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
import com.solidkey.painpoints.look.OGBlendMode
import com.solidkey.painpoints.look.OGLookSpec
import com.solidkey.painpoints.look.OGLooks
import com.solidkey.painpoints.look.ogLook

/**
 * 🎨 Live looks — a data-defined colour grade (OGLookSpec → ColorFilter) applied to any graphic with
 * Modifier.ogLook, plus blend modes. Dogfoods com.solidkey.painpoints.look: one GPU colour op, 60fps,
 * frame-identical on Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LooksScreen() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GradeSection()
        BlendSection()
    }
}

/** Preset chips + live sliders grading one colourful subject through Modifier.ogLook. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GradeSection() {
    var spec by remember { mutableStateOf(OGLookSpec.Identity) }
    val presets = remember {
        listOf<Pair<String, OGLookSpec>>("original" to OGLookSpec.Identity) +
            OGLooks.presets.toList()
    }

    SectionTitle("🎨 Colour grade · OGLookSpec + Modifier.ogLook")
    Caption(
        "A \"look\" is plain data (brightness / contrast / saturation / temperature / tint / hue) " +
            "compiled once into one ColorFilter and applied to ANY graphic. Pick a preset, then fine-tune.",
    )

    // The graded subject.
    Box(
        Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(14.dp)).ogLook(spec),
        contentAlignment = Alignment.Center,
    ) {
        Subject()
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { (name, preset) ->
            val selected = spec == preset
            FilterChip(selected, { spec = preset }, { Text(name) })
        }
    }

    // Live knobs — prove the grade is just data.
    LabeledSlider("brightness", spec.brightness, -0.3f, 0.3f) { spec = spec.copy(brightness = it) }
    LabeledSlider("contrast", spec.contrast, 0.5f, 1.8f) { spec = spec.copy(contrast = it) }
    LabeledSlider("saturation", spec.saturation, 0f, 2f) { spec = spec.copy(saturation = it) }
    LabeledSlider("temperature", spec.temperature, -1f, 1f) { spec = spec.copy(temperature = it) }

    Caption(
        "ColorMatrix is a GPU colour op: zero per-frame allocation, 60fps, pixel-identical on Android " +
            "& iOS. The same spec serializes to a .look JSON a model can author (OGLooks.lookPrompt).",
    )
}

/** Blend modes: a top disc composited over the subject through Modifier.ogLook(blend = …). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlendSection() {
    var blend by remember { mutableStateOf(OGBlendMode.SCREEN) }
    SectionTitle("🧪 Blend modes · OGBlendMode")
    Caption("The amber disc is composited over the gradient with the chosen blend mode.")

    Box(
        Modifier.fillMaxWidth().aspectRatio(1.8f).clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Subject() // the backdrop
        // Top layer blended over the backdrop.
        Box(
            Modifier.size(140.dp).clip(CircleShape)
                .ogLook(OGLookSpec.Identity, blend.toBlendMode())
                .background(Color(0xFFFFC107)),
        )
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OGBlendMode.entries.forEach { mode ->
            FilterChip(blend == mode, { blend = mode }, { Text(mode.name.lowercase()) })
        }
    }
}

/** A colourful target so grades and blends read clearly without needing a picked photo. */
@Composable
private fun Subject() {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(
                    Color(0xFF1A237E), Color(0xFF00BCD4), Color(0xFF8BC34A),
                    Color(0xFFFFEB3B), Color(0xFFFF5722),
                ),
            ),
        ),
    )
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
        androidx.compose.material3.Slider(
            value = value.coerceIn(min, max), onValueChange = onChange,
            valueRange = min..max, modifier = Modifier.weight(1f),
        )
    }
}
