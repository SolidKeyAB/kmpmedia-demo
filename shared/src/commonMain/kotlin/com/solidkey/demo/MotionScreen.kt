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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.solidkey.painpoints.motion.OGDrawOnStroke
import com.solidkey.painpoints.motion.OGMotionPath
import com.solidkey.painpoints.motion.OGMotions
import com.solidkey.painpoints.motion.OGStagger
import com.solidkey.painpoints.motion.ogEasingOf
import com.solidkey.painpoints.motion.ogMotionPath
import com.solidkey.painpoints.shape.OGParametric
import com.solidkey.painpoints.shape.OGPoint
import kotlin.math.cos
import kotlin.math.sin

private val GOLD = Color(0xFFFFC107)
private val STAGE = Color(0xFF0B1018)
private val TILE_COLORS = listOf(
    Color(0xFFFF5252), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF69F0AE),
    Color(0xFF40C4FF), Color(0xFF7C4DFF), Color(0xFFFF4081),
)

/**
 * ✍️ Motion design — the dogfood for com.solidkey.painpoints.motion. Three primitives:
 *  • draw-on: a star outline that draws itself on (OGDrawOnStroke, progress read inside Canvas),
 *  • motion path: an arrow gliding + turning along a smooth loop (Modifier.ogMotionPath, orient),
 *  • stagger: a row of tiles cascading in with offset timing (OGStagger + a preset picker).
 * All normalized 0..1, pure-maths core, 60fps, same code on Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionScreen() {
    // One breathing timeline for draw-on + stagger (reveals, then reverses = draw-off / cascade-out).
    val reveal by rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse),
    )
    // A separate continuous timeline for the orbit.
    val orbit by rememberInfiniteTransition().animateFloat(
        0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
    )

    var staggerName by remember { mutableStateOf("cascade") }

    // A five-point star outline (normalized 0..1), closed into a loop so it draws on and joins up.
    val star = remember { OGMotionPath.loop(OGParametric.star(points = 5, innerRatio = 0.45f), smoothing = 0f) }
    // A smooth inset oval the arrow orbits along.
    val track = remember { OGMotionPath.loop(ellipse(cx = 0.5f, cy = 0.5f, rx = 0.36f, ry = 0.3f, n = 32), smoothing = 1f) }

    var trackBox by remember { mutableStateOf(Size.Zero) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("✍️ Draw-on · OGDrawOnStroke")
        Caption("A path reveals itself by arc length as progress goes 0 → 1 (then back). The progress is read inside the Canvas draw, so only this layer repaints.")
        Box(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp)).background(STAGE), contentAlignment = Alignment.Center) {
            OGDrawOnStroke(
                path = star,
                progress = { reveal },
                color = GOLD,
                strokeWidth = 7f,
                modifier = Modifier.size(190.dp),
                cap = StrokeCap.Round,
            )
        }

        SectionTitle("🛤️ Motion path · Modifier.ogMotionPath")
        Caption("An element glides along a curve by distance (constant speed) and turns to face its travel (orient = true). The path is normalized 0..1 inside the container size.")
        Box(
            Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp)).background(STAGE)
                .onSizeChanged { trackBox = it.toSize() },
        ) {
            // Faint guide of the track.
            Canvas(Modifier.fillMaxSize()) {
                var prev = track.pointAt(0f)
                var u = 0.02f
                while (u <= 1f) {
                    val p = track.pointAt(u)
                    drawLine(
                        Color.White.copy(alpha = 0.12f),
                        androidx.compose.ui.geometry.Offset(prev.x * size.width, prev.y * size.height),
                        androidx.compose.ui.geometry.Offset(p.x * size.width, p.y * size.height),
                        strokeWidth = 2f,
                    )
                    prev = p
                    u += 0.02f
                }
            }
            // The travelling element: an arrow that faces its direction of motion.
            Box(
                Modifier.size(40.dp)
                    .ogMotionPath(track, { orbit }, trackBox, orient = true)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF7C4DFF)),
                contentAlignment = Alignment.Center,
            ) {
                Text("➤", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        SectionTitle("🎞️ Stagger · OGStagger.progressFor")
        Caption("One timeline → a per-element progress, so a group animates with offset timing. Pick a feel; tiles slide up + fade in on their own schedule.")
        val spec = remember(staggerName) { OGMotions.staggerPreset(staggerName) ?: error("preset") }
        Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(14.dp)).background(STAGE), contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val n = TILE_COLORS.size
                TILE_COLORS.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(30.dp, 74.dp)
                            .graphicsLayer {
                                val p = OGStagger.progressFor(i, n, reveal, spec.stagger, ogEasingOf(spec.easing), spec.reverse)
                                alpha = p.coerceIn(0f, 1f)
                                translationY = (1f - p) * 46.dp.toPx()
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(c),
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGMotions.staggerPresets.keys.forEach { name ->
                FilterChip(staggerName == name, { staggerName = name }, { Text(name) })
            }
        }
        Caption("stagger=${fmt(spec.stagger)} · easing=${spec.easing}${if (spec.reverse) " · reverse" else ""}. Presets are plain OGStaggerSpec data (OGMotions). See docs/MOTION.md.")
    }
}

// An ellipse as normalized points (no duplicate end; loop() closes it).
private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, n: Int): List<OGPoint> {
    val twoPi = 6.2831855f
    return List(n) { i ->
        val a = twoPi * i / n
        OGPoint(cx + rx * cos(a), cy + ry * sin(a))
    }
}

private fun fmt(v: Float): String {
    val r = (v * 100f).toInt()
    return "0.${r.toString().padStart(2, '0')}"
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}
