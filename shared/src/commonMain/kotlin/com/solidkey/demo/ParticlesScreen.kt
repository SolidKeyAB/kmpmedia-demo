package com.solidkey.demo

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.particle.OGParticleSpec
import com.solidkey.painpoints.particle.OGParticleView
import com.solidkey.painpoints.particle.OGParticles
import com.solidkey.painpoints.shape.OGParametricSpec
import com.solidkey.painpoints.shape.OGParametrics

/** The parametric shape families the demo exposes (chip label → spec `kind`). */
private val PARAMETRIC_KINDS = listOf(
    "Polygon" to "polygon",
    "Star" to "star",
    "Gear" to "gear",
    "Flower" to "flower",
    "Squircle" to "superellipse",
    "Blob" to "blob",
)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * Build a parametric spec from the demo's two generic knobs ([count] + a normalized [detail] mapped
 * to each kind's natural parameter), so one slider drives star spikiness, gear depth, flower depth,
 * squircle roundness or blob lumpiness depending on the chosen [kind].
 */
private fun buildParametricSpec(kind: String, count: Int, detail: Float, seed: Int): OGParametricSpec =
    when (kind) {
        "star" -> OGParametricSpec(kind = "star", count = count, innerRatio = lerp(0.12f, 0.9f, detail))
        "gear" -> OGParametricSpec(kind = "gear", count = count, depth = lerp(0.12f, 0.8f, detail))
        "flower" -> OGParametricSpec(kind = "flower", count = count, depth = lerp(0.2f, 0.95f, detail))
        "superellipse" -> OGParametricSpec(kind = "superellipse", exponent = lerp(0.6f, 10f, detail))
        "blob" -> OGParametricSpec(kind = "blob", count = count, irregularity = lerp(0.2f, 0.95f, detail), seed = seed)
        else -> OGParametricSpec(kind = "polygon", count = count)
    }

/**
 * 🎆 **Particles & parametric shapes** — two data-defined generators from the
 * `com.solidkey.painpoints.particle` / `.shape` packages, dogfooded live.
 *
 * *Particles*: a `.particles` pack (plain serializable data — rate, lifetime, velocity cone, gravity,
 * size/colour-over-life, shape) that `OGParticleView` compiles once and simulates on the frame clock
 * — a fixed-capacity struct-of-arrays, no per-frame allocation, hard cap, deterministic from a seed →
 * 60fps and frame-identical on Android & iOS. Six built-in presets, plus a live JSON editor (how a
 * designer or an LLM ships an effect with no code and no rebuild).
 *
 * *Parametric shapes*: pure generators (`polygon`/`star`/`gear`/`flower`/`superellipse`/`blob`) that
 * turn a handful of numbers into a closed outline, wrapped in an `OGPolygonShape` → any `clipShape`.
 * Generated once, spun on the GPU, so it's free on the 60fps clip path.
 */
@Composable
fun ParticlesScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Particles & parametric shapes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Two data-defined generators. A particle effect and a parametric shape are each plain " +
                "serializable data — so a designer or a language model can author one as JSON, and the " +
                "library brings it to life. Both clear the real-app 60fps perf gate.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        ParticlesSection()

        Spacer(Modifier.height(4.dp))
        ParametricSection()
    }
}

// ── Particles ────────────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticlesSection() {
    val presetNames = remember { OGParticles.presets.keys.toList() } // confetti, sparks, snow, bokeh, rain, fireworks
    var presetName by remember { mutableStateOf(presetNames.first()) }
    var playing by remember { mutableStateOf(true) }
    var replayKey by remember { mutableStateOf(0) }

    val preset = remember(presetName) { OGParticles.preset(presetName)!! }

    Text("Particle presets · OGParticleView(spec)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        presetNames.forEach { name ->
            FilterChip(
                selected = presetName == name,
                onClick = { presetName = name; replayKey++ },
                label = { Text(name.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF0C0A14)),
        contentAlignment = Alignment.Center,
    ) {
        // key(replayKey) forces a fresh system so a one-shot burst (confetti / sparks / fireworks)
        // re-fires on demand; steady emitters (snow / rain / bokeh) just restart.
        key(replayKey, presetName) {
            OGParticleView(spec = preset, modifier = Modifier.fillMaxSize(), playing = playing)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { replayKey++ }) { Text("↻ Replay") }
        FilterChip(selected = playing, onClick = { playing = !playing }, label = { Text(if (playing) "⏸ Pause" else "▶ Play") })
    }

    // ── Data-defined particles: author/edit a JSON pack and preview it live ──
    Spacer(Modifier.height(6.dp))
    Text("Particles from JSON · OGParticles.decodeSpec(pack)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    Text(
        "A particle effect is just a JSON object. Edit the pack and the library decodes it to a live " +
            "emitter — the same path a model's reply takes (OGParticles.particlePrompt → decode).",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
    val particlePresets = remember {
        listOf(
            "Embers" to """{"name":"embers","emissionRate":45,"angleDeg":270,"spreadDeg":40,"speed":0.22,"gravityY":-0.05,"startColor":"#FFE082","endColor":"#FF6D00","endAlpha":0,"startSize":0.012,"endSize":0.004,"maxParticles":220}""",
            "Fountain" to """{"name":"fountain","emissionRate":120,"angleDeg":270,"spreadDeg":26,"speed":0.6,"gravityY":0.7,"drag":0.1,"startColor":"#4FC3F7","endColor":"#B388FF","endAlpha":0,"startSize":0.01,"endSize":0.006,"maxParticles":400}""",
            "Starburst" to """{"name":"starburst","shape":"star","burst":120,"emissionRate":0,"angleDeg":270,"spreadDeg":360,"speed":0.5,"gravityY":0.1,"drag":0.9,"spinDeg":180,"startColor":"#FFEB3B","endColor":"#F50057","endAlpha":0,"lifetimeMs":1600}""",
        )
    }
    var particleJson by remember { mutableStateOf(particlePresets[0].second) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        particlePresets.forEach { (label, json) ->
            FilterChip(selected = particleJson == json, onClick = { particleJson = json }, label = { Text(label) })
        }
    }
    OutlinedTextField(
        value = particleJson,
        onValueChange = { particleJson = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("particle pack (JSON)") },
        textStyle = MaterialTheme.typography.bodySmall,
    )
    val liveSpec = remember(particleJson) { OGParticles.decodeSpecOrNull(particleJson) }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).background(Color(0xFF0C0A14)),
        contentAlignment = Alignment.Center,
    ) {
        if (liveSpec != null) {
            key(particleJson) { OGParticleView(spec = liveSpec, modifier = Modifier.fillMaxSize()) }
        } else {
            Text("⚠️ invalid particle JSON", color = Color(0xFFFF8A80), fontSize = 12.sp)
        }
    }
}

// ── Parametric shapes ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParametricSection() {
    var kind by remember { mutableStateOf("star") }
    var count by remember { mutableStateOf(6f) }
    var detail by remember { mutableStateOf(0.4f) }
    var smoothing by remember { mutableStateOf(0f) }
    var seed by remember { mutableStateOf(3) }
    var spin by remember { mutableStateOf(true) }

    val spec = buildParametricSpec(kind, count.toInt(), detail, seed)
    // Generate the outline ONCE per parameter change (never per frame); spin it on the GPU.
    val shape = remember(kind, count.toInt(), detail, smoothing, seed) { spec.toShape(smoothing) }

    val timeMs by produceState(0L) {
        var start = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { t ->
                if (start < 0) start = t
                value = t - start
            }
        }
    }
    val angle = if (spin) (timeMs / 24f) % 360f else 0f

    Text("Parametric shapes · OGParametricSpec.toShape() → clipShape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PARAMETRIC_KINDS.forEach { (label, k) ->
            FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(label) })
        }
    }
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF14121A)), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .rotate(angle)
                .clip(shape)
                .background(Brush.linearGradient(listOf(Color(0xFFFF5DA2), Color(0xFF7B2FF7), Color(0xFF00C2A8)))),
        )
    }

    if (kind != "superellipse") {
        Text("Count: ${count.toInt()}  (sides / points / teeth / petals / lobes)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = count, onValueChange = { count = it }, valueRange = 3f..16f, steps = 12)
    }
    if (kind != "polygon") {
        val detailLabel = when (kind) {
            "star" -> "Spikiness"
            "gear" -> "Tooth depth"
            "flower" -> "Petal depth"
            "superellipse" -> "Roundness (circle → squircle → star)"
            "blob" -> "Lumpiness"
            else -> "Detail"
        }
        Text(detailLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = detail, onValueChange = { detail = it }, valueRange = 0f..1f)
    }
    Text("Corner smoothing (OGPolygonShape): ${(smoothing * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Slider(value = smoothing, onValueChange = { smoothing = it }, valueRange = 0f..1f)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = spin, onClick = { spin = !spin }, label = { Text(if (spin) "Spinning" else "Still") })
        if (kind == "blob") OutlinedButton(onClick = { seed++ }) { Text("🎲 New blob") }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            "This shape as JSON:  ${OGParametrics.encode(spec)}\n\n" +
                "The outline is generated once from the spec (pure maths, same result on every platform & " +
                "in export), wrapped in an OGPolygonShape and dropped into a clipShape — the identical slot " +
                "that clips a photo / GIF / video. The spin here is one GPU graphicsLayer, so the clip stays " +
                "free on the 60fps path.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}
