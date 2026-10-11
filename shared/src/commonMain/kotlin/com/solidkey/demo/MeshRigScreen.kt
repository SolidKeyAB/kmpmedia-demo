package com.solidkey.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGAiVector
import com.solidkey.painpoints.ai.OGJoint
import com.solidkey.painpoints.ai.OGPointSpec
import com.solidkey.painpoints.ai.OGRig
import com.solidkey.painpoints.motion.OGSpringSpec
import com.solidkey.painpoints.motion.OGSpringValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val RW = 1080
private const val RH = 602

private fun hex(s: String?): Color {
    if (s == null || s.length < 7 || s[0] != '#') return Color(0xFF808080)
    return try {
        Color(s.substring(1, 3).toInt(16), s.substring(3, 5).toInt(16), s.substring(5, 7).toInt(16))
    } catch (e: Throwable) { Color(0xFF808080) }
}

/** Everything precomputed off the frame clock: the bound rig, a flat triangle index table (3 vertex
 *  indices + colour per triangle) over the scene's flattened vertices, the rest positions, scratch
 *  output arrays for the per-frame deform, and a rasterised REST image for the idle fast-path. */
private class MeshData(
    val rig: OGRig,
    val triIdx: IntArray,      // 3 entries per triangle, into the flat vertex array
    val triColor: List<Color>,
    val restX: FloatArray,
    val restY: FloatArray,
    val outX: FloatArray,
    val outY: FloatArray,
    val restBmp: ImageBitmap,
    val triCount: Int,
    val buildMs: Long,
)

/** Decode the lion scene, flatten its triangles, bind a joint-grid rig, and rasterise the rest pose. */
private fun buildMesh(json: String, joints: List<OGJoint>): MeshData? {
    val scene = OGAiVector.decodeSceneOrNull(json)?.sanitized() ?: return null
    // Flatten vertices in the SAME order OGRig.bind uses (shapes in order, points in order).
    val flatX = ArrayList<Float>()
    val flatY = ArrayList<Float>()
    val idx = ArrayList<Int>()
    val colors = ArrayList<Color>()
    for (s in scene.shapes) {
        val base = flatX.size
        for (p in s.points) { flatX.add(p.x); flatY.add(p.y) }
        val col = hex(s.fill)
        // fan-triangulate (lion regions are triangles, so this is just the triangle)
        for (i in 1 until s.points.size - 1) {
            idx.add(base); idx.add(base + i); idx.add(base + i + 1); colors.add(col)
        }
    }
    val n = flatX.size
    val restX = FloatArray(n) { flatX[it] }
    val restY = FloatArray(n) { flatY[it] }
    val triIdx = IntArray(idx.size) { idx[it] }

    val rig = OGRig.bind(scene, joints, nearest = 4, falloff = 2.5f)

    // rasterise the rest pose once (the idle fast-path texture)
    val bmp = ImageBitmap(RW, RH)
    val path = Path()
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, GraphicsCanvas(bmp), Size(RW.toFloat(), RH.toFloat())) {
        var t = 0
        while (t < triIdx.size) {
            val a = triIdx[t]; val b = triIdx[t + 1]; val c = triIdx[t + 2]
            path.rewind()
            path.moveTo(restX[a] * RW, restY[a] * RH)
            path.lineTo(restX[b] * RW, restY[b] * RH)
            path.lineTo(restX[c] * RW, restY[c] * RH)
            path.close()
            drawPath(path, colors[t / 3])
            t += 3
        }
    }
    return MeshData(rig, triIdx, colors, restX, restY, FloatArray(n), FloatArray(n), bmp, triIdx.size / 3, 0L)
}

@Composable
fun MeshRigScreen() {
    val cols = 4; val rows = 3
    var joints by remember { mutableStateOf(OGRig.gridJoints(cols, rows, margin = 0.16f)) }
    val springX = remember { Array(cols * rows) { OGSpringValue(0f) } }
    val springY = remember { Array(cols * rows) { OGSpringValue(0f) } }
    val springSpec = remember { OGSpringSpec(stiffness = 220f, dampingRatio = 0.42f) } // bouncy return

    var mesh by remember { mutableStateOf<MeshData?>(null) }
    var wire by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf(-1) }
    var springing by remember { mutableStateOf(false) }
    var liveMs by remember { mutableStateOf(0.0) }

    // true while the mesh is NOT at rest — the live-deform path; otherwise we draw the cached texture.
    val posed = held >= 0 || springing || joints.any { abs(it.offset.x) + abs(it.offset.y) > 1e-4f }

    LaunchedEffect(Unit) {
        val json = DemoAssets.vecLion11k
        if (json.isBlank()) return@LaunchedEffect
        val t0 = kotlin.time.TimeSource.Monotonic.markNow()
        val built = withContext(Dispatchers.Default) { buildMesh(json, joints) }
        if (built != null) mesh = MeshData(
            built.rig, built.triIdx, built.triColor, built.restX, built.restY,
            built.outX, built.outY, built.restBmp, built.triCount, t0.elapsedNow().inWholeMilliseconds,
        )
    }

    // Spring-back loop: after a release, every joint eases home with the dynamics spring.
    LaunchedEffect(springing) {
        if (!springing) return@LaunchedEffect
        var last = 0L
        while (springing) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f)
            last = now
            var moving = false
            val next = joints.toMutableList()
            for (j in next.indices) {
                if (j == held) continue
                springX[j].update(0f, dt, springSpec); springY[j].update(0f, dt, springSpec)
                next[j] = next[j].copy(offset = OGPointSpec(springX[j].value, springY[j].value))
                if (abs(springX[j].value) + abs(springY[j].value) + abs(springX[j].velocity) + abs(springY[j].velocity) > 1.5e-3f) moving = true
            }
            joints = next
            if (!moving) {
                joints = joints.map { it.copy(offset = OGPointSpec(0f, 0f)) }
                springing = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Clickable joints — a mesh you can bend", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "The SAME dense lion mesh, but rigged. Twelve joints are bound to the triangle vertices by linear-blend skinning; drag any pin and the mesh warps under your finger, then springs home. This is the one thing the rasterise-once/cache trick can't do — while you drag, every vertex is re-skinned and the mesh is re-drawn each frame.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        val data = mesh
        Box(
            Modifier.fillMaxWidth().aspectRatio(RW.toFloat() / RH)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF221B2E)),
            contentAlignment = Alignment.Center,
        ) {
            if (data == null) {
                Text("Rigging the lion…", color = Color.White)
            } else {
                if (!posed) {
                    // Idle fast-path: the cached rest texture (zero per-frame cost, like VectorizedScreen).
                    Image(
                        bitmap = data.restBmp, contentDescription = "lion mesh at rest",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize().graphicsLayer { },
                    )
                }
                // Live layer: the deformed mesh (drawn only while posed) + the draggable joints (always).
                Canvas(
                    Modifier.fillMaxSize().pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                val at = OGPointSpec(pos.x / size.width, pos.y / size.height)
                                val h = data.rig.nearestJoint(joints, at, grabRadius = 0.12f)
                                held = h
                                springing = false
                                if (h >= 0) { springX[h].snapTo(joints[h].offset.x); springY[h].snapTo(joints[h].offset.y) }
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val h = held
                                if (h >= 0) {
                                    val next = joints.toMutableList()
                                    val off = next[h].offset
                                    val nx = (off.x + drag.x / size.width).coerceIn(-0.6f, 0.6f)
                                    val ny = (off.y + drag.y / size.height).coerceIn(-0.6f, 0.6f)
                                    next[h] = next[h].copy(offset = OGPointSpec(nx, ny))
                                    springX[h].snapTo(nx); springY[h].snapTo(ny)
                                    joints = next
                                }
                            },
                            onDragEnd = { held = -1; springing = true },
                            onDragCancel = { held = -1; springing = true },
                        )
                    },
                ) {
                    val w = size.width; val h = size.height
                    if (posed) {
                        val mark = kotlin.time.TimeSource.Monotonic.markNow()
                        data.rig.deformInto(joints, data.outX, data.outY)
                        val ox = data.outX; val oy = data.outY; val ti = data.triIdx
                        val path = Path()
                        var t = 0
                        while (t < ti.size) {
                            val a = ti[t]; val b = ti[t + 1]; val c = ti[t + 2]
                            path.rewind()
                            path.moveTo(ox[a] * w, oy[a] * h)
                            path.lineTo(ox[b] * w, oy[b] * h)
                            path.lineTo(ox[c] * w, oy[c] * h)
                            path.close()
                            if (wire) drawPath(path, Color(0xFF6CC7FF), style = Stroke(width = 1f))
                            else drawPath(path, data.triColor[t / 3])
                            t += 3
                        }
                        liveMs = mark.elapsedNow().inWholeMicroseconds / 1000.0
                    }
                    // joint handles
                    for (i in joints.indices) {
                        val p = joints[i].pose
                        val c = Offset(p.x * w, p.y * h)
                        drawCircle(Color(0x66000000), radius = 13f, center = c)
                        drawCircle(if (i == held) Color(0xFFFFD166) else Color(0xFFEF6FB5), radius = 10f, center = c)
                        drawCircle(Color.White, radius = 3.5f, center = c)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { wire = !wire }) { Text(if (wire) "▦ Wireframe" else "🎨 Filled") }
            Button(onClick = {
                for (j in joints.indices) { springX[j].snapTo(0f); springY[j].snapTo(0f) }
                joints = joints.map { it.copy(offset = OGPointSpec(0f, 0f)) }
                held = -1; springing = false
            }) { Text("↺ Reset") }
        }

        data?.let {
            Text(
                "${it.triCount} triangles · ${it.rig.vertexCount} vertices · ${joints.size} joints, bound in ${it.buildMs} ms",
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (posed) "LIVE deform — re-skinning + re-drawing every frame · ${(liveMs * 10).toInt() / 10.0} ms/frame"
                else "At rest — showing the cached texture (0 per-frame cost). Drag a pin to go live.",
                fontSize = 11.sp, color = if (posed) Color(0xFF6CC7FF) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "Skinning (OGRig) is pure maths in the library — ~1.3 ms to move 20k vertices. The real cost is the redraw, which is exactly why a rig is the case caching can't cover.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}
