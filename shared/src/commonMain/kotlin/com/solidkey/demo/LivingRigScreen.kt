package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGJoint
import com.solidkey.painpoints.ai.OGPlacedShapeSpec
import com.solidkey.painpoints.ai.OGPointSpec
import com.solidkey.painpoints.ai.OGRig
import com.solidkey.painpoints.ai.OGSceneSpec
import com.solidkey.painpoints.stage.OGSceneRigView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 🫳 Living vector — a compiled illustration becomes grabbable BY PART. OGRig.sceneJoints derives one
 * semantic pin per structural region (body / head / arm / tail); OGSceneRigView binds the mesh once,
 * warps it live under your finger (linear-blend skinning) and springs each pin home. Flip the switch and
 * the SAME rig is driven by code instead — a scripted wave — showing the new pose param. Pure maths,
 * 60fps, identical on Android & iOS.
 */
@Composable
fun LivingRigScreen() {
    val scene = remember { buildCreature() }
    val base = remember(scene) { OGRig.sceneJoints(scene) }
    val armIdx = remember(base) {
        base.indices.minByOrNull { i ->
            val p = base[i].rest
            val dx = p.x - 0.26f; val dy = p.y - 0.48f
            dx * dx + dy * dy
        } ?: -1
    }
    var codeDriven by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("🫳 Poke the creature · OGSceneRigView + OGRig.sceneJoints")
        Caption(
            if (codeDriven) {
                "Code-driven mode: your own animation owns the deform. A fresh pose (the arm joint nudged by a " +
                    "sine wave) is handed to OGSceneRigView each frame — the rig binds once, only the cheap " +
                    "re-tessellate runs. The same seam feeds a gaze that follows or a squash on a score."
            } else {
                "Touch mode: drag any part — the body, the head, the arm, the tail — and the mesh warps under " +
                    "your finger, then springs home when you let go. The pins are semantic (one per region), not a blind grid."
            },
        )

        val pose: List<OGJoint>? = if (codeDriven && armIdx >= 0) {
            val t = rememberInfiniteTransition(label = "wave")
            val phase by t.animateFloat(
                initialValue = 0f, targetValue = (2f * PI).toFloat(),
                animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
                label = "phase",
            )
            val dx = sin(phase) * 0.12f
            val dy = -0.035f - cos(phase) * 0.015f
            base.mapIndexed { i, j -> if (i == armIdx) j.copy(offset = OGPointSpec(dx, dy)) else j }
        } else {
            null
        }

        Box(
            Modifier.fillMaxWidth().aspectRatio(0.9f).clip(RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            OGSceneRigView(
                scene = scene,
                modifier = Modifier.fillMaxSize(),
                joints = base,
                showHandles = !codeDriven,
                pose = pose,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(codeDriven, { codeDriven = it })
            Text(
                if (codeDriven) "  code-driven (auto wave)" else "  touch (drag a part)",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        Caption(
            "This is the one capability a rasterise-once cache can't fake: while a pin moves, every vertex is " +
                "re-skinned and the mesh re-drawn. The rig scaffold also serializes — author, save or " +
                "model-describe the joints with OGRigSpec / OGRigs.",
        )
    }
}

/** A tiny hand-built creature: labelled elliptical regions so each structural part gets its own pin. */
private fun buildCreature(): OGSceneSpec = OGSceneSpec(
    shapes = listOf(
        OGPlacedShapeSpec(points = ellipse(0.74f, 0.74f, 0.07f, 0.11f), label = "tail", fill = "#D98A2B"),
        OGPlacedShapeSpec(points = ellipse(0.5f, 0.66f, 0.22f, 0.26f), label = "body", fill = "#E8A33D"),
        OGPlacedShapeSpec(points = ellipse(0.5f, 0.33f, 0.17f, 0.17f), label = "head", fill = "#F0B050"),
        OGPlacedShapeSpec(points = ellipse(0.44f, 0.31f, 0.03f, 0.035f), label = "eye", fill = "#2A2320"),
        OGPlacedShapeSpec(points = ellipse(0.56f, 0.31f, 0.03f, 0.035f), label = "eye", fill = "#2A2320"),
        OGPlacedShapeSpec(points = ellipse(0.5f, 0.385f, 0.025f, 0.02f), label = "nose", fill = "#B5701E"),
        OGPlacedShapeSpec(points = ellipse(0.26f, 0.48f, 0.08f, 0.13f), label = "arm", fill = "#E8A33D"),
    ),
    background = "#F3EEE4",
)

private fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, n: Int = 28): List<OGPointSpec> =
    (0 until n).map { i ->
        val a = (i.toFloat() / n) * 2f * PI.toFloat()
        OGPointSpec(cx + rx * cos(a), cy + ry * sin(a))
    }

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}
