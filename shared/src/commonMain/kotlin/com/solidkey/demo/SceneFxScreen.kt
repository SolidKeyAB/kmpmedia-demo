package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGAiVector
import com.solidkey.painpoints.ai.OGColorMotion
import com.solidkey.painpoints.ai.OGFillSpec
import com.solidkey.painpoints.ai.OGGlowSpec
import com.solidkey.painpoints.ai.OGLookMotion
import com.solidkey.painpoints.ai.OGMotionOffset
import com.solidkey.painpoints.ai.OGPlacedShapeSpec
import com.solidkey.painpoints.ai.OGPointSpec
import com.solidkey.painpoints.ai.OGRegionMotion
import com.solidkey.painpoints.ai.OGScatterSpec
import com.solidkey.painpoints.ai.OGSceneMotion
import com.solidkey.painpoints.ai.OGSceneSpec
import com.solidkey.painpoints.ai.OGStopSpec
import com.solidkey.painpoints.ai.OGTextureSpec
import com.solidkey.painpoints.ai.animatedAt
import com.solidkey.painpoints.ai.drawOGGlow
import com.solidkey.painpoints.ai.drawOGTexture
import com.solidkey.painpoints.ai.expanded
import com.solidkey.painpoints.look.OGLookSpec
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🌄 **Richer stills (1.39.0) — dogfood.** Proves the new, optional `OGSceneSpec` paint fields
 * actually RENDER, not just compile. The hero is one little landscape drawn TWICE from the same
 * scene: flat fills only (what a plain trace gives — the "green-rectangle bush, flat-disc sun"
 * look) vs. the SAME scene with gradient + glow ([OGGlowSpec]) + texture ([OGTextureSpec]) +
 * scatter ([OGScatterSpec]). Every pixel goes through the REAL wire path — the scene is built, run
 * through `OGAiVector.encodeScene` → `decodeScene` (a JSON round-trip), then `expanded()` resolves
 * scatter — so this verifies the format carries the fields AND the library draws them. Same code
 * Android & iOS.
 */
@Composable
fun SceneFxScreen() {
    val bundle = remember { buildSceneFxBundle() }
    val scene = bundle.scene
    val treeCount = remember(scene) { scene.expanded().shapes.count { it.label == "trees" } }

    // (v4) ONE app clock in SECONDS feeds the scene's OWN motion timelines — the sun's rise/set
    // (OGRegionMotion) and the day→night swing (OGSceneMotion) — both DECLARED in the decoded JSON,
    // not in this file. A full reverse cycle is 2×3.4s; loop the clock over it. Reading it in the draw
    // lambdas makes the Canvases redraw each frame.
    val clock by rememberInfiniteTransition(label = "live").animateFloat(
        0f, 6.8f, infiniteRepeatable(tween(6800, easing = LinearEasing), RepeatMode.Restart), label = "clock",
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🌄 Richer stills → make it live", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "The same traced scene, drawn two ways — flat fills (top) vs. the 1.39 paint fields (bottom). And now " +
                "it MOVES: the sun's arc and the day→night swing are declared IN the scene JSON (1.40 motion), so " +
                "the scene animates itself — this screen just feeds it a clock.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        // ── HERO: the flat → painterly diff; the SUN rises, SETS behind the hill, and the sky swings
        // day↔night — ALL read from the scene's own motion JSON (1.40 make-it-live). The heavy
        // foreground is baked into a cached layer; only the sky wash, the moving sun and a veil redraw.
        AnimatedSunPanel("① Flat fills — a plain trace", scene, enriched = false, clock)
        AnimatedSunPanel("② 1.39 + 1.40 — ☀️🌙 sun arc + day→night, declared in the scene JSON", scene, enriched = true, clock)

        Text(
            "From JSON: ${scene.shapes.size} regions · ${bundle.jsonChars} chars · decoded " +
                "glow ${check(scene.has("sun") { it.glow != null })} texture ${check(scene.has("ground") { it.texture != null })} " +
                "scatter ${check(scene.has("trees") { it.scatter != null })} · " +
                "MOTION ${check(scene.has("sun") { it.motion != null } && scene.motion != null)} " +
                "(sun arc + day→night) · scatter → $treeCount trees",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(2.dp))
        Text("Each field on its own (flat → on)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        val sun = remember(scene) { scene.shapes.first { it.label == "sun" } }
        val ground = remember(scene) { scene.shapes.first { it.label == "ground" } }
        val tree = remember(scene) { scene.shapes.first { it.label == "trees" } }
        val treeField = remember(scene) { scene.expanded().shapes.filter { it.label == "trees" } }

        FxRow("✨ Glow — sun bloom", "flat disc", "+ halo") { fx -> renderOne(sun, size.width, size.height, fx) }
        FxRow("🌿 Texture — grass blades", "flat fill", "+ strokes") { fx -> renderOne(ground, size.width, size.height, fx) }
        FxRow("🌲 Scatter — one tree → a forest", "one tree", "+ field") { fx ->
            if (fx) treeField.forEach { renderOne(it, size.width, size.height, enriched = false) }
            else renderOne(tree, size.width, size.height, enriched = false)
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Every field is pure, deterministic and computed once at decode time (never per frame), so it stays " +
                "off the 60fps path and is byte-for-byte identical on both platforms.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

private fun check(b: Boolean) = if (b) "✓" else "✗"
private fun OGSceneSpec.has(label: String, pred: (OGPlacedShapeSpec) -> Boolean): Boolean =
    shapes.firstOrNull { it.label == label }?.let(pred) ?: false

/**
 * A hero panel that composites in LAYERS so the sun can actually SET behind the hill while the sky
 * shifts day↔night — all driven by one phase [sunT]:
 *   1. a day→night SKY gradient (redrawn per frame, cheap: one quad),
 *   2. the moving SUN region (+ its glow),
 *   3. the FOREGROUND (ground+texture+trees) rasterised ONCE into a cached bitmap with a transparent
 *      sky — drawn on top, so the descending sun disappears behind it,
 *   4. a night VEIL that dims the lit ground as the sun goes down.
 * Only layers 1,2,4 touch the GPU per frame; the heavy grass texture is baked into the cached layer.
 */
@Composable
private fun AnimatedSunPanel(label: String, scene: OGSceneSpec, enriched: Boolean, t: Float) {
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val fgBmp = remember(sizePx, enriched) {
        if (sizePx.width <= 0 || sizePx.height <= 0) null else rasterizeForeground(scene, enriched, sizePx)
    }
    val sun = remember(scene) { scene.shapes.firstOrNull { it.label == "sun" } }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.7f).clip(RoundedCornerShape(14.dp))
                .onSizeChanged { sizePx = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                // The day→night factor is the SCENE's own timeline phase (from the JSON), evaluated at t.
                val night = scene.motion?.phase(t) ?: 0f
                val w = size.width; val h = size.height
                // 1) sky — a day→night ramp (gradient for the painterly panel, a flat wash for the plain one)
                if (enriched) drawRect(brush = skyBrush(night, h)) else drawRect(color = flatSky(night))
                // 2) the sun region, moved by ITS OWN declared motion (animatedAt) — glow follows for free
                sun?.let { renderOne(it.animatedAt(t), w, h, enriched) }
                // 3) cached foreground on top → the sun sets BEHIND the hill
                fgBmp?.let { drawImage(it) }
                // 4) night veil over the lit ground
                if (night > 0.01f) drawRect(color = Color(0xFF0A1024).copy(alpha = night * 0.4f))
            }
        }
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f))
    }
}

/** Rasterise ONLY the foreground (ground+texture, trees) once — the sky + sun are drawn live, so the
 *  sky region of this bitmap stays transparent and the setting sun shows through behind the hill. */
private fun rasterizeForeground(scene: OGSceneSpec, enriched: Boolean, size: IntSize): ImageBitmap {
    val bmp = ImageBitmap(size.width, size.height)
    val canvas = GraphicsCanvas(bmp)
    val w = size.width.toFloat(); val h = size.height.toFloat()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w, h)) {
        val shapes = (if (enriched) scene.expanded().shapes else scene.shapes)
            .filter { it.label == "ground" || it.label == "trees" }
        for (s in shapes) renderOne(s, w, h, enriched)
    }
    return bmp
}

private fun lerpColor(a: Color, b: Color, t: Float) = Color(
    a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t,
)

/** Day→night sky gradient for the painterly panel ([night] 0..1). */
private fun skyBrush(night: Float, h: Float) = androidx.compose.ui.graphics.Brush.verticalGradient(
    0f to lerpColor(Color(0xFF4C7AB8), Color(0xFF0B1030), night),   // zenith
    0.6f to lerpColor(Color(0xFF9FC0E6), Color(0xFF1B2447), night), // mid
    1f to lerpColor(Color(0xFFFFD69A), Color(0xFF3A2A44), night),   // horizon glow → dusk
    startY = 0f, endY = h * 0.62f,
)

/** Day→night flat sky wash for the plain panel. */
private fun flatSky(night: Float) = lerpColor(Color(0xFF86B6E6), Color(0xFF141A3A), night)

@Composable
private fun FxRow(title: String, leftLabel: String, rightLabel: String, draw: DrawScope.(fx: Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FxCell(leftLabel, Modifier.weight(1f)) { draw(false) }
            FxCell(rightLabel, Modifier.weight(1f)) { draw(true) }
        }
    }
}

@Composable
private fun FxCell(label: String, modifier: Modifier, body: DrawScope.() -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF0E1420))) {
            Canvas(Modifier.fillMaxSize()) { body() }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

// --- Rendering (plain consumer code, exactly what an app would write) -----------------------

private fun parseHexColor(s: String?): Color {
    if (s == null || s.length < 7 || s[0] != '#') return Color(0xFF808080)
    return try {
        Color(s.substring(1, 3).toInt(16), s.substring(3, 5).toInt(16), s.substring(5, 7).toInt(16))
    } catch (e: Throwable) { Color(0xFF808080) }
}

/** Draw one region into [0,0]–(w,h). [enriched] gates gradient fill + glow-behind + texture-over. */
private fun DrawScope.renderOne(s: OGPlacedShapeSpec, w: Float, h: Float, enriched: Boolean) {
    val pts = s.points
    if (pts.size < 3) return
    val path = Path().apply {
        moveTo(pts[0].x * w, pts[0].y * h)
        for (i in 1 until pts.size) lineTo(pts[i].x * w, pts[i].y * h)
        close()
    }
    val alpha = s.opacity ?: 1f
    if (enriched) s.glow?.let { drawOGGlow(path, it) }         // glow sits UNDER the fill
    val g = s.gradient
    if (enriched && g != null && g.isRenderable) {
        val b = path.getBounds()
        drawPath(path, brush = g.toBrush(b.left, b.top, b.width, b.height), alpha = alpha)
    } else {
        drawPath(path, color = parseHexColor(s.fill), alpha = alpha)
    }
    if (enriched) s.texture?.let { tex ->                       // texture rides OVER the fill, clipped in
        val b = path.getBounds()
        clipPath(path) { drawOGTexture(tex, b.left, b.top, b.width, b.height) }
    }
}

// --- The demo scene (built once, then round-tripped through the codec) ----------------------

private class SceneFxBundle(val scene: OGSceneSpec, val jsonChars: Int)

private const val PI_F = 3.1415927f

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, n: Int = 32): List<OGPointSpec> =
    (0 until n).map { i ->
        val a = i / n.toFloat() * 2f * PI_F
        OGPointSpec(cx + rx * cos(a), cy + ry * sin(a))
    }

private fun rect(x0: Float, y0: Float, x1: Float, y1: Float): List<OGPointSpec> =
    listOf(OGPointSpec(x0, y0), OGPointSpec(x1, y0), OGPointSpec(x1, y1), OGPointSpec(x0, y1))

private fun buildSceneFxBundle(): SceneFxBundle {
    // A tiny landscape: sky, a low sun, ground, a tree-line. Each region carries BOTH a flat [fill]
    // (the plain-trace swatch) AND the richer 1.39 paint, so the same scene renders flat or painterly.
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
        points = ellipse(0.72f, 0.44f, 0.10f, 0.10f),
        label = "sun",
        fill = "#FFC24D",
        gradient = OGFillSpec(
            type = "radial", center = OGPointSpec(0.5f, 0.5f), radius = 0.5f,
            stops = listOf(OGStopSpec(0f, "#FFF6D0"), OGStopSpec(0.55f, "#FFC24D"), OGStopSpec(1f, "#FF8A2B")),
        ),
        // A soft warm bloom: lower intensity + more passes so it falls off smoothly (a halo, not a ring).
        glow = OGGlowSpec(color = "#FFCB63", radius = 1.7f, intensity = 0.5f, passes = 8),
        // (v4) MAKE IT LIVE: the sun's rise & set is declared here, in the scene data — it drifts from
        // high in the sky (centre 0.28) down to below the horizon (0.74), ping-ponging over 3.4s.
        motion = OGRegionMotion(
            duration = 3.4f, mode = "reverse", easing = "easeInOut",
            translate = OGMotionOffset(OGPointSpec(0f, -0.16f), OGPointSpec(0.06f, 0.30f)),
        ),
    )
    // Horizon at y=0.60 — the tree-line below stands on it.
    val ground = OGPlacedShapeSpec(
        points = rect(0f, 0.60f, 1f, 1f),
        label = "ground",
        fill = "#3E7A3A",
        gradient = OGFillSpec(
            type = "linear", from = OGPointSpec(0.5f, 0f), to = OGPointSpec(0.5f, 1f),
            stops = listOf(OGStopSpec(0f, "#5A9A4E"), OGStopSpec(1f, "#1F4A1C")),
        ),
        texture = OGTextureSpec(
            kind = "strokes", density = 44, length = 0.08f, angle = 90f, jitter = 0.7f,
            weight = 1.5f, amount = 0.85f, octaves = 4, seed = 7, color = "#C7F08A",
        ),
    )
    // One small pine (base at y=0.62, apex at 0.42). The scatter places its CENTROID in a narrow band
    // just below the horizon so every copy's base lands on the ground and its top pokes into the sky.
    val tree = OGPlacedShapeSpec(
        points = listOf(OGPointSpec(0.50f, 0.42f), OGPointSpec(0.545f, 0.62f), OGPointSpec(0.455f, 0.62f)),
        label = "trees",
        fill = "#1E5A2A",
        scatter = OGScatterSpec(count = 32, x0 = 0.03f, y0 = 0.57f, x1 = 0.97f, y1 = 0.63f, scaleMin = 0.5f, scaleMax = 1.0f, seed = 3),
    )

    val raw = OGSceneSpec(
        shapes = listOf(sky, sun, ground, tree), version = 4, background = "#101826",
        // (v4) MAKE IT LIVE: the whole-scene day→night swing is declared here too — one 3.4s timeline
        // (in sync with the sun) sliding the sky colour and a grade from day to deep night.
        motion = OGSceneMotion(
            duration = 3.4f, mode = "reverse", easing = "easeInOut",
            look = OGLookMotion(OGLookSpec.Identity, OGLookSpec(brightness = -0.42f, saturation = 0.7f, temperature = -0.22f)),
            background = OGColorMotion("#4C7AB8", "#0B1030"),
        ),
    )
    val json = OGAiVector.encodeScene(raw)
    val decoded = OGAiVector.decodeScene(json).sanitized()
    return SceneFxBundle(decoded, json.length)
}
