package com.solidkey.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.motion.OGDynamics
import com.solidkey.painpoints.motion.OGFollowChain
import com.solidkey.painpoints.motion.OGSpringValue
import com.solidkey.painpoints.motion.OGSquash
import com.solidkey.painpoints.motion.OGSway
import com.solidkey.painpoints.shape.OGPoint
import kotlin.math.atan2
import kotlin.math.sqrt

private val STAGE = Color(0xFF0B1018)
private val GOLD = Color(0xFFFFC107)
private val ROBO = Color(0xFF9E9E9E)
private val TARGET = Color(0xFF40C4FF)

/** Mutable, non-recomposing simulation state, advanced on the frame clock. */
private class DynSim(seed: OGPoint) {
    var elapsed = 0f
    var lastJump = 0f
    var idx = 0
    var linX = seed.x
    var linY = seed.y
}

/**
 * 🌀 Motion dynamics — the dogfood for the pure-maths "feel" layer of com.solidkey.painpoints.motion.
 * The SAME target (blue ring) jumps around; two followers chase it on one frame clock:
 *  • a robotic twin that moves at constant speed and stops dead (no library dynamics), and
 *  • an alive one driven by OGSpring (lag + overshoot + settle), OGSquash (stretch into the move),
 *    an OGFollowChain comet tail (follow-through) and OGSway (idle wander at rest).
 * Pick a feel (OGDynamics.presets); every number is plain data, pure maths, 60fps, same code both platforms.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicsScreen() {
    // A fixed tour of targets (no randomness → deterministic + capture-friendly).
    val targets = remember {
        listOf(
            OGPoint(0.22f, 0.28f), OGPoint(0.80f, 0.26f), OGPoint(0.78f, 0.74f),
            OGPoint(0.26f, 0.72f), OGPoint(0.52f, 0.46f),
        )
    }

    var presetName by remember { mutableStateOf("bouncy") }
    val spec = remember(presetName) { OGDynamics.preset(presetName) ?: OGDynamics.presets.values.first() }

    val springX = remember { OGSpringValue(targets[0].x) }
    val springY = remember { OGSpringValue(targets[0].y) }
    val chain = remember { OGFollowChain(7) }
    val sway = remember { OGSway(amplitude = 1f, frequency = 0.5f) }
    val sim = remember { DynSim(targets[0]) }
    var tick by remember { mutableStateOf(0) }

    // Re-feel the follow chain when the preset changes (whippier/tighter tail).
    LaunchedEffect(presetName) { chain.spec = spec.spring }

    // One frame clock drives both followers; dt comes from real frame deltas (clamped inside the lib).
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = (now - last) / 1_000_000_000f
                    sim.elapsed += dt
                    if (sim.elapsed - sim.lastJump >= 1.5f) {
                        sim.lastJump = sim.elapsed
                        sim.idx = (sim.idx + 1) % targets.size
                    }
                    val tgt = targets[sim.idx]

                    // Alive: spring toward the target, tail follows the ball.
                    springX.update(tgt.x, dt, spec.spring)
                    springY.update(tgt.y, dt, spec.spring)
                    chain.update(springX.value, springY.value, dt)

                    // Robotic: march at constant speed, stop dead on arrival.
                    val dx = tgt.x - sim.linX
                    val dy = tgt.y - sim.linY
                    val d = sqrt(dx * dx + dy * dy)
                    val step = 0.9f * dt
                    if (d <= step || d < 1e-4f) {
                        sim.linX = tgt.x; sim.linY = tgt.y
                    } else {
                        sim.linX += dx / d * step; sim.linY += dy / d * step
                    }
                    tick++
                }
                last = now
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("🌀 Robotic → alive · OGSpring + OGSquash + OGFollowChain + OGSway")
        Caption("One moving target (blue ring). The grey square chases it with plain constant-speed motion — arrives and stops dead. The gold ball is fed through the dynamics layer: it lags, overshoots and settles (spring), stretches into the move (squash), trails a comet tail (follow-through) and drifts a little when idle (sway).")

        Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(14.dp)).background(STAGE)) {
            Canvas(Modifier.fillMaxSize()) {
                if (tick < 0) return@Canvas // subscribe to the frame tick so we redraw each frame
                val w = size.width
                val h = size.height
                val r = 16f

                // Target ring.
                val tgt = targets[sim.idx]
                drawCircle(TARGET, radius = 20f, center = Offset(tgt.x * w, tgt.y * h), style = Stroke(width = 3f))
                drawCircle(TARGET.copy(alpha = 0.5f), radius = 4f, center = Offset(tgt.x * w, tgt.y * h))

                // Robotic twin.
                val rx = sim.linX * w
                val ry = sim.linY * h
                drawRect(ROBO, topLeft = Offset(rx - r, ry - r), size = Size(2 * r, 2 * r))

                // Alive: comet tail (chain links, fading + shrinking from the ball backwards).
                for (i in chain.count - 1 downTo 0) {
                    val f = 1f - i.toFloat() / chain.count
                    drawCircle(
                        GOLD.copy(alpha = 0.10f + 0.35f * f),
                        radius = r * (0.35f + 0.5f * f),
                        center = Offset(chain.x(i) * w, chain.y(i) * h),
                    )
                }

                // Alive ball: spring position + idle sway, stretched along its direction of travel (squash).
                val vx = springX.velocity
                val vy = springY.velocity
                val speed = sqrt(vx * vx + vy * vy)
                val settle = (1f - speed).coerceIn(0f, 1f) // sway only shows once it has calmed
                val off = sway.offset(sim.elapsed)
                val cx = (springX.value + off.x * spec.sway * 0.03f * settle) * w
                val cy = (springY.value + off.y * spec.sway * 0.03f * settle) * h
                val sc = OGSquash.fromSpeed(speed, intensity = 0.06f + spec.squash * 0.14f, max = 0.45f)
                val along = r * sc.scaleY
                val cross = r * sc.scaleX
                val angle = atan2(vy, vx) * 180f / 3.1415927f
                rotate(angle, pivot = Offset(cx, cy)) {
                    drawOval(GOLD, topLeft = Offset(cx - along, cy - cross), size = Size(2 * along, 2 * cross))
                }
            }
            Legend()
        }

        SectionTitle("Feel · OGDynamics.presets")
        Caption("Each feel is a plain OGDynamicsSpec (spring stiffness/damping, squash, sway, follow). A model can author it from a sentence via OGDynamics.dynamicsPrompt — the same data pattern as OGLooks / OGMotions / OGStyles.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OGDynamics.presets.keys.forEach { name ->
                FilterChip(presetName == name, { presetName = name }, { Text(name) })
            }
        }
        Caption(
            "stiffness=${spec.spring.stiffness.toInt()} · damping=${fmt2(spec.spring.dampingRatio)} · " +
                "squash=${fmt2(spec.squash)} · sway=${fmt2(spec.sway)}. " +
                "Overshoot comes from damping < 1; a stiffer spring settles faster. See docs/MOTION.md.",
        )
    }
}

@Composable
private fun Legend() {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        LegendRow(GOLD, "alive · OGSpring + squash + tail + sway")
        LegendRow(ROBO, "robotic · constant-speed, stops dead")
        LegendRow(TARGET, "target")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text("  $label", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
    }
}

private fun fmt2(v: Float): String {
    val r = (v * 100f + 0.5f).toInt()
    return "${r / 100}.${(r % 100).toString().padStart(2, '0')}"
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}
