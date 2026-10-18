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
import com.solidkey.painpoints.light.OGLight
import com.solidkey.painpoints.light.OGShadowSpec
import com.solidkey.painpoints.light.OGSpotSpec
import com.solidkey.painpoints.light.ogLight
import com.solidkey.painpoints.light.ogSheen
import com.solidkey.painpoints.light.ogShadow
import com.solidkey.painpoints.light.ogSpot

/**
 * 💡 OGLight — data-defined light, shade, sheen & spot on any shape. A directional form-shade
 * (ogLight), a drop/contact shadow (ogShadow), a sweeping gloss (ogSheen) and a radial spotlight
 * (ogSpot), each a serializable spec. Dogfoods com.solidkey.painpoints.light: no shader, 60fps, Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LightScreen() {
    var light by remember { mutableStateOf(OGLight.SCULPT) }
    var shadow by remember { mutableStateOf<OGShadowSpec>(OGLight.DROP) }
    var spot by remember { mutableStateOf<OGSpotSpec>(OGLight.VIGNETTE) }
    var sheenOn by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("💡 Light / shade / sheen / spot · Modifier.ogLight …")
        Caption(
            "A flat fill reads as paper. These four modifiers add cheap, shape-aware lighting to the same " +
                "neutral disc: a directional form-shade, a lifted drop shadow, a sweeping gloss, and a scene spotlight.",
        )

        Box(
            Modifier.fillMaxWidth().aspectRatio(1.3f).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2F3A), Color(0xFF14161C))))
                .ogSpot(spot),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth(0.5f).aspectRatio(1f)
                    .ogShadow(shadow, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFFB7BCC6))
                    .ogLight(light, CircleShape)
                    .then(if (sheenOn) Modifier.ogSheen(OGLight.GLINT, CircleShape) else Modifier),
            )
        }

        ChipRow("Light", OGLight.lights.keys.toList(), light.name) { light = OGLight.lights.getValue(it) }
        ChipRow("Shadow", OGLight.shadows.keys.toList(), shadow.name) { shadow = OGLight.shadows.getValue(it) }
        ChipRow("Spot", OGLight.spots.keys.toList(), spot.name) { spot = OGLight.spots.getValue(it) }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(sheenOn, { sheenOn = it })
            Text("  animated sheen (glint)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
        }

        LabeledSlider("light angle°", light.angleDeg, -180f, 180f) { light = light.copy(angleDeg = it) }
        LabeledSlider("light intensity", light.intensity, 0f, 1f) { light = light.copy(intensity = it) }
        LabeledSlider("ambient", light.ambient, 0f, 1f) { light = light.copy(ambient = it) }

        Caption(
            "Every spec is @Serializable with a JSON codec (OGLight.decodeLight / decodeShadow / decodeSheen " +
                "/ decodeSpot), so a model can author lighting like any other pack. Pure commonMain, no runtime shader.",
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(title: String, names: List<String>, selected: String?, onPick: (String) -> Unit) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEach { n ->
            FilterChip(selected == n, { onPick(n) }, { Text(n) })
        }
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
