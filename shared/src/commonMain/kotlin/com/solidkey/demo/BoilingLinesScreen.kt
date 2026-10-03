package com.solidkey.demo

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.shape.OGPoint
import com.solidkey.painpoints.shape.OGPolygonShape
import com.solidkey.painpoints.style.OGBoil
import com.solidkey.painpoints.style.OGStyles
import com.solidkey.painpoints.style.boiled
import com.solidkey.painpoints.style.pixelateFill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A circle sampled into [n] vertices (so the boil wiggles a smooth curve, not a coarse polygon). */
private fun circleOutline(n: Int, r: Float = 0.42f): List<OGPoint> =
    (0 until n).map { i ->
        val a = i.toFloat() / n * 2f * PI.toFloat()
        OGPoint(0.5f + r * cos(a), 0.5f + r * sin(a))
    }

/** A 5-point star outline (10 vertices) in normalized 0..1 space. */
private fun starOutline(): List<OGPoint> =
    (0 until 10).map { i ->
        val outer = i % 2 == 0
        val rr = if (outer) 0.46f else 0.2f
        val a = (-90f + i * 36f) * (PI.toFloat() / 180f)
        OGPoint(0.5f + rr * cos(a), 0.5f + rr * sin(a))
    }

private fun closedPath(points: List<OGPoint>, w: Float, h: Float): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].x * w, points[0].y * h)
    for (i in 1 until points.size) lineTo(points[i].x * w, points[i].y * h)
    close()
}

/**
 * 🖊️ **Boiling lines** — a prototype of a *moving* drawing style (the hand-drawn "living line" look).
 * One pure library primitive, `OGBoil`, jitters every vertex of an outline to a fresh pseudo-random
 * offset a few times a second. Drawn as a stroke it's a wiggly line; fed into a `clipShape` it's a
 * living, hand-cut media edge. Deterministic + zero-dependency, so it's a candidate for the core
 * `com.solidkey.painpoints.style` package. Same code on Android & iOS.
 */
@Composable
fun BoilingLinesScreen() {
    // A continuous, monotonically rising time (ms from screen open) driving the boil at ~60fps.
    val timeMs by produceState(0L) {
        var start = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { t ->
                if (start < 0) start = t
                value = t - start
            }
        }
    }

    var amplitude by remember { mutableStateOf(0.02f) }
    var boilFps by remember { mutableStateOf(8f) }
    var smooth by remember { mutableStateOf(true) }
    var pixelRes by remember { mutableStateOf(18f) }

    val boil = OGBoil(amplitude = amplitude, boilFps = boilFps, smooth = smooth)
    val blob = remember { circleOutline(56) }
    val star = remember { starOutline() }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Boiling lines", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "A moving style: OGBoil jitters each vertex of an outline a few times a second — the classic " +
                "hand-drawn 'living line'. Drawn as a stroke it's a wiggly line; dropped into a clipShape " +
                "it's a living media edge. Pure, deterministic, zero-dependency.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        // ── Wiggly STROKED lines (the literal "wiggly lines") ─────────────────────────
        Text("Wiggly strokes · boiled outline", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFFAF7FF)),
        ) {
            val w = size.width
            val h = size.height
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawPath(closedPath(boil.displace(blob, timeMs), w, h), color = Color(0xFF7B2FF7), style = stroke)
            drawPath(closedPath(boil.displace(star, timeMs), w, h), color = Color(0xFFFF5DA2), style = stroke)
        }

        // ── Same primitive as a FILLED living shape (boiled OGPolygonShape → clipShape) ──
        Text("Living fill · OGPolygonShape.boiled() in a clipShape slot", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(OGPolygonShape(blob).boiled(boil, timeMs))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF5DA2), Color(0xFF7B2FF7), Color(0xFF00C2A8)))),
            )
        }

        // ── Pixelated boil: boil → pixelateFill (the "op pipeline") = a shimmering 8-bit sprite ──
        Text("Pixelated · boil → pixelateFill (low-res sprite)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF14121A)),
        ) {
            val res = pixelRes.toInt().coerceAtLeast(2)
            val w = size.width
            val h = size.height
            // Pipeline: boil the outline, then rasterize the boiled shape into a coarse grid.
            val cells = pixelateFill(boil.displace(blob, timeMs), res)
            for (c in cells) {
                drawRect(
                    color = Color(0xFF00E0A8),
                    topLeft = Offset((c.x - 0.5f / res) * w, (c.y - 0.5f / res) * h),
                    size = Size(w / res * 0.9f, h / res * 0.9f),
                )
            }
        }
        Text("Pixel resolution: ${pixelRes.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = pixelRes, onValueChange = { pixelRes = it }, valueRange = 6f..36f)

        // ── Controls ──────────────────────────────────────────────────────────────────
        Text("Amplitude: ${(amplitude * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = amplitude, onValueChange = { amplitude = it }, valueRange = 0f..0.06f)
        Text("Boil speed: ${boilFps.toInt()} fps", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = boilFps, onValueChange = { boilFps = it }, valueRange = 1f..16f)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = smooth, onClick = { smooth = true }, label = { Text("Smooth wobble") })
            FilterChip(selected = !smooth, onClick = { smooth = false }, label = { Text("Classic stutter") })
        }

        // ── Data-defined style: decode a JSON "pack" and apply it live (designer / AI authored) ──
        Text("Style from JSON · OGStyles.decode(pack).apply(outline, t)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "A style is just data — a pipeline of {op, params}. Pick or edit the pack below and the " +
                "library decodes it to a live OGStyle, applied to the same circle every frame. This is how " +
                "a designer (or an LLM) ships a shareable '.style' with no code and no rebuild.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        val stylePresets = remember {
            listOf(
                "Boil" to """{"name":"boil","ops":[{"op":"boil","amplitude":0.03,"boilFps":9}]}""",
                "Stepped" to """{"name":"stepped","ops":[{"op":"boil","amplitude":0.04,"boilFps":7},{"op":"quantize","grid":22}]}""",
                "Pixel sprite" to """{"name":"pixel","ops":[{"op":"boil","amplitude":0.03,"boilFps":10},{"op":"pixelate","resolution":20}]}""",
            )
        }
        var styleJson by remember { mutableStateOf(stylePresets[2].second) }
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stylePresets.forEach { (label, json) ->
                FilterChip(selected = styleJson == json, onClick = { styleJson = json }, label = { Text(label) })
            }
        }
        OutlinedTextField(
            value = styleJson,
            onValueChange = { styleJson = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("style pack (JSON)") },
            textStyle = MaterialTheme.typography.bodySmall,
        )
        // Decode once per edit (not per frame); apply the compiled style to the outline each frame.
        val liveStyle = remember(styleJson) { OGStyles.decodeOrNull(styleJson) }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF14121A)),
        ) {
            val w = size.width
            val h = size.height
            val frame = liveStyle?.apply(blob, timeMs) ?: return@Canvas
            if (frame.isPixelated) {
                val ps = frame.pixelSize
                for (c in frame.pixels) {
                    drawRect(
                        color = Color(0xFF00E0A8),
                        topLeft = Offset((c.x - ps / 2f) * w, (c.y - ps / 2f) * h),
                        size = Size(w * ps * 0.9f, h * ps * 0.9f),
                    )
                }
            } else {
                val path = closedPath(frame.outline, w, h)
                drawPath(path, color = Color(0xFF7B2FF7).copy(alpha = 0.85f))
                drawPath(
                    path,
                    color = Color(0xFFFF5DA2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
        if (liveStyle == null) {
            Text("⚠️ invalid style JSON — fix it to see the preview", color = Color(0xFFFF8A80), fontSize = 12.sp)
        }

        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                "OGBoil.displace(points, timeMs) is a pure integer-hash offset per vertex (no RNG, no deps), " +
                    "so Android and iOS boil identically and an export matches the preview. The same call " +
                    "powers both views here: a stroked Path, and OGPolygonShape.boiled() fed to a clipShape — " +
                    "so a photo / GIF / video / live-camera clip gets a living hand-cut edge with no other " +
                    "change. Per-frame cost is just the offset + lerp, so it holds 60fps.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}
