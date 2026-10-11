package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGColorMotion
import com.solidkey.painpoints.ai.OGFillSpec
import com.solidkey.painpoints.ai.OGLookMotion
import com.solidkey.painpoints.ai.OGMotionOffset
import com.solidkey.painpoints.ai.OGPlacedShapeSpec
import com.solidkey.painpoints.ai.OGPointSpec
import com.solidkey.painpoints.ai.OGRegionMotion
import com.solidkey.painpoints.ai.OGScatterSpec
import com.solidkey.painpoints.ai.OGSceneMotion
import com.solidkey.painpoints.ai.OGSceneSpec
import com.solidkey.painpoints.ai.OGStopSpec
import com.solidkey.painpoints.compositor.OGEasing
import com.solidkey.painpoints.fx.OGLightningSpec
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFileType
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.look.OGBlendMode
import com.solidkey.painpoints.look.OGLookSpec
import com.solidkey.painpoints.particle.OGParticles
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.stage.OGStage
import com.solidkey.painpoints.stage.OGStageView
import com.solidkey.painpoints.stage.OGStages
import com.solidkey.painpoints.stage.exportGif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🎞️ **OGStage — one JSON, a whole movie + "describe → movie".** The library already had both halves
 * of an animation engine; this screen proves they are now ONE thing — and that a language model can
 * author the whole thing. Tap a one-line prompt ([SHOWCASE_MOVIES]) to play a movie a model wrote from
 * it (decoded from its raw JSON reply, unchanged), or watch the built-in day→night movie. A single
 * [OGStage] document —
 * authored here, then round-tripped through [OGStages.encode] → [OGStages.decode] (the real wire
 * path a model would use) — stacks a model-authorable day→night **scene** layer (its sun arcs and
 * sets, the frame grades day→dusk→night, all declared in the scene's own JSON motion) and a
 * **particle** layer of drifting petals (blend `plus`) on ONE timeline, with a slow push-in keyframed
 * on the scene. [OGStageView] plays the decoded movie at 60fps and scrubs it (the scene + transforms
 * are closed-form t-addressable). Same code on Android & iOS.
 */
@Composable
fun StageScreen() {
    // The built-in movie, authored once. It is encoded to JSON and decoded back before playing, so
    // what the player renders is literally what came off the wire — exactly what a model's reply does.
    val builtIn = remember { OGStages.decode(OGStages.encode(buildMovie())) }

    // "Describe → movie": pick a one-line prompt and play the movie a model wrote from it (decoded from
    // its raw JSON reply, unchanged). null = the built-in day→night movie.
    var selected by remember { mutableStateOf<ShowcaseMovie?>(null) }
    val stage = remember(selected) { selected?.let { OGStages.decode(it.json) } ?: builtIn }
    val jsonChars = remember(stage) { OGStages.encode(stage).length }

    var scrubbing by remember { mutableStateOf(false) }
    var scrubT by remember { mutableStateOf(0f) } // 0..1 of the timeline
    val positionMs = if (scrubbing) (scrubT * stage.durationMs).toLong() else null

    // A down-fps / down-sized copy for a quick on-device export (generate → play → EXPORT).
    val exportStage = remember(stage) { stage.copy(width = 640, height = 360, fps = 12) }
    val gifSaver = rememberGifSaver()
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportInfo by remember { mutableStateOf<String?>(null) }
    var exportFile by remember { mutableStateOf<String?>(null) }
    var exportNonce by remember { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "🎞️ OGStage — one JSON, a whole movie",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "A single OGStage document = a timeline of layers. One generator emits it, one player plays it. " +
                "Tap a one-line prompt below to watch a movie a language model wrote from it — scene + " +
                "particles + action-FX + a styled-text title, decoded from its raw JSON reply and played unchanged.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        // ── Describe → movie: the prompt gallery ────────────────────────────────────────────────
        Text("🤖 Describe → playable movie", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PromptChip("🎞️ Built-in", selected == null) { selected = null; scrubbing = false; scrubT = 0f }
            for (movie in SHOWCASE_MOVIES) {
                PromptChip(movie.label, selected === movie) { selected = movie; scrubbing = false; scrubT = 0f }
            }
        }

        // The player. 16:9 "movie" frame; plays live, or shows the scrubbed instant.
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            OGStageView(
                stage = stage,
                modifier = Modifier.fillMaxSize(),
                isPlaying = true,
                loop = true,
                positionMs = positionMs,
            )
        }

        // Transport: tap to scrub (freezes & jumps to an exact t — proving t-addressability), or resume.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (scrubbing) "▶ Resume" else "⏸ Scrub",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { scrubbing = !scrubbing },
            )
            Slider(
                value = scrubT,
                onValueChange = { scrubbing = true; scrubT = it },
                modifier = Modifier.weight(1f),
            )
            Text("${(scrubT * stage.durationMs / 1000f * 10).toInt() / 10f}s", fontSize = 12.sp)
        }

        Text(
            "From JSON: ${stage.layers.size} layers · $jsonChars chars · decoded → played " +
                "(the real OGStages wire path) · live = ${check(stage.isLive)}",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(2.dp))
        val active = selected
        if (active == null) {
            Text("What's on the timeline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Bullet("① scene — a day→night landscape; sun arcs & sets, the frame grades day→dusk→night (scene's own motion), with a slow push-in (a scale keyframe track on the layer).")
            Bullet("② particles — cherry-blossom petals drifting down (OGParticles.PETALS), composited with blend \"plus\".")
            Bullet("③ action-FX — a forked lightning bolt (OGLightningView) striking into the hill, re-striking on a long gap; the unified player now plays the fx pack on the same timeline.")
        } else {
            Text("The one line a model turned into this movie", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
            ) {
                Text("“${active.prompt}”", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
            }
            Text(
                "Handed only OGStages.stagePrompt(…), the model emitted one JSON object; the app ran " +
                    "OGStages.decode on that raw reply and played it — no tools, no hand-editing. Scrub it, or " +
                    "export it to a GIF below.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }

        // ── Export: generate → play → EXPORT, the loop no Lottie/Rive runtime does ──────────────
        Spacer(Modifier.height(8.dp))
        Text("Export this movie (generate → play → export)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "The SAME OGStage, rendered off-screen and encoded to a shareable GIF on device — then played " +
                "back below through the platform's own GIF decoder (proof it's a real file). The end-to-end " +
                "loop no Lottie or Rive runtime does. Everything bakes in: the day→night scene, the petals in " +
                "their real shape, and the lightning bolt — the exported file matches the preview.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Button(
            onClick = {
                if (exporting) return@Button
                scope.launch {
                    exporting = true
                    exportInfo = "Rendering ${exportStage.frameCount} frames…"
                    yield() // let the "Rendering…" label paint before the one-shot encode
                    val bytes = withContext(Dispatchers.Default) { exportStage.exportGif() }
                    val name = "stage_export_$exportNonce"
                    val ok = gifSaver.save(name, bytes)
                    exportInfo = if (ok) {
                        exportFile = name
                        exportNonce++
                        "Exported ${exportStage.frameCount} frames · ${bytes.size / 1024} KB GIF · " +
                            "${exportStage.width}×${exportStage.height} @ ${exportStage.fps}fps"
                    } else {
                        "Save failed"
                    }
                    exporting = false
                }
            },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Exporting…" else "Export to animated GIF")
        }
        exportInfo?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        exportFile?.let { name ->
            Text("Played back through the platform GIF decoder:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.fillMaxWidth().background(Color(0xFF05060F)).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                OGImageView(
                    source = OGImageFileType(name, OGImageFormat.GIF),
                    displayShape = OGShapeType.SQUARE,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(260.dp),
                    onError = { exportInfo = "Playback error: $it" },
                    onEventTriggered = { _, _ -> },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Thin orchestration over shipped primitives: the scene (OGSceneSpec.liveAt), the keyframe tracks " +
                "(the compositor's OGKeyframedFloat) and the grade/blend (OGLookSpec/OGBlendMode) are reused as-is. " +
                "Scene + transforms are closed-form t-addressable, so the scrubber jumps to any instant; particles " +
                "run their own seeded sim. 60fps, same code on Android & iOS.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

private fun check(b: Boolean) = if (b) "✓" else "✗"

/** A pill that selects a describe→movie prompt (or the built-in movie). */
@Composable
private fun PromptChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
    }
}

// --- The movie, authored as data (then round-tripped through the JSON codec) ----------------

private const val PI_F = 3.1415927f

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, n: Int = 28): List<OGPointSpec> =
    (0 until n).map { i ->
        val a = i / n.toFloat() * 2f * PI_F
        OGPointSpec(cx + rx * cos(a), cy + ry * sin(a))
    }

private fun rect(x0: Float, y0: Float, x1: Float, y1: Float): List<OGPointSpec> =
    listOf(OGPointSpec(x0, y0), OGPointSpec(x1, y0), OGPointSpec(x1, y1), OGPointSpec(x0, y1))

/** The same little day→night landscape as the "Richer stills" screen, as a plain [OGSceneSpec]. Its
 *  sun arc (region motion) and day→night swing (scene motion) are declared in the data, not in code. */
private fun buildLandscapeScene(): OGSceneSpec {
    val sky = OGPlacedShapeSpec(
        points = rect(0f, 0f, 1f, 0.72f),
        label = "sky",
        fill = "#86B6E6",
        gradient = OGFillSpec(
            type = "linear", from = OGPointSpec(0.5f, 0f), to = OGPointSpec(0.5f, 1f),
            stops = listOf(OGStopSpec(0f, "#3E6FB0"), OGStopSpec(0.65f, "#9FC0E6"), OGStopSpec(1f, "#FFD69A")),
        ),
    )
    val sun = OGPlacedShapeSpec(
        points = ellipse(0.70f, 0.44f, 0.09f, 0.09f),
        label = "sun",
        fill = "#FFC24D",
        gradient = OGFillSpec(
            type = "radial", center = OGPointSpec(0.5f, 0.5f), radius = 0.5f,
            stops = listOf(OGStopSpec(0f, "#FFF6D0"), OGStopSpec(0.55f, "#FFC24D"), OGStopSpec(1f, "#FF8A2B")),
        ),
        // MAKE IT LIVE: the sun drifts from high (−0.16) down past the horizon (+0.30), ping-pong 3.4s.
        motion = OGRegionMotion(
            duration = 3.4f, mode = "reverse", easing = "easeInOut",
            translate = OGMotionOffset(OGPointSpec(0f, -0.16f), OGPointSpec(0.06f, 0.30f)),
        ),
    )
    // Drawn AFTER the sun, so the descending sun sets behind this hill.
    val ground = OGPlacedShapeSpec(
        points = rect(0f, 0.60f, 1f, 1f),
        label = "ground",
        fill = "#3E7A3A",
        gradient = OGFillSpec(
            type = "linear", from = OGPointSpec(0.5f, 0f), to = OGPointSpec(0.5f, 1f),
            stops = listOf(OGStopSpec(0f, "#5A9A4E"), OGStopSpec(1f, "#1F4A1C")),
        ),
    )
    val tree = OGPlacedShapeSpec(
        points = listOf(OGPointSpec(0.50f, 0.42f), OGPointSpec(0.545f, 0.62f), OGPointSpec(0.455f, 0.62f)),
        label = "trees",
        fill = "#1E5A2A",
        scatter = OGScatterSpec(count = 28, x0 = 0.03f, y0 = 0.57f, x1 = 0.97f, y1 = 0.63f, scaleMin = 0.5f, scaleMax = 1.0f, seed = 3),
    )
    return OGSceneSpec(
        shapes = listOf(sky, sun, ground, tree), version = 4, background = "#101826",
        // The whole-scene day→night swing (in sync with the sun): a grade + sky colour slide.
        motion = OGSceneMotion(
            duration = 3.4f, mode = "reverse", easing = "easeInOut",
            look = OGLookMotion(OGLookSpec.Identity, OGLookSpec(brightness = -0.42f, saturation = 0.7f, temperature = -0.22f)),
            background = OGColorMotion("#4C7AB8", "#0B1030"),
        ),
    )
}

/** Author the whole movie: a scene layer (with a push-in) + a petals layer, on one 6.8s timeline. */
private fun buildMovie(): OGStage = OGStage.of(
    OGStages.scene(
        id = "landscape",
        scene = buildLandscapeScene(),
        // A gentle push-in that returns, so it loops seamlessly with the 2×3.4s day→night cycle.
        scale = OGStages.eased(OGEasing.EASE_IN_OUT, 0L to 1.0f, 3400L to 1.07f, 6800L to 1.0f),
    ),
    OGStages.particles(
        id = "petals",
        spec = OGParticles.PETALS,
        blend = OGBlendMode.PLUS,
    ),
    // An action-FX layer on the SAME timeline — proves the unified player plays the fx pack too.
    // A forked bolt striking into the hill, re-striking with a long gap (a dusk storm accent).
    OGStages.lightning(
        id = "bolt",
        spec = OGLightningSpec(
            x1 = 0.64f, y1 = 0.02f, x2 = 0.58f, y2 = 0.58f,
            branches = 5, strikeMs = 80f, decayMs = 280f, gapMs = 1500f,
        ),
    ),
    width = 1600,
    height = 900,
    durationMs = 6800L,
)
