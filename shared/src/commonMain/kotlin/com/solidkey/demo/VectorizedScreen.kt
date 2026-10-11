package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGAiVector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val RW = 1080
private const val RH = 602  // 1080 * 558/1000

private class VTri(val path: Path, val color: Color)

private fun parseHexColor(s: String?): Color {
    if (s == null || s.length < 7 || s[0] != '#') return Color(0xFF808080)
    return try {
        val r = s.substring(1, 3).toInt(16)
        val g = s.substring(3, 5).toInt(16)
        val b = s.substring(5, 7).toInt(16)
        Color(r, g, b)
    } catch (e: Throwable) { Color(0xFF808080) }
}

/** Decode the scene JSON (via the library codec) and build one Path per triangle, pre-scaled to RW×RH. */
private fun buildTriangles(json: String): List<VTri> {
    val scene = OGAiVector.decodeSceneOrNull(json) ?: return emptyList()
    val out = ArrayList<VTri>(scene.shapes.size)
    for (s in scene.shapes) {
        val pts = s.points
        if (pts.size < 3) continue
        val path = Path().apply {
            moveTo(pts[0].x * RW, pts[0].y * RH)
            for (i in 1 until pts.size) lineTo(pts[i].x * RW, pts[i].y * RH)
            close()
        }
        out.add(VTri(path, parseHexColor(s.fill)))
    }
    return out
}

/** Rasterize all triangles once into a cached ImageBitmap (heavy, done off the frame clock). */
private fun rasterize(tris: List<VTri>): ImageBitmap {
    val bmp = ImageBitmap(RW, RH)
    val canvas = GraphicsCanvas(bmp)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(RW.toFloat(), RH.toFloat())) {
        for (t in tris) drawPath(t.path, t.color)
    }
    return bmp
}

private val ID = floatArrayOf(1f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,1f,0f,0f, 0f,0f,0f,1f,0f)
private val NIGHT = floatArrayOf(
    0.42f,0.10f,0.06f,0f,0f,
    0.08f,0.44f,0.10f,0f,0f,
    0.12f,0.16f,0.66f,0f,14f,
    0f,0f,0f,1f,0f,
)
private fun gradeMatrix(t: Float): ColorMatrix =
    ColorMatrix(FloatArray(20) { ID[it] + (NIGHT[it] - ID[it]) * t })

@Composable
fun VectorizedScreen() {
    var dense by remember { mutableStateOf(true) }       // true = 20k, false = 11k
    var night by remember { mutableStateOf(false) }
    var shimmer by remember { mutableStateOf(false) }
    var bmp by remember { mutableStateOf<ImageBitmap?>(null) }
    var triCount by remember { mutableStateOf(0) }
    var buildMs by remember { mutableStateOf(0L) }

    // Build (decode + rasterize) off the main frame clock whenever the density changes.
    LaunchedEffect(dense) {
        bmp = null
        val json = if (dense) DemoAssets.vecLion20k else DemoAssets.vecLion11k
        if (json.isBlank()) return@LaunchedEffect
        val t0 = kotlin.time.TimeSource.Monotonic.markNow()
        val built = withContext(Dispatchers.Default) {
            val tris = buildTriangles(json)
            rasterize(tris) to tris.size
        }
        bmp = built.first
        triCount = built.second
        buildMs = t0.elapsedNow().inWholeMilliseconds
    }

    // Live animation clocks: a day↔night transition, a sweeping shimmer, a slow breathe.
    val nightP by animateFloatAsState(if (night) 1f else 0f, animationSpec = tween(1400), label = "night")
    val trans = rememberInfiniteTransition(label = "live")
    val sheen by trans.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "sheen")

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Photo → vector, cranked", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "The lion traced into a dense triangle mesh — every triangle is one OGSceneSpec polygon+fill, decoded by the library. Realistic AND still live: re-theme day↔night and it shimmers, drawn from a cached layer so it holds 60fps.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Box(
            Modifier.fillMaxWidth().aspectRatio(RW.toFloat() / RH)
                .clip(RoundedCornerShape(14.dp))
                .background(if (nightP > 0.5f) Color(0xFF0E1430) else Color(0xFF2A2238)),
            contentAlignment = Alignment.Center,
        ) {
            val image = bmp
            if (image == null) {
                Text("Vectorizing ${if (dense) "~20k" else "~11k"} triangles…", color = Color.White)
            } else {
                // Static mesh layer: drawn once, re-graded only when day/night changes; breathe is a
                // cheap GPU transform (no per-frame bitmap resample).
                // Static mesh layer: a cached GPU texture. Re-graded only when day/night changes, so
                // between toggles it never redraws (the 20k triangles cost nothing per frame).
                Image(
                    bitmap = image,
                    contentDescription = "vectorized lion",
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.colorMatrix(gradeMatrix(nightP)),
                    // Own render layer → cached as a texture, so a sibling overlay repaint doesn't
                    // re-rasterize the 20k-triangle image every frame.
                    modifier = Modifier.fillMaxSize().graphicsLayer { },
                )
                // Optional shimmer sweep on its own overlay (one additive gradient rect per frame).
                if (shimmer) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = (-0.3f + sheen * 1.6f) * size.width
                        val band = 0.16f * size.width
                        // Only composite the additive blend inside the moving band (cheap on weak GPUs).
                        clipRect(left = (cx - band).coerceAtLeast(0f), top = 0f, right = (cx + band).coerceAtMost(size.width), bottom = size.height) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = 0.18f),
                                    1f to Color.Transparent,
                                    start = Offset(cx - band, 0f),
                                    end = Offset(cx + band, size.height),
                                ),
                                blendMode = BlendMode.Plus,
                            )
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { night = !night }) { Text(if (night) "☀️ Day" else "🌙 Night") }
            Button(onClick = { shimmer = !shimmer }) { Text(if (shimmer) "✨ On" else "✨ Shimmer") }
            Button(onClick = { dense = !dense }) { Text(if (dense) "20k" else "11k") }
        }

        Text(
            "$triCount triangles · decoded + rasterised once in ${buildMs} ms, then cached as one layer",
            fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Same engine, same format as the editable stylised scenes — this is just the far end of the realism↔editability dial.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
