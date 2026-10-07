package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.fx.OGSlashView
import com.solidkey.painpoints.fx.OGSlashes
import com.solidkey.painpoints.particle.OGParticleView
import com.solidkey.painpoints.particle.OGParticles

/** The three signature slash forms (chip label → preset name). */
private val SLASH_PRESETS = listOf(
    "🌊 Water" to "water",
    "🔥 Flame" to "flame",
    "⚡ Thunder" to "thunder",
)

/** A matching particle overlay (droplets / embers / sparks) that composes with each slash element. */
private val SLASH_PARTICLES: Map<String, String> = mapOf(
    "water" to """{"name":"droplets","x":0.45,"y":0.3,"spawnRadius":0.25,"emissionRate":40,"angleDeg":90,"spreadDeg":60,"speed":0.18,"gravityY":0.5,"drag":0.1,"startColor":"#E1F5FE","endColor":"#4FC3F7","startAlpha":0.9,"endAlpha":0,"startSize":0.007,"endSize":0.004,"maxParticles":140}""",
    "flame" to """{"name":"embers","x":0.5,"y":0.42,"spawnRadius":0.25,"emissionRate":55,"angleDeg":270,"spreadDeg":50,"speed":0.22,"gravityY":-0.06,"drag":0.2,"startColor":"#FFE082","endColor":"#FF6D00","endAlpha":0,"startSize":0.012,"endSize":0.003,"maxParticles":200}""",
    "thunder" to """{"name":"sparks","x":0.5,"y":0.46,"spawnRadius":0.1,"emissionRate":60,"angleDeg":270,"spreadDeg":360,"speed":0.5,"speedJitter":0.8,"gravityY":0.3,"drag":1.2,"startColor":"#FFFFFF","endColor":"#FFD600","endAlpha":0,"startSize":0.008,"endSize":0.002,"maxParticles":120}""",
)

/**
 * 🗡️ **Breathing-slash ribbons** — the signature anime-action effect from the new
 * `com.solidkey.painpoints.fx` package, dogfooded live.
 *
 * `OGSlashView` draws a glowing, tapered arc that reveals head → tail, then stays alive with a flowing
 * edge and a breathing width. One primitive wears three elements chosen by DATA: a **water** sweep, a
 * **flame** lick, a **thunder** bolt. The expensive centreline is sampled once; each frame only
 * rebuilds the tapered outline and fills it with a GPU gradient + an additive glow — so it holds 60fps
 * and runs frame-identically on Android & iOS. Pair it with an `OGParticleView` (droplets / embers /
 * sparks) for the full look.
 */
@Composable
fun SlashScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Breathing-slash ribbons", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "A glowing tapered arc that draws on along a path and stays alive — water, flame or thunder, " +
                "chosen by data. The whole slash is just a serializable spec, so a designer or a language " +
                "model can author one as JSON. Clears the real-app 60fps perf gate.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        PresetSection()

        Spacer(Modifier.height(4.dp))
        JsonSection()
    }
}

// ── Presets + live knobs ───────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetSection() {
    var presetName by remember { mutableStateOf("water") }
    var playing by remember { mutableStateOf(true) }
    var withParticles by remember { mutableStateOf(true) }
    var replayKey by remember { mutableStateOf(0) }
    var width by remember { mutableStateOf(-1f) }   // -1 = "use the preset's value"
    var glow by remember { mutableStateOf(-1f) }
    var breathe by remember { mutableStateOf(-1f) }

    val base = remember(presetName) { OGSlashes.preset(presetName)!! }
    // On preset change, snap the sliders to that preset's values.
    val spec = remember(base, width, glow, breathe) {
        base.copy(
            width = if (width < 0f) base.width else width,
            glow = if (glow < 0f) base.glow else glow,
            breatheAmp = if (breathe < 0f) base.breatheAmp else breathe,
        )
    }
    val overlay = remember(presetName) { OGParticles.decodeSpecOrNull(SLASH_PARTICLES[presetName] ?: "") }

    Text("Signature presets · OGSlashView(spec)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SLASH_PRESETS.forEach { (label, name) ->
            FilterChip(
                selected = presetName == name,
                onClick = { presetName = name; width = -1f; glow = -1f; breathe = -1f; replayKey++ },
                label = { Text(label) },
            )
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).background(Color(0xFF07060D)),
        contentAlignment = Alignment.Center,
    ) {
        // key(replayKey, spec) restarts the draw-on on demand and whenever the spec changes.
        key(replayKey, presetName) {
            OGSlashView(spec = spec, modifier = Modifier.fillMaxSize(), playing = playing)
            if (withParticles && overlay != null) {
                OGParticleView(spec = overlay, modifier = Modifier.fillMaxSize(), playing = playing)
            }
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { replayKey++ }) { Text("↻ Replay") }
        FilterChip(selected = playing, onClick = { playing = !playing }, label = { Text(if (playing) "⏸ Pause" else "▶ Play") })
        FilterChip(selected = withParticles, onClick = { withParticles = !withParticles }, label = { Text("✨ Particles") })
    }

    Text("Width: ${(spec.width * 100).toInt()}% of short side", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Slider(value = spec.width, onValueChange = { width = it }, valueRange = 0.02f..0.2f)
    Text("Glow: ${(spec.glow * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Slider(value = spec.glow, onValueChange = { glow = it }, valueRange = 0f..1f)
    Text("Breathing: ${(spec.breatheAmp * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Slider(value = spec.breatheAmp, onValueChange = { breathe = it }, valueRange = 0f..0.4f)
}

// ── Slash from JSON ────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JsonSection() {
    Text("Slash from JSON · OGSlashes.decodeSpec(pack)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Text(
        "A slash is just a JSON object: a path plus an edge/colour/glow. Edit it and the library decodes " +
            "it to a live effect — the same path a model's reply takes (OGSlashes.slashPrompt → decodeSpec).",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
    val examples = remember {
        listOf(
            "Downstroke" to """{"name":"down","path":[{"x":0.3,"y":0.1},{"x":0.5,"y":0.45},{"x":0.62,"y":0.9}],"edge":"wave","colorStart":"#80D8FF","colorEnd":"#2962FF","glow":0.9,"width":0.1}""",
            "Crescent flame" to """{"name":"crescent","path":[{"x":0.1,"y":0.6},{"x":0.35,"y":0.35},{"x":0.65,"y":0.35},{"x":0.9,"y":0.6}],"edge":"rough","colorStart":"#FFEE58","colorEnd":"#E65100","glow":0.9,"edgeSpeed":7,"width":0.12}""",
            "Zig bolt" to """{"name":"zig","path":[{"x":0.15,"y":0.2},{"x":0.45,"y":0.4},{"x":0.3,"y":0.55},{"x":0.8,"y":0.85}],"edge":"bolt","colorStart":"#FFF9C4","colorEnd":"#FFD600","glowColor":"#FFFFFF","glow":1,"width":0.05,"edgeSpeed":8,"revealMs":160}""",
        )
    }
    var json by remember { mutableStateOf(examples[0].second) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEach { (label, pack) ->
            FilterChip(selected = json == pack, onClick = { json = pack }, label = { Text(label) })
        }
    }
    OutlinedTextField(
        value = json,
        onValueChange = { json = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("slash pack (JSON)") },
        textStyle = MaterialTheme.typography.bodySmall,
    )
    val liveSpec = remember(json) { OGSlashes.decodeSpecOrNull(json) }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.4f).background(Color(0xFF07060D)),
        contentAlignment = Alignment.Center,
    ) {
        if (liveSpec != null) {
            key(json) { OGSlashView(spec = liveSpec, modifier = Modifier.fillMaxSize()) }
        } else {
            Text("⚠️ invalid slash JSON", color = Color(0xFFFF8A80), fontSize = 12.sp)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            "The smooth centreline through the path is sampled ONCE (parse-once); each frame only rebuilds " +
                "the tapered outline (a cheap sine/noise pass) and fills it with a gradient brush + an " +
                "additive glow. Pure maths with an integer-hash noise — no RNG — so the same seed renders " +
                "frame-identically on Android & iOS. Zero new dependencies.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}
