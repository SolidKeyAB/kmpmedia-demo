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
import com.solidkey.painpoints.puppet.OGPuppet
import com.solidkey.painpoints.puppet.OGPuppetSpec
import com.solidkey.painpoints.puppet.OGPuppetView

/**
 * 🪁 **Cut-out puppet — OGPuppetView data-defined raster rig.** A hand-drawn figure placed as its own
 * pixels and brought to life by the lib's pure, deterministic pose math (OGPuppetRig): a hovering float,
 * a breathing scale pulse, a lean + an airborne tilt, all keyed off one clock and a −1..1 **look** target.
 * The raster sibling of the particle / parametric data packs: a designer (or the Layer Cutter tool)
 * authors a `.puppet` pack of joints + motion and the library animates it — no per-app rigging code.
 * Same `tSec` + `look` → identical pose on Android & iOS; the pack round-trips through the JSON codec.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PuppetScreen() {
    // Authored once, then encoded → decoded so what plays is literally what came off the wire (the real
    // OGPuppet codec path). The base sprite references an app-bundled drawable (res/drawable → the loader).
    val spec = remember { OGPuppet.decode(OGPuppet.encode(buildHeroPuppet())) }

    var mode by remember { mutableStateOf("idle") } // idle = slow auto-glance, else a fixed look
    var look by remember { mutableStateOf(0f) }
    var playing by remember { mutableStateOf(true) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PuppetSectionTitle("🪁 Living cut-out · OGPuppetView(spec)")
        PuppetCaption(
            "The hand-drawn hero placed on screen as ITS OWN pixels, brought alive by the lib's pure pose " +
                "math: it hovers (a slow vertical float), breathes (a gentle scale pulse), leans toward a " +
                "look target and tilts as it banks. All deterministic from one clock — same inputs, same " +
                "pose on both platforms. Authored as a .puppet pack and round-tripped through the codec.",
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == "idle", { mode = "idle" }, { Text("Idle glance") })
            FilterChip(mode == "look", { mode = "look" }, { Text("Look (drive it)") })
        }

        // A deep sky backdrop so the flying figure reads; aspect matches the drawn frame (wide).
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                .background(Brush.radialGradient(listOf(Color(0xFF2B3A67), Color(0xFF0B1026)))),
            contentAlignment = Alignment.Center,
        ) {
            OGPuppetView(
                spec = spec,
                modifier = Modifier.fillMaxWidth().aspectRatio(spec.aspect),
                look = if (mode == "look") look else null,
                playing = playing,
            )
        }

        if (mode == "look") {
            PuppetSlider("look  (−1 left · +1 right)", look, -1f, 1f) { look = it }
        } else {
            PuppetCaption("Idle: the figure slowly glances left↔right on its own (OGPuppetRig.effectiveLook). Switch to \"Look\" to drive the gaze with the slider.")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(playing, { playing = it })
            Text(
                if (playing) "  playing" else "  paused",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        PuppetCaption(
            "This puppet is a single-part base (the whole clean flying drawing). Add jointed movers — a " +
                "fist, a cape, a lock of hair — each pinned at a 0..1 joint on the same frame, and they swing / " +
                "gaze / head-turn about those pins. The lib CONSUMES clean part frames; a person (or the Layer " +
                "Cutter tool) SEPARATES the art. 60fps, same code on Android & iOS.",
        )
    }
}

@Composable
private fun PuppetSectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun PuppetCaption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}

@Composable
private fun PuppetSlider(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label  ${(value * 100).toInt() / 100f}",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.width(180.dp),
        )
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}

/**
 * The hero puppet, authored as OGPuppet data (then round-tripped through the JSON codec). A flying
 * (ungrounded) figure: it hovers + breathes + leans + banks toward the look target. The base references
 * the bundled `superx_fig` drawable (1024×572 → aspect ≈ 1.79). No movers here — the whole clean drawing
 * is the body — so it reads honestly as "a drawing turned into a living, watchable cut-out".
 */
private fun buildHeroPuppet(): OGPuppetSpec = OGPuppetSpec(
    name = "super-x",
    base = "superx_fig",
    aspect = 1024f / 572f,
    grounded = false,
    breatheAmp = 0.025f,
    breatheSpeed = 0.6f,
    leanAmp = 0.08f,
    hoverAmp = 0.05f,
    tiltDeg = 2.5f,
)
