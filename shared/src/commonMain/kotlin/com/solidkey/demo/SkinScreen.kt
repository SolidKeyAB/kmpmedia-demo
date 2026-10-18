package com.solidkey.demo

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.skin.OGSkinSpec
import com.solidkey.painpoints.skin.OGSkins
import com.solidkey.painpoints.skin.ogSkin
import com.solidkey.painpoints.skin.ogSkinEdge

/**
 * 🐊 Procedural repeating skins — OGSkin grows a seamless, repeating material (reptile scales, animal
 * hide, woven fabric) and fills any shape with it via Modifier.ogSkin. Dogfoods
 * com.solidkey.painpoints.skin: one cached tile + a GPU ImageShader, no per-pixel work, 60fps, Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinScreen() {
    var spec by remember { mutableStateOf(OGSkins.preset("reptile") ?: OGSkinSpec()) }
    var circle by remember { mutableStateOf(false) }
    var edge by remember { mutableStateOf(false) }
    val shape: Shape = if (circle) CircleShape else RoundedCornerShape(20.dp)
    val edgeSpec = remember { OGSkins.CREST }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("🐊 Repeating skin · OGSkinSpec + Modifier.ogSkin")
        Caption(
            "A flat fill reads as plastic; a real hide has a pattern. One small seamless tile is hashed to " +
                "wrap edge-to-edge, then GPU-tiled across the shape. Pick a skin, swap the shape, add a raised edge.",
        )

        Box(
            Modifier.fillMaxWidth().aspectRatio(1.3f).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                    .ogSkin(spec, shape)
                    .then(if (edge) Modifier.ogSkinEdge(spec, shape, edgeSpec) else Modifier),
            )
        }

        SectionTitle("Skin")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGSkins.presets.forEach { (name, preset) ->
                FilterChip(spec.kind == preset.kind && spec.base == preset.base, { spec = preset }, { Text(name) })
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(circle, { circle = it })
            Text("  circle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            Switch(edge, { edge = it }, modifier = Modifier.padding(start = 20.dp))
            Text("  raised edge (crest)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
        }

        LabeledSlider("cell (pattern size)", spec.cell, 0.05f, 0.3f) { spec = spec.copy(cell = it) }
        LabeledSlider("relief", spec.relief, 0f, 1f) { spec = spec.copy(relief = it) }
        LabeledSlider("contrast", spec.contrast, 0f, 1f) { spec = spec.copy(contrast = it) }

        Caption(
            "Every field — kind / base / pattern / highlight / accent / cell / roughness / contrast / relief " +
                "/ seed — is a knob, and the spec serializes to a .skin JSON a model can author (OGSkins.skinPrompt). " +
                "Eight presets across reptile, hide and fabric families.",
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
