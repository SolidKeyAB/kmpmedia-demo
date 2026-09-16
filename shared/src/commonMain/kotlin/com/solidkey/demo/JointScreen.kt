package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.shape.OGShapeType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "Jointed Shapes (a rig)" — the prototype for connecting shape-clipped photos at a single pixel
 * with a movable angular limit, the building block for articulated rigs and (later) motion dynamics.
 *
 * A **joint** is a pivot: a segment (an [OGImageView] clipped to any [OGShapeType]) rotates around
 * ONE anchor pixel and is clamped between a min and a max angle. Compose gives us exactly that in one
 * modifier — `graphicsLayer { transformOrigin = <the joint pixel>; rotationZ = angle.coerceIn(min, max) }`
 * — and it's all commonMain, so Android and iOS behave identically. NOTHING in the library changes:
 * this is pure demo code layered on top of the existing OGImageView shape-clip, the same way the crop
 * screen's pan/zoom are (see [CropStage]).
 *
 * **Chaining → rigs.** Nest a joint inside another joint's rotated frame and the child inherits the
 * parent's rotation (forward kinematics for free), so a base → upper-arm → forearm chain articulates
 * as one rig. This screen builds a two-link arm you can drag; each link stops dead at its own limit.
 *
 * **Ready for motion dynamics (the "later").** Because every joint angle is just a `Float`, you can
 * drive it with anything afterwards — here the "Let it swing" toggle runs a perpetual pendulum whose
 * envelope is literally the min/max sliders, proving the clamp keeps procedurally-driven motion
 * physical. Swap that driver for a spring/gravity/drag model and the rig animates itself.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun JointScreen() {
    // The photo both segments are cut from — a bundled sample by default, but the shared picker lets
    // you drop in your own gallery photo or a URL, exactly like the crop screen.
    var photo by remember { mutableStateOf<PhotoSource>(PHOTO_CHOICES.first().toPhotoSource()) }
    // The silhouette each limb segment is clipped to. Rounded rectangles read most like an arm.
    var segShape by remember { mutableStateOf(OGShapeType.RECTANGLE) }

    // Joint 1 (base → upper arm) and joint 2 (upper arm → forearm), in degrees. angle2 is RELATIVE to
    // link 1, so the forearm bends in the upper arm's already-rotated frame (true forward kinematics).
    var angle1 by remember { mutableStateOf(18f) }
    var angle2 by remember { mutableStateOf(28f) }

    // The movable limits. Ranges are split at 0 (min ≤ 0 ≤ max) so min can never exceed max — that
    // keeps every coerceIn() well-formed no matter where the sliders sit.
    var min1 by remember { mutableStateOf(-70f) }
    var max1 by remember { mutableStateOf(70f) }
    var min2 by remember { mutableStateOf(-95f) }
    var max2 by remember { mutableStateOf(95f) }

    // Physics preview: a perpetual pendulum whose swing envelope is the min/max sliders themselves.
    var swing by remember { mutableStateOf(false) }

    // Which link the current drag grabbed (1 = upper arm, 2 = forearm). Set on drag-start.
    var grabbed by remember { mutableStateOf(1) }

    // Stage pixel size, captured for LAYING OUT the arm (gestures read `size` directly instead).
    var stagePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val infinite = rememberInfiniteTransition(label = "swing")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )

    // The angles actually rendered this frame: the pendulum when swinging, else the dragged values.
    val a1 = if (swing) envelope(min1, max1, phase) else angle1
    val a2 = if (swing) envelope(min2, max2, phase + HALF_PI) else angle2

    // Resolve the library source once per photo (kept OUT of the size-gated block below so the
    // remember slot count never changes across recompositions).
    val src = remember(photo.key) { photo.toSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Jointed shapes — build a rig",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "A joint pins a shape-clipped photo at ONE pixel and lets it swing between a min and a " +
                "max angle. Drag either segment — it stops dead at its limit. Chain two and you have a " +
                "rig; the base drags the whole arm, the forearm bends within it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── The interactive rig ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.82f)
                .clip(RoundedCornerShape(20.dp))
                .background(cropStageBrush)
                .onSizeChanged { stagePx = it }
                // Drag to pose. Gestures read `size` (the live pointer-area size) so they never see a
                // stale value; disabled while the pendulum drives the rig.
                .pointerInput(swing) {
                    if (swing) return@pointerInput
                    detectDragGestures(
                        onDragStart = { pos ->
                            val base = basePivot(size)
                            val end1 = base + rot(Offset(0f, link1Len(size)), angle1)
                            val end2 = end1 + rot(Offset(0f, link2Len(size)), angle1 + angle2)
                            grabbed = if ((pos - end2).getDistance() <= (pos - end1).getDistance()) 2 else 1
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val p = change.position
                            if (grabbed == 2) {
                                val pivot2 = basePivot(size) + rot(Offset(0f, link1Len(size)), angle1)
                                angle2 = safeCoerce(angleToward(p - pivot2) - angle1, min2, max2)
                            } else {
                                angle1 = safeCoerce(angleToward(p - basePivot(size)), min1, max1)
                            }
                        }
                    )
                },
        ) {
            val w = stagePx.width.toFloat()
            val h = stagePx.height.toFloat()
            if (w > 0f && h > 0f) {
                val baseX = w / 2f
                val baseY = h * BASE_Y_FRAC
                val segW = w * SEG_W_FRAC
                val l1 = h * LINK1_FRAC
                val segW2 = segW * 0.82f
                val l2 = h * LINK2_FRAC

                fun px(v: Float) = with(density) { v.toDp() }

                // Link 1 (upper arm): top-center pinned to the base pivot, rotates by a1.
                Box(
                    modifier = Modifier
                        .offset { IntOffset((baseX - segW / 2f).roundToInt(), baseY.roundToInt()) }
                        .size(px(segW), px(l1))
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            rotationZ = a1
                        }
                ) {
                    OGImageView(
                        source = src,
                        displayShape = segShape,
                        cornerRadius = 22.dp,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onError = {},
                        onEventTriggered = { _, _ -> }
                    )

                    // Joint 2 marker sits at link 1's far end (its bottom-center), so it stays glued
                    // to the connection pixel as the whole arm rotates.
                    JointDot(
                        color = Color.White,
                        diameter = px(segW * 0.28f),
                        modifier = Modifier.offset {
                            IntOffset((segW / 2f - segW * 0.14f).roundToInt(), (l1 - segW * 0.14f).roundToInt())
                        }
                    )

                    // Link 2 (forearm): nested INSIDE link 1's rotated frame, so it inherits a1 and
                    // adds only its own relative a2 — forward kinematics with no math on our side.
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((segW / 2f - segW2 / 2f).roundToInt(), l1.roundToInt()) }
                            .size(px(segW2), px(l2))
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0f)
                                rotationZ = a2
                            }
                    ) {
                        OGImageView(
                            source = src,
                            displayShape = segShape,
                            cornerRadius = 22.dp,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = {},
                            onEventTriggered = { _, _ -> }
                        )
                        // End-effector cap (the "hand") at the forearm tip.
                        JointDot(
                            color = Color.White.copy(alpha = 0.9f),
                            diameter = px(segW2 * 0.5f),
                            modifier = Modifier.offset {
                                IntOffset((segW2 / 2f - segW2 * 0.25f).roundToInt(), (l2 - segW2 * 0.25f).roundToInt())
                            }
                        )
                    }
                }

                // Base pivot — the single connection pixel the whole rig hangs from. Drawn last so it
                // sits on top; a ring makes "one anchor pixel" unmistakable.
                JointDot(
                    color = MaterialTheme.colorScheme.primary,
                    diameter = px(segW * 0.34f),
                    ring = true,
                    modifier = Modifier.offset {
                        IntOffset((baseX - segW * 0.17f).roundToInt(), (baseY - segW * 0.17f).roundToInt())
                    }
                )
            }
        }

        // Live readout of both joints + their limits.
        Text(
            "Joint 1: ${a1.roundToInt()}°  (limit ${min1.roundToInt()}…${max1.roundToInt()})     " +
                "Joint 2: ${a2.roundToInt()}°  (limit ${min2.roundToInt()}…${max2.roundToInt()})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
        )

        // ── Physics preview toggle ────────────────────────────────────────────────────
        FilterChip(
            selected = swing,
            onClick = { swing = !swing },
            leadingIcon = { Text(if (swing) "⏸️" else "🌀", fontSize = 14.sp) },
            label = { Text(if (swing) "Swinging — tap to pose by hand" else "Let it swing (physics preview)") }
        )
        Text(
            "The swing is a perpetual pendulum whose range IS the min/max sliders below — drag them " +
                "while it runs and the motion re-frames instantly. Any motion-dynamics driver (spring, " +
                "gravity, drag) plugs in the same way; the clamp keeps it physical.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Segment silhouette ────────────────────────────────────────────────────────
        Text("Segment shape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CROP_SHAPE_CHOICES.forEach { choice ->
                FilterChip(
                    selected = segShape == choice.type,
                    onClick = { segShape = choice.type },
                    leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                    label = { Text(choice.label) }
                )
            }
        }

        // ── Movable limits ────────────────────────────────────────────────────────────
        Text("Joint 1 limit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        LimitSliders(
            min = min1, max = max1,
            onMin = { min1 = it; angle1 = safeCoerce(angle1, min1, max1) },
            onMax = { max1 = it; angle1 = safeCoerce(angle1, min1, max1) }
        )
        Text("Joint 2 limit", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        LimitSliders(
            min = min2, max = max2, wide = true,
            onMin = { min2 = it; angle2 = safeCoerce(angle2, min2, max2) },
            onMax = { max2 = it; angle2 = safeCoerce(angle2, min2, max2) }
        )

        // ── Photo source (bundled sample · device gallery · URL) ──────────────────────
        Text("Photo the segments are cut from", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        PhotoSourcePicker(selected = photo, onSelected = { photo = it })

        Spacer(Modifier.height(4.dp))
        Text(
            "Under the hood: each segment is your existing OGImageView(displayShape = …) wrapped in " +
                "Modifier.graphicsLayer { transformOrigin = <joint pixel>; rotationZ = angle.coerceIn(min, max) }. " +
                "Nesting one joint inside another's rotated layer chains them into a rig. All commonMain — " +
                "identical on Android and iOS — and ZERO library change: it's the OGJoint wrapper we'd " +
                "promote into the library next, prototyped entirely in demo code.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/** A min/max angle pair as two sliders. Split ranges guarantee min ≤ 0 ≤ max. */
@Composable
private fun LimitSliders(
    min: Float,
    max: Float,
    onMin: (Float) -> Unit,
    onMax: (Float) -> Unit,
    wide: Boolean = false,
) {
    val minEnd = if (wide) -140f else -120f
    val maxEnd = if (wide) 140f else 120f
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("min ${min.roundToInt()}°", fontSize = 11.sp, modifier = Modifier.width(64.dp))
        Slider(value = min, onValueChange = onMin, valueRange = minEnd..0f, modifier = Modifier.weight(1f))
    }
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("max ${max.roundToInt()}°", fontSize = 11.sp, modifier = Modifier.width(64.dp))
        Slider(value = max, onValueChange = onMax, valueRange = 0f..maxEnd, modifier = Modifier.weight(1f))
    }
}

/** A joint anchor marker: a filled dot, optionally with a contrasting ring for the base pivot. */
@Composable
private fun JointDot(
    color: Color,
    diameter: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    ring: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(diameter)
            .background(if (ring) Color.White else color, CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (ring) {
            Box(Modifier.size(diameter * 0.55f).background(color, CircleShape))
        }
    }
}

// ── Joint geometry (all in the stage's pixel space) ───────────────────────────────────
// One convention shared by the graphicsLayer rotation AND our position math, so the drawn segments
// and the computed pivot/limit geometry always agree. angle 0° = the segment hangs straight down.

private const val TWO_PI = (2.0 * PI).toFloat()
private const val HALF_PI = (PI / 2.0).toFloat()
private const val RAD = (PI / 180.0).toFloat()

private const val BASE_Y_FRAC = 0.18f
private const val SEG_W_FRAC = 0.15f
private const val LINK1_FRAC = 0.30f
private const val LINK2_FRAC = 0.26f

private fun basePivot(size: IntSize) = Offset(size.width / 2f, size.height * BASE_Y_FRAC)
private fun link1Len(size: IntSize) = size.height * LINK1_FRAC
private fun link2Len(size: IntSize) = size.height * LINK2_FRAC

/** Rotate [v] by [deg] using the same sign convention as graphicsLayer.rotationZ. */
private fun rot(v: Offset, deg: Float): Offset {
    val r = deg * RAD
    val c = cos(r)
    val s = sin(r)
    return Offset(v.x * c - v.y * s, v.x * s + v.y * c)
}

/** The rotationZ (our convention) that makes a straight-down segment point along [v]. */
private fun angleToward(v: Offset): Float = atan2(-v.x, v.y) / RAD

/** coerceIn that can't throw even if the two bounds arrive out of order. */
private fun safeCoerce(value: Float, a: Float, b: Float): Float =
    value.coerceIn(minOf(a, b), maxOf(a, b))

/** A pendulum sample in [lo, hi] driven by [phase] (radians). */
private fun envelope(lo: Float, hi: Float, phase: Float): Float =
    lo + (hi - lo) * (0.5f + 0.5f * sin(phase))
