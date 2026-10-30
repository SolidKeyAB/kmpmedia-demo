package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.loading.loadImageFromResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 🦸 **SUPER X Rig Studio** — Özge's hand-drawn hero as a MOVABLE puppet, built the honest way.
 *
 * The whole clean flying DRAWING is the body ([superx_fig]) — so the head, torso, cape, right arm and
 * legs are ALWAYS correctly joined (it is the original art, untouched). Only the raised-fist left arm
 * is a separate layer ([superx_fig_arml]), cut out of that same drawing — so it sockets back exactly
 * where it belongs and swings from the real shoulder (a forward-kinematics `graphicsLayer {
 * transformOrigin = <shoulder>; rotationZ = angle }`). Pick a move (Idle / Wave / Fly / Dance): the
 * whole figure floats + banks and the fist pumps. The Rig-parts sliders nudge where a layer sits on
 * its joint (the "move the parts on layers" tool); Export dumps the layout to bake. Pure commonMain
 * over the lib's resource loader + Compose transforms; identical on Android & iOS.
 *
 * The parts are cut from ONE coherent drawing, not a study sheet, which is why the joints line up:
 * a flat drawing can't be auto-split into limbs, but cutting a limb that projects into empty space
 * (the raised fist) out of the finished pose gives a clean, correctly-registered movable part.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SuperXRigScreen() {
    val density = LocalDensity.current
    var stagePx by remember { mutableStateOf(IntSize.Zero) }

    val parts = remember { defaultRig() }
    var selected by remember { mutableStateOf("arm_l") }
    var move by remember { mutableStateOf(SxMove.Fly) }
    var showExport by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "sx")
    val phase by infinite.animateFloat(
        initialValue = 0f, targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(move.periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val pose = move.pose(phase)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("🦸 SUPER X Rig Studio", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Your drawn hero as a movable puppet. The whole flying drawing is the body — head, torso, cape, " +
                "legs stay joined — and the raised fist is a separate layer cut from the SAME drawing, so it " +
                "sockets back exactly and swings from the real shoulder. Pick a move; nudge a layer with Rig " +
                "parts; Export to bake.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        // ── The stage (landscape — matches the drawing so the arm registers 1:1) ──────
        Box(
            Modifier.fillMaxWidth().aspectRatio(FIG_W / FIG_H).clip(RoundedCornerShape(18.dp))
                .background(sxStageBrush).onSizeChanged { stagePx = it },
        ) {
            val w = stagePx.width.toFloat(); val h = stagePx.height.toFloat()
            if (w > 0f && h > 0f) {
                // Whole-figure float + bank about the pelvis; the fist pumps on top.
                Box(
                    Modifier.fillMaxSize()
                        .offset { IntOffset(0, (pose.bob * h).roundToInt()) }
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.92f)
                            rotationZ = pose.sway
                        },
                ) {
                    PartSprite(parts.getValue("fig"), "superx_fig", w, h, FIG_ASPECT, 0f, density)
                    PartSprite(parts.getValue("arm_l"), "superx_fig_arml", w, h, ARML_ASPECT, pose.armL, density)
                }
            }
        }

        // ── Moves ────────────────────────────────────────────────────────────────────
        Text("Move", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SxMove.entries.forEach { m ->
                FilterChip(selected = move == m, onClick = { move = m },
                    leadingIcon = { Text(m.glyph, fontSize = 14.sp) }, label = { Text(m.label) })
            }
        }

        // ── Rig parts (the "move the parts on layers" tool) ──────────────────────────
        Text("Rig parts — nudge the selected layer", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.keys.forEach { id ->
                FilterChip(selected = selected == id, onClick = { selected = id }, label = { Text(id) })
            }
        }
        val xf = parts.getValue(selected)
        EditRow("x", xf.x, 0f, 1f) { xf.x = it }
        EditRow("y", xf.y, 0f, 1f) { xf.y = it }
        EditRow("size", xf.scale, 0.05f, 1.2f) { xf.scale = it }
        EditRow("rotate ${xf.rot.roundToInt()}°", (xf.rot + 180f) / 360f, 0f, 1f) { xf.rot = it * 360f - 180f }
        EditRow("pivot x", xf.pivX, 0f, 1f) { xf.pivX = it }
        EditRow("pivot y", xf.pivY, 0f, 1f) { xf.pivY = it }
        Text(
            "x/y = where the layer's PIVOT sits on the stage · size = width as a fraction of the stage · " +
                "rotate = resting angle · pivot = the joint it turns around (the move adds its swing on top).",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )

        // ── Export ───────────────────────────────────────────────────────────────────
        FilterChip(selected = showExport, onClick = { showExport = !showExport },
            label = { Text(if (showExport) "Hide rig JSON" else "⬇ Export rig") })
        if (showExport) {
            SelectionContainer {
                Text(
                    exportRig(parts, move.name),
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp),
                )
            }
            Text("Long-press to copy, then paste it back to me and I'll bake the loop.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "The rig + every move are pure commonMain demo code; the only library primitive is the resource " +
                "loader that places each drawn layer. We move YOUR drawing, not a redraw of it. Same code on " +
                "Android & iOS.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/** One rig layer: the drawn [resource] placed so its pivot sits at ([xf].x,[xf].y) of the stage,
 *  sized to [xf].scale of the stage width, turned by its resting [xf].rot + the move's [artRot] about
 *  the pivot. [aspect] = part pixel height / width (to size it without distortion). */
@Composable
private fun PartSprite(
    xf: PartXf, resource: String, stageW: Float, stageH: Float, aspect: Float, artRot: Float, density: Density,
) {
    var painter by remember(resource) { mutableStateOf<Painter?>(null) }
    loadImageFromResource(resource, onError = {}, onLoaded = { painter = it })
    val p = painter ?: return
    fun px(v: Float) = with(density) { v.toDp() }
    val partW = xf.scale * stageW
    val partH = partW * aspect
    Box(
        Modifier
            .offset { IntOffset((xf.x * stageW - xf.pivX * partW).roundToInt(), (xf.y * stageH - xf.pivY * partH).roundToInt()) }
            .size(px(partW), px(partH))
            .graphicsLayer {
                transformOrigin = TransformOrigin(xf.pivX, xf.pivY)
                rotationZ = xf.rot + artRot
            },
    ) {
        Image(p, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun EditRow(label: String, value: Float, min: Float, max: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 11.sp, modifier = Modifier.width(78.dp))
        Slider(value = value.coerceIn(min, max), onValueChange = onChange, valueRange = min..max, modifier = Modifier.weight(1f))
    }
}

// ── Editable rig state ──────────────────────────────────────────────────────────────────

private class PartXf(x: Float, y: Float, scale: Float, rot: Float, pivX: Float, pivY: Float) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var scale by mutableStateOf(scale)
    var rot by mutableStateOf(rot)
    var pivX by mutableStateOf(pivX)
    var pivY by mutableStateOf(pivY)
}

// The source drawing is 1024×572; the stage matches that aspect so the arm registers 1:1 with the body.
private const val FIG_W = 1024f
private const val FIG_H = 572f
private const val FIG_ASPECT = FIG_H / FIG_W            // body fills the stage
private const val ARML_ASPECT = 112f / 124f            // left-arm cut aspect (h/w)

/** Default layout. The body fills the stage; the left arm sits where it was cut (shoulder = its pivot),
 *  all in the drawing's normalized coords, so it lines up exactly. */
private fun defaultRig(): LinkedHashMap<String, PartXf> = linkedMapOf(
    //  id        x        y        size    rot  pivX    pivY
    "fig" to PartXf(0.5f, 0.5f, 1.0f, 0f, 0.5f, 0.5f),
    // left raised fist: cut box (332,130)-(456,242) of 1024×572; shoulder ≈ (440,150).
    "arm_l" to PartXf(440f / 1024f, 150f / 572f, 124f / 1024f, 0f, 0.871f, 0.179f),
)

private fun exportRig(parts: Map<String, PartXf>, move: String): String {
    val body = parts.entries.joinToString(",\n") { (id, p) ->
        "  \"$id\": { \"x\": ${f(p.x)}, \"y\": ${f(p.y)}, \"size\": ${f(p.scale)}, " +
            "\"rot\": ${f(p.rot)}, \"pivX\": ${f(p.pivX)}, \"pivY\": ${f(p.pivY)} }"
    }
    return "{\n  \"rig\": \"superx\", \"v\": 2, \"move\": \"$move\",\n$body\n}"
}

private fun f(v: Float): String = ((v * 1000f).roundToInt() / 1000f).toString()

// ── Pose model + moves (the raised fist turns at the shoulder; the whole figure floats) ─────

private data class SxPose(
    val armL: Float,   // left-arm rotation about the shoulder (deg)
    val bob: Float,    // whole-figure float (fraction of stage height; up = negative)
    val sway: Float,   // whole-figure bank about the pelvis (deg)
)

private const val TWO_PI = (2.0 * PI).toFloat()

private enum class SxMove(val label: String, val glyph: String, val periodMs: Int) {
    Idle("Idle", "🧘", 3400) {
        override fun pose(p: Float): SxPose {
            val s = sin(p)
            return SxPose(armL = 4f * s, bob = -0.01f * (0.5f + 0.5f * s), sway = 1.2f * s)
        }
    },
    Wave("Wave", "👋", 760) {
        override fun pose(p: Float): SxPose {
            // The raised fist pumps up and down from the shoulder.
            return SxPose(armL = -18f + 22f * (0.5f + 0.5f * sin(p)), bob = -0.004f * sin(p * 0.5f), sway = 0f)
        }
    },
    Fly("Fly", "🚀", 2600) {
        override fun pose(p: Float): SxPose {
            val s = sin(p)
            return SxPose(armL = 6f * s, bob = -0.035f * (0.5f + 0.5f * s), sway = 3.5f * s)
        }
    },
    Dance("Dance", "🕺", 1200) {
        override fun pose(p: Float): SxPose {
            val a = sin(p)
            return SxPose(armL = 30f * a, bob = -0.014f * abs(sin(p * 2f)), sway = 8f * a)
        }
    };

    abstract fun pose(p: Float): SxPose
}

/** A calm dark stage so the hand-drawn hero pops. */
private val sxStageBrush: Brush
    get() = Brush.verticalGradient(listOf(Color(0xFF121833), Color(0xFF241A42)))
