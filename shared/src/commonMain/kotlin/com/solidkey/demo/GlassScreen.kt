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
import com.solidkey.painpoints.glass.OGGlass
import com.solidkey.painpoints.glass.OGGlassSpec

/**
 * 🔍 **Look-behind-glass lens — OGGlass over any content.** A round, fixed, movable lens laid over a
 * block of fine print: a **magnifier** (zoom > 1), a **minifier** (zoom < 1) or **coloured glass** (a
 * tint). Drag the lens by its grip handle; its body is pointer-transparent so you keep interacting with
 * what's underneath. The content beneath is recorded once into a GraphicsLayer and redrawn scaled about
 * the lens centre, clipped to a circle, plus a tint + rim + specular sheen — GPU work only, no shader,
 * 60fps, same code on Android & iOS. An accessibility win as much as an effect.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassScreen() {
    var spec by remember { mutableStateOf(com.solidkey.painpoints.glass.OGGlass.MAGNIFIER) }
    var enabled by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassSectionTitle("🔍 Movable lens · OGGlass(spec) { content }")
        GlassCaption(
            "A round piece of glass over the text below. zoom > 1 magnifies, zoom < 1 minifies, tint " +
                "colours it. Drag the ● grip to move it; the glass body is pointer-transparent so the " +
                "content keeps responding. Every field is a plain knob and the spec serialises to a .glass " +
                "JSON a model can author (OGGlass.glassPrompt).",
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.solidkey.painpoints.glass.OGGlass.presets.forEach { (name, preset) ->
                FilterChip(spec.name == preset.name, { spec = preset }, { Text(name.replaceFirstChar { it.uppercase() }) })
            }
        }

        // A block of fine print under the glass so the magnification reads immediately.
        Box(
            Modifier.fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFF7F3E8), Color(0xFFEAE2CF)))),
        ) {
            OGGlass(spec = spec, modifier = Modifier.fillMaxSize(), enabled = enabled) {
                Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("The quick brown fox", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2A2420))
                    repeat(9) {
                        Text(
                            "Small print line $it — jumps over the lazy dog, 0123456789, legible under a lens.",
                            fontSize = 11.sp, color = Color(0xFF4A423A),
                        )
                    }
                    Text("K · M · P · M · E · D · I · A", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF7B2FF7))
                }
            }
        }

        GlassSlider("zoom", spec.zoom, 0.3f, 4f) { spec = spec.copy(zoom = it) }
        GlassSlider("radius", spec.radius, 0.08f, 0.5f) { spec = spec.copy(radius = it) }
        GlassSlider("tintStrength", spec.tintStrength, 0f, 1f) { spec = spec.copy(tintStrength = it) }
        GlassSlider("highlight", spec.highlight, 0f, 1f) { spec = spec.copy(highlight = it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(enabled, { enabled = it })
            Text(
                if (enabled) "  glass on" else "  glass off",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        GlassCaption(
            "Five presets: magnifier / reader / minifier / amber / rose. Flip zoom under 1 for a fit-more " +
                "minifier, or tint it for a colour-filter glass — older eyes get a movable magnifier over a " +
                "board, a photo or a word.",
        )
    }
}

@Composable
private fun GlassSectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun GlassCaption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}

@Composable
private fun GlassSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label  ${(value * 100).toInt() / 100f}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.width(150.dp),
        )
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}
