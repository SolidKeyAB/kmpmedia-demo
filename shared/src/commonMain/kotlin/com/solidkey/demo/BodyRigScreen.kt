package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.shape.OGPolygonShape
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.source.OGSourceType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "Add your head to a rigged body" — the friendly payoff of the joint prototype ([JointScreen]).
 *
 * Instead of asking the user to build a rig from scratch, this screen ships an **already-prepared
 * body**: a torso with two two-link arms and two two-link legs, every segment pinned at one pixel
 * exactly like [JointScreen]'s links (`graphicsLayer { transformOrigin = <the joint pixel>;
 * rotationZ = angle }`, children nested in their parent's rotated frame → forward kinematics for
 * free). The user does ONE creative thing: **drop in a photo, clip it to a shape, and it becomes the
 * head** — attached at the neck joint they set (head size + neck tilt). Then they pick a **dynamic**
 * (Idle / Wave / Walk / Jumping jacks / Dance) and the whole rig animates, head bobbing along.
 *
 * Everything the user's image touches goes through the library's [OGImageView] shape-crop
 * (`displayShape` + `contentScale` + `alignment`) — the SAME primitive as the crop screen, reused
 * verbatim from [CropEditor]. The rig itself and every animation are pure commonMain demo code
 * (identical on Android & iOS) with **zero library change** — the neck is just one more joint.
 */
/**
 * The head outline the AI traced on the bundled sample photo ([avatar_ozge]), expressed in the
 * library's normalized 0..1 lasso space — exactly the kind of vertex list a segmentation model OR an
 * on-image finger-draw produces. Fed to [OGPolygonShape] it clips the photo to JUST the head — no
 * shoulders, shirt or couch — with no external editor. (Traced for the sample at 1× center framing.)
 */
private val AVATAR_HEAD_OUTLINE: Shape = OGPolygonShape.of(
    0.48f to 0.02f, 0.64f to 0.04f, 0.78f to 0.10f, 0.86f to 0.22f,
    0.88f to 0.38f, 0.84f to 0.54f, 0.73f to 0.69f, 0.59f to 0.79f,
    0.48f to 0.83f, 0.37f to 0.79f, 0.25f to 0.68f, 0.16f to 0.53f,
    0.13f to 0.38f, 0.15f to 0.22f, 0.24f to 0.09f, 0.35f to 0.03f,
)

/**
 * The chin's y in [AVATAR_HEAD_OUTLINE] (its lowest point, ≈0.83 of the head box). The lasso leaves
 * the box empty below this, so the neck joint is pinned here — not at the box bottom — to seat the
 * head against the torso with no floating gap.
 */
private const val LASSO_CHIN_FRAC = 0.83f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BodyRigScreen() {
    // The image the user turns into a head. Defaults to a bundled sample so the body opens complete;
    // the shared picker (samples · device gallery · URL) lets them drop in their own — same as crop.
    var photo by remember { mutableStateOf<PhotoSource>(PhotoSource.Resource("avatar_ozge", "Özge")) }

    // How the head photo is clipped + framed — reusing the crop screen's shape / zoom / alignment.
    var headShape by remember { mutableStateOf(OGShapeType.CIRCLE) }
    var zoom by remember { mutableStateOf(1f) }
    var bias by remember { mutableStateOf(Offset.Zero) }

    // ✂️ Cut out JUST the head with the library's new free-form lasso (OGPolygonShape). Default ON for
    // the sample so the body opens with only the head — no shoulders/couch. Picking a built-in shape
    // turns it off. When on, the AI-traced outline needs the sample at 1× center framing.
    var useLasso by remember { mutableStateOf(true) }

    // "Setting the joint": how big the head is and how the neck tilts it. These two sliders ARE the
    // neck joint — headSize is the segment length, neckTilt is its resting rotationZ at the pivot.
    var headSize by remember { mutableStateOf(0.24f) } // fraction of stage width
    var neckTilt by remember { mutableStateOf(0f) }     // degrees, added on top of the dynamic bob

    // The chosen dynamic. Idle = a gentle breathing loop so the assembled body is alive on arrival.
    var move by remember { mutableStateOf(Move.Wave) }

    var stagePx by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // One perpetual phase drives every joint; each Move maps it to a full-body Pose (see below).
    val infinite = rememberInfiniteTransition(label = "rig")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(move.periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val pose = move.pose(phase)

    val src = remember(photo.key) { photo.toSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Add your head to a body",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "The arms, legs and torso are already rigged for you. Pick a photo, clip it to a shape, and " +
                "it becomes the head — pinned at the neck joint you set. Then tap a dynamic and the whole " +
                "body moves, your head riding along.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── The rigged body ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.74f)
                .clip(RoundedCornerShape(20.dp))
                .background(rigStageBrush)
                .onSizeChanged { stagePx = it },
            contentAlignment = Alignment.Center
        ) {
            val w = stagePx.width.toFloat()
            val h = stagePx.height.toFloat()
            if (w > 0f && h > 0f) {
                BodyFigure(
                    stageW = w, stageH = h,
                    pose = pose,
                    headSrc = src,
                    headShape = headShape,
                    headClip = if (useLasso) AVATAR_HEAD_OUTLINE else null,
                    headSizeFrac = headSize,
                    headZoom = zoom,
                    headBias = bias,
                    neckTilt = neckTilt,
                    density = density,
                )
            }
        }

        // ── Dynamics — the "play with it" part ────────────────────────────────────────
        Text("Dynamics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Move.entries.forEach { m ->
                FilterChip(
                    selected = move == m,
                    onClick = { move = m },
                    leadingIcon = { Text(m.glyph, fontSize = 14.sp) },
                    label = { Text(m.label) }
                )
            }
        }
        Text(
            "Each dynamic maps one looping phase onto every joint — arms counter-swing for the walk, " +
                "both sides open together for jumping jacks, the forearm flaps for the wave. Swap in a " +
                "spring/gravity driver and the same rig would animate itself.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Add your image ────────────────────────────────────────────────────────────
        Text("Your head — pick an image", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        PhotoSourcePicker(selected = photo, onSelected = { photo = it })

        // ── Clip it to a shape ────────────────────────────────────────────────────────
        Text("Clip the head to a shape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ✂️ The new library primitive: a free-form polygon lasso that cuts out JUST the head.
            FilterChip(
                selected = useLasso,
                onClick = { useLasso = true },
                leadingIcon = { Text("✂️", fontSize = 14.sp) },
                label = { Text("Head lasso") }
            )
            CROP_SHAPE_CHOICES.forEach { choice ->
                FilterChip(
                    selected = !useLasso && headShape == choice.type,
                    onClick = { useLasso = false; headShape = choice.type },
                    leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                    label = { Text(choice.label) }
                )
            }
        }
        Text(
            "✂️ Head lasso = the library's new OGPolygonShape: a free-form outline made of line " +
                "segments (normalized 0..1 points). It's AI-friendly — these vertices were traced by " +
                "the AI on the sample photo, so it clips to just the head with no external tool. Pick " +
                "a built-in shape below to switch back to circle/triangle/… framing.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )

        // ── Frame the face (reused crop controls: zoom + a 3×3 focal grid) ────────────
        // Only meaningful for the built-in shapes; the lasso already traces the head exactly.
        if (!useLasso) {
            Text("Frame the face — zoom: ${(zoom * 10).roundToInt() / 10f}×", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Slider(value = zoom, onValueChange = { zoom = it }, valueRange = MIN_ZOOM..MAX_ZOOM)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ALIGN_GRID.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { choice ->
                            val selected = bias == choice.bias
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { bias = choice.bias },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    choice.glyph,
                                    fontSize = 16.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Set the neck joint ────────────────────────────────────────────────────────
        Text("Set the neck joint", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("size", fontSize = 11.sp, modifier = Modifier.width(64.dp))
            Slider(value = headSize, onValueChange = { headSize = it }, valueRange = 0.14f..0.36f, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("tilt ${neckTilt.roundToInt()}°", fontSize = 11.sp, modifier = Modifier.width(64.dp))
            Slider(value = neckTilt, onValueChange = { neckTilt = it }, valueRange = -35f..35f, modifier = Modifier.weight(1f))
        }
        Text(
            "The neck is just one more joint: the head is an OGImageView(displayShape = …) pinned at " +
                "its bottom-center pixel with graphicsLayer { transformOrigin = neck; rotationZ = tilt }. " +
                "Size sets the segment length; tilt is its resting angle — the dynamic adds its bob on top.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(4.dp))
        Text(
            "The rig, the neck and every dynamic are pure commonMain demo code (identical on Android & " +
                "iOS) with ZERO library change — the only library primitive here is the OGImageView " +
                "shape-crop that turns your photo into the head, the same one the crop screen uses.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/**
 * The prepared humanoid: torso + two arms + two legs (each a two-link joint chain) + the user's head
 * at the neck. Everything is positioned in the stage's pixel space so the whole figure scales with
 * the stage; [pose] supplies every joint angle for this frame, plus a vertical bob and a body sway.
 */
@Composable
private fun BodyFigure(
    stageW: Float,
    stageH: Float,
    pose: Pose,
    headSrc: OGSourceType,
    headShape: OGShapeType,
    headClip: Shape?,
    headSizeFrac: Float,
    headZoom: Float,
    headBias: Offset,
    neckTilt: Float,
    density: Density,
) {
    fun px(v: Float) = with(density) { v.toDp() }

    val cx = stageW / 2f
    val torsoTopY = stageH * TORSO_TOP_FRAC
    val torsoBotY = stageH * TORSO_BOT_FRAC
    val torsoW = stageW * TORSO_W_FRAC
    val pelvis = Offset(cx, torsoBotY)

    val shoulderY = torsoTopY + stageH * 0.03f
    val shoulderDx = torsoW * 0.42f
    val hipY = torsoBotY - stageH * 0.01f
    val hipDx = torsoW * 0.30f

    val armUpper = stageH * ARM_UPPER_FRAC
    val armLower = stageH * ARM_LOWER_FRAC
    val armW = stageW * ARM_W_FRAC
    val legUpper = stageH * LEG_UPPER_FRAC
    val legLower = stageH * LEG_LOWER_FRAC
    val legW = stageW * LEG_W_FRAC

    val headPx = stageW * headSizeFrac

    // The whole figure bobs vertically and sways around the pelvis. Both are subtle; the sway lets
    // "Dance"/"Idle" rock the body without doing per-limb math.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, (pose.bob * stageH).roundToInt()) }
            .graphicsLayer {
                transformOrigin = TransformOrigin(pelvis.x / stageW, pelvis.y / stageH)
                rotationZ = pose.sway
            }
    ) {
        // Legs (drawn first so the torso overlaps the hips cleanly).
        Limb(
            pivot = Offset(cx - hipDx, hipY),
            upperLen = legUpper, lowerLen = legLower, width = legW,
            upperAngle = pose.lHip, lowerAngle = pose.lKn,
            brush = legBrush, density = density,
        )
        Limb(
            pivot = Offset(cx + hipDx, hipY),
            upperLen = legUpper, lowerLen = legLower, width = legW,
            upperAngle = pose.rHip, lowerAngle = pose.rKn,
            brush = legBrush, density = density,
        )

        // Torso — a rounded slab that breathes (scaleY) around the hips.
        Box(
            modifier = Modifier
                .offset { IntOffset((cx - torsoW / 2f).roundToInt(), torsoTopY.roundToInt()) }
                .size(px(torsoW), px(torsoBotY - torsoTopY))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    scaleY = pose.torsoScale
                }
                .clip(RoundedCornerShape(percent = 44))
                .background(torsoBrush)
        )

        // Arms (drawn over the torso so a raised arm reads clearly).
        Limb(
            pivot = Offset(cx - shoulderDx, shoulderY),
            upperLen = armUpper, lowerLen = armLower, width = armW,
            upperAngle = pose.lSh, lowerAngle = pose.lEl,
            brush = armBrush, density = density,
        )
        Limb(
            pivot = Offset(cx + shoulderDx, shoulderY),
            upperLen = armUpper, lowerLen = armLower, width = armW,
            upperAngle = pose.rSh, lowerAngle = pose.rEl,
            brush = armBrush, density = density,
        )

        // Head — the user's shape-clipped photo, pinned at the neck joint. For a built-in shape the
        // photo fills the whole box so the neck is the box bottom; for the lasso the head ends at the
        // chin (LASSO_CHIN_FRAC), so the neck is there — pin THAT to the torso so the head isn't left
        // floating above the empty tail of the box.
        val neckFrac = if (headClip != null) LASSO_CHIN_FRAC else 1f
        // Where the neck meets the torso: a small gap for shapes, a slight overlap for the lasso so the
        // chin tucks against the body.
        val neckJoin = if (headClip != null) -stageH * 0.006f else stageH * 0.012f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cx - headPx / 2f).roundToInt(),
                        // box top so that (box top + neckFrac*headPx) — the neck — sits at torsoTopY - neckJoin
                        (torsoTopY - neckJoin - neckFrac * headPx).roundToInt()
                    )
                }
                .size(px(headPx), px(headPx))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, neckFrac) // the neck (chin for the lasso)
                    rotationZ = neckTilt + pose.neck
                }
        ) {
            OGImageView(
                source = headSrc,
                displayShape = headShape,
                // ✂️ When the lasso is active, clip to the traced head outline; the outline is defined
                // in image space, so pin framing to 1× center so it lines up with the photo.
                clipShape = headClip,
                cornerRadius = (headPx * 0.16f).let { px(it) },
                contentScale = cropContentScale(crop = true, zoom = if (headClip != null) 1f else headZoom),
                alignment = if (headClip != null) BiasAlignment(0f, 0f) else BiasAlignment(headBias.x, headBias.y),
                modifier = Modifier.fillMaxSize(),
                onError = {},
                onEventTriggered = { _, _ -> }
            )
        }
    }
}

/**
 * A two-link limb: an [upperLen]×[width] segment pinned (top-center) at [pivot] and rotated by
 * [upperAngle], with a slightly thinner lower segment nested in its rotated frame and rotated by the
 * RELATIVE [lowerAngle] — the exact forward-kinematics nesting [JointScreen] uses, filled with a
 * [brush] so it reads as a mannequin limb (the head is the only photo in the rig).
 */
@Composable
private fun Limb(
    pivot: Offset,
    upperLen: Float,
    lowerLen: Float,
    width: Float,
    upperAngle: Float,
    lowerAngle: Float,
    brush: Brush,
    density: Density,
) {
    fun px(v: Float) = with(density) { v.toDp() }
    val lowerW = width * 0.86f
    // Upper rotation frame — deliberately NOT clipped, so the nested lower segment can extend past
    // it (same as JointScreen). The visible rounded fill is a separate fillMaxSize child.
    Box(
        modifier = Modifier
            .offset { IntOffset((pivot.x - width / 2f).roundToInt(), pivot.y.roundToInt()) }
            .size(px(width), px(upperLen))
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 0f)
                rotationZ = upperAngle
            }
    ) {
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50)).background(brush))

        // Lower link, nested in the upper's rotated frame → inherits its rotation (FK for free).
        Box(
            modifier = Modifier
                .offset { IntOffset((width / 2f - lowerW / 2f).roundToInt(), (upperLen * 0.92f).roundToInt()) }
                .size(px(lowerW), px(lowerLen))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    rotationZ = lowerAngle
                }
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(percent = 50)).background(brush))
        }
    }
}

// ── Pose model + dynamics ──────────────────────────────────────────────────────────────
// Angle convention matches JointScreen: 0° = the segment hangs straight down; a positive rotationZ
// swings its tip toward screen-left. Every field is an ABSOLUTE angle except the *lower* limb angles,
// which are RELATIVE to their upper segment (they live in its rotated frame → forward kinematics).

private data class Pose(
    val neck: Float,
    val lSh: Float, val lEl: Float,
    val rSh: Float, val rEl: Float,
    val lHip: Float, val lKn: Float,
    val rHip: Float, val rKn: Float,
    val bob: Float,        // vertical translate as a fraction of stage height (down = +)
    val sway: Float,       // whole-figure rotation around the pelvis (deg)
    val torsoScale: Float, // breathing (scaleY on the torso)
)

private const val TWO_PI = (2.0 * PI).toFloat()

// Resting pose: arms hang slightly out, legs slightly apart, head level.
private const val ARM_OUT = 15f
private const val ELBOW = 6f
private const val LEG_OUT = 7f
private const val KNEE = 3f

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** The dynamics the user can play with. Each maps the looping [phase] (0..2π) to a full-body [pose]. */
private enum class Move(val label: String, val glyph: String, val periodMs: Int) {
    Idle("Idle", "🧘", 3200) {
        override fun pose(p: Float): Pose {
            val s = sin(p)
            return Pose(
                neck = 2f * s,
                lSh = ARM_OUT + 3f * s, lEl = ELBOW,
                rSh = -ARM_OUT - 3f * s, rEl = -ELBOW,
                lHip = LEG_OUT, lKn = KNEE,
                rHip = -LEG_OUT, rKn = -KNEE,
                bob = 0.006f * s, sway = 1.2f * s, torsoScale = 1f + 0.025f * s,
            )
        }
    },
    Wave("Wave", "👋", 900) {
        override fun pose(p: Float): Pose {
            val flap = sin(p)                 // the forearm oscillation
            val idle = sin(p * 0.25f)
            return Pose(
                neck = -6f + 2f * idle,        // tilt slightly toward the raised arm
                lSh = ARM_OUT + 3f * idle, lEl = ELBOW,
                rSh = -150f, rEl = -32f + 26f * flap,
                lHip = LEG_OUT, lKn = KNEE,
                rHip = -LEG_OUT, rKn = -KNEE,
                bob = 0f, sway = 0f, torsoScale = 1f,
            )
        }
    },
    Walk("Walk", "🚶", 1100) {
        override fun pose(p: Float): Pose {
            val s = sin(p)
            val hip = 20f * s
            // Knees bend on the forward swing of each leg (half-phase offset between the two).
            val lBend = 34f * (0.5f + 0.5f * sin(p + HALF_PI))
            val rBend = 34f * (0.5f + 0.5f * sin(p - HALF_PI))
            return Pose(
                neck = 0f,
                // Arms counter-swing the legs.
                lSh = ARM_OUT - 22f * s, lEl = ELBOW + 8f,
                rSh = -ARM_OUT + 22f * s, rEl = -ELBOW - 8f,
                lHip = LEG_OUT + hip, lKn = KNEE + lBend,
                rHip = -LEG_OUT + hip, rKn = -KNEE - rBend,
                bob = -0.02f * abs(sin(p * 2f)), sway = 0f, torsoScale = 1f,
            )
        }
    },
    JumpingJacks("Jumping jacks", "🤸", 900) {
        override fun pose(p: Float): Pose {
            val open = 0.5f + 0.5f * sin(p)   // 0 = closed (arms down), 1 = open (arms up, legs wide)
            return Pose(
                neck = 0f,
                lSh = lerp(ARM_OUT, 150f, open), lEl = ELBOW,
                rSh = lerp(-ARM_OUT, -150f, open), rEl = -ELBOW,
                lHip = lerp(LEG_OUT, 26f, open), lKn = KNEE,
                rHip = lerp(-LEG_OUT, -26f, open), rKn = -KNEE,
                bob = -0.03f * open, sway = 0f, torsoScale = 1f,
            )
        }
    },
    Dance("Dance", "🕺", 1400) {
        override fun pose(p: Float): Pose {
            val a = sin(p)
            val b = sin(p + PI.toFloat())
            return Pose(
                neck = 8f * sin(p * 2f),
                lSh = ARM_OUT + 70f * (0.5f + 0.5f * a), lEl = ELBOW + 20f * a,
                rSh = -ARM_OUT - 70f * (0.5f + 0.5f * b), rEl = -ELBOW - 20f * b,
                lHip = LEG_OUT + 8f * a, lKn = KNEE + 14f * (0.5f + 0.5f * b),
                rHip = -LEG_OUT + 8f * a, rKn = -KNEE - 14f * (0.5f + 0.5f * a),
                bob = -0.012f * abs(sin(p * 2f)), sway = 9f * a, torsoScale = 1f + 0.02f * a,
            )
        }
    };

    abstract fun pose(p: Float): Pose
}

private val HALF_PI = (PI / 2.0).toFloat()

// ── Body geometry (fractions of the stage) + look ────────────────────────────────────────
private const val TORSO_TOP_FRAC = 0.34f
private const val TORSO_BOT_FRAC = 0.58f
private const val TORSO_W_FRAC = 0.26f
private const val ARM_UPPER_FRAC = 0.15f
private const val ARM_LOWER_FRAC = 0.14f
private const val ARM_W_FRAC = 0.075f
private const val LEG_UPPER_FRAC = 0.17f
private const val LEG_LOWER_FRAC = 0.16f
private const val LEG_W_FRAC = 0.085f

/** A calm dark stage so the colourful mannequin + the photo head pop. */
private val rigStageBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF1B1130), Color(0xFF2A1A4A)))

private val torsoBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF7B2FF7), Color(0xFF5A1FC0)))

private val armBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF00C2A8), Color(0xFF008F7D)))

private val legBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFFFF5DA2), Color(0xFFC23C79)))
