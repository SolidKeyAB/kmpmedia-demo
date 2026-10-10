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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.fx.OGSpeedLines
import com.solidkey.painpoints.fx.OGSpeedLinesView
import com.solidkey.painpoints.fx.ogAfterImage
import com.solidkey.painpoints.fx.ogGlow
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.look.OGLookSpec
import com.solidkey.painpoints.look.OGLooks
import com.solidkey.painpoints.look.ogLook
import com.solidkey.painpoints.motion.OGCloth
import com.solidkey.painpoints.motion.OGClothCollider
import com.solidkey.painpoints.motion.OGClothMeshSpec
import com.solidkey.painpoints.motion.OGClothPinMode
import com.solidkey.painpoints.motion.OGClothSpec
import com.solidkey.painpoints.motion.OGClothStrip
import com.solidkey.painpoints.motion.OGCloths
import com.solidkey.painpoints.motion.OGSpringSpec
import com.solidkey.painpoints.motion.OGSpringValue
import com.solidkey.painpoints.motion.OGSquash
import com.solidkey.painpoints.motion.OGSway
import com.solidkey.painpoints.particle.OGParticleView
import com.solidkey.painpoints.particle.OGParticles
import com.solidkey.painpoints.shape.OGShapeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 🎬 A **scene in motion** — a side-on walk cycle wearing Özge's head, driven by the 1.35.0 motion
 * dynamics so the gait reads *real*, not robotic:
 *  • a profile two-leg / two-arm rig strides with a proper stance/swing cycle,
 *  • the **knees and elbows spring-LAG** their drive angle ([OGSpringValue]) → follow-through / whip on
 *    the lower limbs, and the body **squashes** on each footfall ([OGSquash]),
 *  • a real cloth scarf hangs + flutters at the neck via [OGClothStrip] and the head drifts with [OGSway],
 *  • dressed with `OGSpeedLines`, `ogAfterImage`, `ogGlow`, drifting `OGParticles`, and an `ogLook`
 *    colour grade over the whole frame.
 * Reuses the FK [Limb] from [BodyRigScreen]; every effect toggles. Same code Android & iOS.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneScreen() {
    var speedLines by remember { mutableStateOf(true) }
    var petals by remember { mutableStateOf(true) }
    var trail by remember { mutableStateOf(false) }
    var glow by remember { mutableStateOf(false) }
    var scarf by remember { mutableStateOf(true) }
    var scaffold by remember { mutableStateOf(true) }
    var dynamics by remember { mutableStateOf(true) }
    var sunset by remember { mutableStateOf(true) }
    var lookName by remember { mutableStateOf("warm") }

    val density = LocalDensity.current
    val headSrc = remember { OGImageResourceFileType("avatar_ozge", OGImageFormat.JPEG) }

    // The walk phase: an infinite transition drives the figure (reading it recomposes the figure every
    // frame, so it animates — same pattern as the Body-rig screen).
    val clock by rememberInfiniteTransition(label = "scene").animateFloat(
        0f, TAU, infiniteRepeatable(tween(WALK_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart), label = "walk",
    )
    // The sun's slow descent: 0 = high day, 1 = sunk below the horizon. Reverses so the sky breathes
    // day→dusk→night→dawn. Drives the sun position/colour, the sky gradient, and the whole-frame ogLook grade.
    val sunProgress by rememberInfiniteTransition(label = "sun").animateFloat(
        0f, 1f, infiniteRepeatable(tween(SUN_PERIOD_MS, easing = LinearEasing), RepeatMode.Reverse), label = "sunset",
    )

    // Dynamics state: one spring per lower joint (knees + elbows) that LAGS its drive angle → the
    // follow-through that makes the gait feel alive; a follow-chain scarf; and footfall squash.
    val lagFeel = remember { OGSpringSpec(stiffness = 140f, dampingRatio = 0.55f) }
    val kneeN = remember { OGSpringValue(0f) }
    val kneeF = remember { OGSpringValue(0f) }
    val elbowN = remember { OGSpringValue(0f) }
    val elbowF = remember { OGSpringValue(0f) }
    // A livelier silk than the default "scarf" preset, so it flutters clearly in the showcase.
    val cloth = remember {
        OGClothStrip(
            (OGCloths.preset("scarf") ?: OGClothSpec())
                .copy(length = 0.52f, gravity = 0.45f, damping = 0.5f, wind = -1.6f, windAmplitude = 0.7f, windFrequency = 0.9f),
        )
    }
    val sway = remember { OGSway(amplitude = 1f, frequency = 0.55f) }
    // A 2-D OGCloth FLAG flying from a pole in the top-left (clear of the figure): a real cloth SHEET (not the
    // 1-D scarf) pinned along its hoist (LEFT_EDGE) that streams out, waves, and droops in the gusty wind, its
    // folds catching light. The pole is a collider, so the flag presses/wraps against it.
    val flag = remember {
        OGCloth(
            (OGCloths.meshPreset("sail") ?: OGClothMeshSpec())
                .copy(cols = 16, rows = 9, width = 0.3f, height = 0.17f, gravity = 0.3f, damping = 0.35f, wind = 1.7f, windAmplitude = 1.5f, windFrequency = 1.5f, windSeed = 9, friction = 0.2f, pinMode = OGClothPinMode.LEFT_EDGE),
        ).apply {
            colliders.add(OGClothCollider(FLAG_POLE_X, FLAG_TOP, FLAG_POLE_X, FLAG_POLE_B, 0.01f)) // the flagpole
        }
    }
    val sim = remember { SceneSim() }
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    sim.elapsed += dt
                    val ph = clock

                    // Footfall bob (two dips/cycle) → its vertical speed drives the squash.
                    val bob = -BOB * abs(sin(ph * 2f))
                    val vy = if (dt > 0f) (bob - sim.lastBob) / dt else 0f
                    sim.lastBob = bob
                    sim.bob = bob

                    // Lower-joint drive targets, then spring-LAG them (or snap, if dynamics off).
                    val kneeTN = KNEE_MAX * (0.5f + 0.5f * cos(ph))
                    val kneeTF = KNEE_MAX * (0.5f - 0.5f * cos(ph))
                    // Elbows bend the OPPOSITE way to knees (forearm forward, not back) → negative angle.
                    val elbowTN = -ELBOW_BASE - ELBOW_SW * sin(ph)
                    val elbowTF = -ELBOW_BASE + ELBOW_SW * sin(ph)
                    if (dynamics) {
                        kneeN.update(kneeTN, dt, lagFeel); kneeF.update(kneeTF, dt, lagFeel)
                        elbowN.update(elbowTN, dt, lagFeel); elbowF.update(elbowTF, dt, lagFeel)
                    } else {
                        kneeN.snapTo(kneeTN); kneeF.snapTo(kneeTF); elbowN.snapTo(elbowTN); elbowF.snapTo(elbowTF)
                    }
                    sim.kneeN = kneeN.value; sim.kneeF = kneeF.value
                    sim.elbowN = elbowN.value; sim.elbowF = elbowF.value

                    // Squash the body on footfall (volume-preserving), only when dynamics is on.
                    if (dynamics) {
                        val sc = OGSquash.fromSpeed(vy * SQUASH_K, intensity = 1f, max = 0.22f)
                        sim.squashX = sc.scaleX; sim.squashY = sc.scaleY
                    } else {
                        sim.squashX = 1f; sim.squashY = 1f
                    }

                    sim.headSwayDeg = if (dynamics) sway.value(sim.elapsed) * 4f else 0f

                    // Scarf anchor = the figure's ACTUAL rendered neck, so the wrap + cloth tail stay glued
                    // to it. The figure bobs (an offset) and squashes (scaleY around the pelvis) with NO
                    // horizontal sway, so reproduce exactly that transform here (don't invent a sway).
                    if (sim.figW > 0f) {
                        val pelvisY = sim.figH * PELVIS_Y_FRAC
                        val neckBaseY = sim.figH * (SHOULDER_Y_FRAC + 0.03f) // just below the chin
                        val ax = sim.figW * CX_FRAC
                        val ay = bob * sim.figH + pelvisY + (neckBaseY - pelvisY) * sim.squashY
                        sim.neckX = ax; sim.neckY = ay
                        cloth.step(dt, ax / sim.figW, ay / sim.figH)
                    }
                    // The flag lives in SCENE-normalized space (0..1 of the scene box): its hoist (left edge)
                    // pins along the flagpole; the body streams out to the right and waves in the wind.
                    flag.step(dt, FLAG_POLE_X, FLAG_HOIST_TOP, FLAG_POLE_X, FLAG_HOIST_BOT)
                    tick++
                }
                last = now
            }
        }
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("🎬 Scene in motion", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "A side-on walk cycle with your head at sunset. The dynamics make it feel real: the knees & " +
                "elbows spring-lag the stride (follow-through) and the body squashes on each footfall. Two " +
                "kinds of real cloth — a 1-D OGClothStrip scarf at the neck and a 2-D OGCloth flag on the pole " +
                "— ripple in the wind, while the sun descends and an ogLook grade shifts the whole frame from " +
                "day to dusk to night. Toggle 'Sunset', 'Flag' or 'Dynamics' to see each part.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        // Read the clocks UNCONDITIONALLY so this composable recomposes every frame (otherwise a first
        // size==0 frame wouldn't subscribe and the figure would never appear once the size arrived).
        val walkPhase = clock
        // Sun progress drives the sunset: animated when 'Sunset' is on, else a fixed dusk so the manual
        // colour-grade picker + a static backdrop apply instead.
        val sp = if (sunset) sunProgress else 0.45f
        // Quantize the value feeding the ogLook grade so its cached ColorFilter rebuilds ~40×/cycle, not 60×/s.
        val spLook = (sp * 40f).roundToInt() / 40f
        val lookSpec = remember(sunset, spLook, lookName) {
            when {
                sunset -> sunsetLook(spLook)
                lookName == "original" -> OGLookSpec.Identity
                else -> OGLooks.preset(lookName) ?: OGLookSpec.Identity
            }
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(0.80f).clip(RoundedCornerShape(18.dp))
                .background(skyBrush(sp))
                .ogLook(lookSpec),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (tick < 0) return@Canvas
                drawScene(sim.elapsed, sp, size.width, size.height)
            }

            // A cloth FLAG (2-D OGCloth) flying from a pole in the top-left. Pole first, then the sheet.
            if (scaffold) {
                Canvas(Modifier.fillMaxSize()) {
                    if (tick < 0) return@Canvas
                    val wd = size.width; val ht = size.height
                    val poleColor = Color(0xFF4E4034)
                    val sw = wd * 0.012f
                    drawLine(poleColor, Offset(FLAG_POLE_X * wd, FLAG_POLE_B * ht), Offset(FLAG_POLE_X * wd, FLAG_TOP * ht), strokeWidth = sw, cap = StrokeCap.Round)
                    drawCircle(Color(0xFFCDB89A), radius = sw * 0.9f, center = Offset(FLAG_POLE_X * wd, FLAG_TOP * ht)) // finial
                    // The flag — filled quads through the cloth nodes, shaded along its length (depth toward the
                    // free edge) AND by fold (a cell squeezed horizontally is a wave crest, so darken it).
                    val restW = 0.3f / (flag.cols - 1)
                    for (r in 0 until flag.rows - 1) {
                        for (c in 0 until flag.cols - 1) {
                            val base = lerpColor(FLAG_A, FLAG_B, c / (flag.cols - 2f))
                            val fold = (abs(flag.x(c + 1, r) - flag.x(c, r)) / restW).coerceIn(0.45f, 1f)
                            val col = Color(base.red * fold, base.green * fold, base.blue * fold, 1f)
                            val quad = Path().apply {
                                moveTo(flag.x(c, r) * wd, flag.y(c, r) * ht)
                                lineTo(flag.x(c + 1, r) * wd, flag.y(c + 1, r) * ht)
                                lineTo(flag.x(c + 1, r + 1) * wd, flag.y(c + 1, r + 1) * ht)
                                lineTo(flag.x(c, r + 1) * wd, flag.y(c, r + 1) * ht)
                                close()
                            }
                            drawPath(quad, color = col)
                        }
                    }
                }
            }

            if (speedLines) OGSpeedLinesView(OGSpeedLines.MOTION, Modifier.fillMaxSize())

            Box(
                Modifier.fillMaxHeight(0.94f).aspectRatio(FIGURE_ASPECT)
                    .onSizeChanged { sim.figW = it.width.toFloat(); sim.figH = it.height.toFloat() }
                    .then(if (trail) Modifier.ogAfterImage(count = 5, step = Offset(-13f, 0f), maxAlpha = 0.28f) else Modifier)
                    .then(if (glow) Modifier.ogGlow(color = Color(0xFFFFD166), radius = 1.2f, intensity = 0.28f) else Modifier),
            ) {
                if (scarf) {
                    Canvas(Modifier.fillMaxSize()) {
                        if (tick < 0 || sim.figW <= 0f) return@Canvas
                        val n = cloth.count
                        var prevX = cloth.x(0) * sim.figW
                        var prevY = cloth.y(0) * sim.figH
                        for (i in 1 until n) {
                            val f = i / (n - 1f)
                            val x = cloth.x(i) * sim.figW
                            val y = cloth.y(i) * sim.figH
                            drawLine(
                                color = lerpColor(SCARF_A, SCARF_B, f).copy(alpha = 0.95f * (1f - 0.1f * f)),
                                start = Offset(prevX, prevY),
                                end = Offset(x, y),
                                strokeWidth = (sim.figW * 0.055f) * (1f - 0.5f * f),
                                cap = StrokeCap.Round,
                            )
                            prevX = x; prevY = y
                        }
                    }
                }

                val w = sim.figW
                val h = sim.figH
                if (w > 0f && h > 0f) {
                    ProfileWalker(
                        figW = w, figH = h, phase = walkPhase,
                        kneeN = sim.kneeN, kneeF = sim.kneeF, elbowN = sim.elbowN, elbowF = sim.elbowF,
                        bob = sim.bob, squashX = sim.squashX, squashY = sim.squashY, headSwayDeg = sim.headSwayDeg,
                        headSrc = headSrc, density = density,
                    )
                }

                // The scarf WRAP around the neck — a collar band + knot drawn ON TOP of the figure, at the
                // neck the cloth tail streams from, so it reads as a scarf tied round the neck (not a loose
                // streamer). The tail itself (drawn behind, above) is the real OGClothStrip cloth.
                if (scarf && sim.figW > 0f) {
                    Canvas(Modifier.fillMaxSize()) {
                        if (tick < 0) return@Canvas
                        val cxp = sim.neckX
                        val cyp = sim.neckY
                        val wc = sim.figW * 0.22f
                        val hc = sim.figH * 0.045f
                        drawOval(SCARF_A, topLeft = Offset(cxp - wc / 2f, cyp - hc / 2f), size = Size(wc, hc))
                        drawOval(SCARF_B.copy(alpha = 0.5f), topLeft = Offset(cxp - wc / 2f, cyp - hc * 0.05f), size = Size(wc, hc * 0.5f))
                        drawCircle(SCARF_B, radius = sim.figW * 0.04f, center = Offset(cxp - sim.figW * 0.02f, cyp + hc * 0.15f))
                    }
                }
            }

            if (petals) OGParticleView(spec = OGParticles.PETALS, modifier = Modifier.fillMaxSize(), playing = true)
        }

        Text("Effects", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            EffectChip("🌅 Sunset", sunset) { sunset = it }
            EffectChip("🏃 Dynamics (follow-through)", dynamics) { dynamics = it }
            EffectChip("🧣 Scarf", scarf) { scarf = it }
            EffectChip("🚩 Flag (cloth sheet)", scaffold) { scaffold = it }
            EffectChip("💨 Speed lines", speedLines) { speedLines = it }
            EffectChip("🌸 Petals", petals) { petals = it }
            EffectChip("👻 Trail", trail) { trail = it }
            EffectChip("✨ Glow", glow) { glow = it }
        }

        Text("Colour grade · ogLook", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("original") + OGLooks.presets.keys).forEach { name ->
                FilterChip(lookName == name, { lookName = name }, { Text(name) })
            }
        }
        Text(
            "The gait + rig are demo code (reusing the 🧍 Body-rig's FK Limb); 'Dynamics' off snaps the " +
                "joints to their drive angle (stiff, robotic), on springs them so the lower limbs lag + the " +
                "body squashes (OGSpring/OGSquash). The scarf is a real OGClothStrip (Verlet cloth — " +
                "inextensible, gravity + wind); the dressing (speed lines, trail, glow, petals, grade) is all " +
                "shipped library primitives. Same code Android & iOS.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/** Non-recomposing scene state advanced on the frame clock. */
private class SceneSim {
    var elapsed = 0f
    var figW = 0f
    var figH = 0f
    var neckX = 0f
    var neckY = 0f
    var headSwayDeg = 0f
    var kneeN = 0f
    var kneeF = 0f
    var elbowN = 0f
    var elbowF = 0f
    var bob = 0f
    var lastBob = 0f
    var squashX = 1f
    var squashY = 1f
}

@Composable
private fun EffectChip(label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    FilterChip(on, { onToggle(!on) }, { Text(label, fontSize = 12.sp) })
}

/**
 * A side-on (profile) walker facing screen-right. Two legs + two arms share a hip / shoulder pivot (near
 * in front, far dimmed behind); the upper segments swing with the stride [phase], the lower segments use
 * the spring-lagged [kneeN]/[elbowN] etc. (follow-through). The whole figure bobs ([bob]) and squashes
 * ([squashX]/[squashY]) around the pelvis. Built from the FK [Limb] of [BodyRigScreen].
 */
@Composable
private fun ProfileWalker(
    figW: Float, figH: Float, phase: Float,
    kneeN: Float, kneeF: Float, elbowN: Float, elbowF: Float,
    bob: Float, squashX: Float, squashY: Float, headSwayDeg: Float,
    headSrc: OGImageResourceFileType, density: Density,
) {
    fun px(v: Float) = with(density) { v.toDp() }

    val cx = figW * CX_FRAC
    val pelvisY = figH * PELVIS_Y_FRAC
    val shoulderY = figH * SHOULDER_Y_FRAC
    val torsoW = figW * 0.17f

    val legUpper = figH * 0.20f
    val legLower = figH * 0.19f
    val legW = figW * 0.075f
    val armUpper = figH * 0.15f
    val armLower = figH * 0.14f
    val armW = figW * 0.058f
    val headPx = figW * 0.27f

    // Upper-segment stride angles (facing right → forward swing is a negative Limb angle).
    val nearThigh = -STRIDE * sin(phase)
    val farThigh = STRIDE * sin(phase)
    val nearArm = ARM_SWING * sin(phase)   // arms counter-swing the same-side leg
    val farArm = -ARM_SWING * sin(phase)

    Box(
        Modifier.fillMaxSize()
            .offset { IntOffset(0, (bob * figH).roundToInt()) }
            .graphicsLayer {
                transformOrigin = TransformOrigin(cx / figW, pelvisY / figH)
                scaleX = squashX; scaleY = squashY
            },
    ) {
        val hip = Offset(cx, pelvisY)
        val shoulder = Offset(cx, shoulderY)

        // Far side (behind the torso), dimmed for depth.
        Limb(shoulder, armUpper, armLower, armW, farArm, elbowF, farArmBrush, density)
        Limb(hip, legUpper, legLower, legW, farThigh, kneeF, farLegBrush, density)

        // Torso — a rounded slab from shoulder to pelvis.
        Box(
            Modifier
                .offset { IntOffset((cx - torsoW / 2f).roundToInt(), shoulderY.roundToInt()) }
                .size(px(torsoW), px(pelvisY - shoulderY))
                .clip(RoundedCornerShape(percent = 46))
                .background(torsoBrush),
        )

        // Near side (in front).
        Limb(hip, legUpper, legLower, legW, nearThigh, kneeN, nearLegBrush, density)
        Limb(shoulder, armUpper, armLower, armW, nearArm, elbowN, nearArmBrush, density)

        // Head — the photo clipped to the traced lasso, pinned at the neck, bobbing + swaying.
        val neckFrac = LASSO_CHIN_FRAC
        Box(
            Modifier
                .offset {
                    IntOffset(
                        (cx + figW * 0.015f - headPx / 2f).roundToInt(),
                        (shoulderY + figH * 0.006f - neckFrac * headPx).roundToInt(),
                    )
                }
                .size(px(headPx), px(headPx))
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, neckFrac)
                    rotationZ = headSwayDeg
                },
        ) {
            OGImageView(
                source = headSrc,
                displayShape = OGShapeType.CIRCLE,
                clipShape = AVATAR_HEAD_OUTLINE,
                cornerRadius = px(headPx * 0.16f),
                contentScale = cropContentScale(crop = true, zoom = 1f),
                alignment = BiasAlignment(0f, 0f),
                modifier = Modifier.fillMaxSize(),
                onError = {},
                onEventTriggered = { _, _ -> },
            )
        }
    }
}

// ── Gait + geometry tuning ─────────────────────────────────────────────────────────────────
private val TAU = (2.0 * PI).toFloat()
private const val WALK_PERIOD_MS = 1150
private const val FIGURE_ASPECT = 0.74f
private const val CX_FRAC = 0.5f
private const val PELVIS_Y_FRAC = 0.56f
private const val SHOULDER_Y_FRAC = 0.33f
private const val STRIDE = 30f       // hip swing amplitude (deg)
private const val KNEE_MAX = 48f     // knee flex during swing (deg)
private const val ARM_SWING = 24f    // shoulder swing (deg)
private const val ELBOW_BASE = 12f   // resting elbow bend (deg)
private const val ELBOW_SW = 12f     // elbow swing (deg)
private const val BOB = 0.02f        // vertical bob (fraction of figH)
private const val SQUASH_K = 2.2f    // maps bob speed → squash intensity

// Flagpole geometry (normalized 0..1 of the scene box), top-left so the flag clears the figure.
private const val SUN_PERIOD_MS = 9000  // half-cycle of the sun's descent (day→night), then it reverses
private const val FLAG_POLE_X = 0.13f   // flagpole X
private const val FLAG_TOP = 0.08f      // pole top (finial)
private const val FLAG_POLE_B = 0.9f    // pole bottom
private const val FLAG_HOIST_TOP = 0.11f // where the flag's top edge pins to the pole
private const val FLAG_HOIST_BOT = 0.28f // where the flag's bottom edge pins to the pole

private val SCARF_A = Color(0xFF00E5FF) // at the neck — bright cyan, pops against the warm dusk
private val SCARF_B = Color(0xFFB388FF) // at the free end — violet
private val FLAG_A = Color(0xFFFF6B5C)  // flag, at the hoist — warm coral, reads against the sunset
private val FLAG_B = Color(0xFFB32B46)  // flag, toward the free edge — deep rose, shaded for depth

private val torsoBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF7B2FF7), Color(0xFF5A1FC0)))

private val nearArmBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF00C2A8), Color(0xFF008F7D)))

private val farArmBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF0A6F62), Color(0xFF0A5048)))

private val nearLegBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFFFF5DA2), Color(0xFFC23C79)))

private val farLegBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF9E3463), Color(0xFF6E2546)))

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

/** The sky gradient for a given sun progress [sp] (0 = day, 1 = night). */
private fun skyBrush(sp: Float): Brush = Brush.verticalGradient(
    listOf(
        lerpColor(Color(0xFF4C7AB8), Color(0xFF0E0A24), sp), // top: day blue → night near-black
        lerpColor(Color(0xFFB07AA0), Color(0xFF34203F), sp), // middle: warm → dusk purple
        lerpColor(Color(0xFFFFC27A), Color(0xFF6A2E34), sp), // horizon: bright → dim red
    ),
)

/** The whole-frame colour grade for a given sun progress [sp]: warm golden hour mid, cool + dim at night. */
private fun sunsetLook(sp: Float): OGLookSpec {
    val warm = sin(sp * PI).toFloat() // 0 at the ends, 1 at the middle (golden hour)
    return OGLookSpec(
        brightness = 0.06f - 0.30f * sp,
        contrast = 1f + 0.06f * sp,
        saturation = 1.05f - 0.28f * sp,
        temperature = 0.08f + 0.30f * warm - 0.26f * sp,
    )
}

/** A dusk backdrop: a sun that descends + reddens with [sp], over two scrolling parallax hill layers. */
private fun DrawScope.drawScene(elapsed: Float, sp: Float, w: Float, h: Float) {
    // The sun sinks (and drifts right) and shifts warm-white → orange → deep red as it sets.
    val sunX = (0.62f + 0.22f * sp) * w
    val sunY = (0.16f + 0.76f * sp) * h
    val sunCol = if (sp < 0.5f) lerpColor(Color(0xFFFFF4C8), Color(0xFFFF9A3D), sp * 2f)
    else lerpColor(Color(0xFFFF9A3D), Color(0xFFE03A28), (sp - 0.5f) * 2f)
    drawCircle(sunCol.copy(alpha = 0.30f), radius = w * (0.26f - 0.06f * sp), center = Offset(sunX, sunY))
    drawCircle(sunCol.copy(alpha = 0.90f), radius = w * 0.12f, center = Offset(sunX, sunY))
    // Land darkens into the night; hills are drawn in front of the sun, so it sets behind them.
    val farHill = lerpColor(Color(0xFF7A4A6A), Color(0xFF2A1838), sp)
    val nearHill = lerpColor(Color(0xFF4A2A52), Color(0xFF180E26), sp)
    val ground = lerpColor(Color(0xFF3A2048), Color(0xFF120A1E), sp)
    hillLayer(elapsed * 9f, w, h, baseY = h * 0.60f, amp = h * 0.03f, color = farHill, wavelen = w * 0.65f)
    hillLayer(elapsed * 24f, w, h, baseY = h * 0.72f, amp = h * 0.05f, color = nearHill, wavelen = w * 0.42f)
    drawRect(ground, topLeft = Offset(0f, h * 0.80f), size = Size(w, h * 0.20f))
}

private fun DrawScope.hillLayer(scroll: Float, w: Float, h: Float, baseY: Float, amp: Float, color: Color, wavelen: Float) {
    val path = Path()
    path.moveTo(0f, h)
    var x = 0f
    val step = w / 48f
    while (x <= w) {
        val y = baseY + amp * sin((x + scroll) / wavelen * TAU)
        path.lineTo(x, y)
        x += step
    }
    path.lineTo(w, h)
    path.close()
    drawPath(path, color)
}
