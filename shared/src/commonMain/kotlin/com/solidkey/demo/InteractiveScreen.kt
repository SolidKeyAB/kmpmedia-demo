package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.gesture.OGHitArea
import com.solidkey.painpoints.gesture.OGInteractionConfig
import com.solidkey.painpoints.gesture.ogInteractive
import com.solidkey.painpoints.gesture.rememberOGInteractionState
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.shape.DiamondShape
import com.solidkey.painpoints.shape.OGPoint
import com.solidkey.painpoints.shape.OGPolygonShape
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.shape.TriangleDirection
import com.solidkey.painpoints.shape.TriangleShape
import com.solidkey.painpoints.shape.toShape
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

// A 5-pointed star lasso in normalized 0..1 space — shows hit-testing on an arbitrary polygon.
private val IxStarLasso: OGPolygonShape = OGPolygonShape(
    (0 until 10).map { i ->
        val r = if (i % 2 == 0) 0.5f else 0.21f
        val a = -PI / 2 + i * PI / 5
        OGPoint((0.5f + r * cos(a)).toFloat(), (0.5f + r * sin(a)).toFloat())
    },
)

private enum class IxShape(val label: String, val clip: Shape, val hit: OGHitArea) {
    Circle("● Circle", OGShapeType.CIRCLE.toShape(), OGHitArea.CIRCLE),
    Triangle("▲ Triangle", TriangleShape(TriangleDirection.UP), OGHitArea.TRIANGLE_UP),
    Diamond("◆ Diamond", DiamondShape(), OGHitArea.DIAMOND),
    Star("★ Star lasso", IxStarLasso, OGHitArea.polygon(IxStarLasso)),
}

private enum class IxMode(val label: String, val config: OGInteractionConfig) {
    Drag("Drag", OGInteractionConfig.DragOnly.copy(maxPanFractionX = 0.75f, maxPanFractionY = 0.75f)),
    PanZoom("Pan + zoom", OGInteractionConfig.PanZoom.copy(maxPanFractionX = 0.75f, maxPanFractionY = 0.75f)),
    All("All (+ rotate)", OGInteractionConfig.All.copy(maxPanFractionX = 0.75f, maxPanFractionY = 0.75f)),
}

/**
 * **Interactive shapes** — dogfoods the new `com.solidkey.painpoints.gesture` primitives:
 * `Modifier.ogInteractive` + `OGInteractionState` + `OGHitArea`. A sample photo is clipped to a
 * shape; grab it to drag, pinch to zoom, twist to rotate — then let go and it flings on and springs
 * back inside the stage. With **shape-aware touch** on, a drag only starts if your finger lands
 * inside the actual silhouette (try grabbing a corner of the triangle/star — nothing happens).
 * Zero library change beyond consuming the new API; identical on Android & iOS.
 */
@Composable
fun InteractiveScreen() {
    var shape by remember { mutableStateOf(IxShape.Circle) }
    var mode by remember { mutableStateOf(IxMode.All) }
    var shapeAware by remember { mutableStateOf(true) }

    val state = rememberOGInteractionState(mode.config)
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Interactive shapes", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Grab the shaped photo to drag it, pinch to zoom, twist to rotate — then fling it and " +
                "watch it spring back. One Modifier.ogInteractive does the transform, the momentum " +
                "fling and the spring settle. With shape-aware touch on, only touches inside the " +
                "actual silhouette grab it.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        // ── The stage ────────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF0B1020)),
            contentAlignment = Alignment.Center,
        ) {
            OGImageView(
                source = OGImageResourceFileType("avatar_ozge", OGImageFormat.JPEG),
                clipShape = shape.clip,
                modifier = Modifier
                    .size(200.dp)
                    .ogInteractive(state, hitArea = if (shapeAware) shape.hit else null),
                onError = {},
                onEventTriggered = { _, _ -> },
            )
        }

        IxReadout(state)

        // ── Controls ─────────────────────────────────────────────────────────────────────
        Text("Shape", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        IxChoiceRow(IxShape.entries, shape, { it.label }) { shape = it }

        Text("Gestures", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        IxChoiceRow(IxMode.entries, mode, { it.label }) { mode = it }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = shapeAware, onCheckedChange = { shapeAware = it })
            Spacer(Modifier.size(8.dp))
            Text(
                if (shapeAware) "Shape-aware touch: grab only inside the silhouette"
                else "Shape-aware touch off: the whole box grabs",
                fontSize = 13.sp,
            )
        }

        Button(onClick = { scope.launch { state.reset() } }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset")
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Under the hood: com.solidkey.painpoints.gesture — Modifier.ogInteractive(state, hitArea) " +
                "applies the pan / zoom / rotation through one graphicsLayer and drives it from a " +
                "custom awaitEachGesture loop (momentum fling via exponentialDecay, spring settle, " +
                "pan bounds). OGHitArea ray-casts the touch against the clip silhouette so transparent " +
                "corners fall through. Pure math (hit-test, clamps) is unit-tested on JVM + iOS; the " +
                "transform rides the GPU layer, so it holds 60fps. Same code on Android & iOS.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/** Live transform HUD — isolated so only it recomposes as the Animatables change each frame. */
@Composable
private fun IxReadout(state: com.solidkey.painpoints.gesture.OGInteractionState) {
    val o = state.offset.value
    val s = state.scale.value
    val r = state.rotation.value
    Text(
        "offset ${round(o.x).toInt()}, ${round(o.y).toInt()} px   ·   " +
            "scale ${(round(s * 100) / 100)}×   ·   rotation ${round(r).toInt()}°",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> IxChoiceRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEach { item ->
            if (item == selected) {
                Button(onClick = { onPick(item) }) { Text(label(item), fontSize = 12.sp) }
            } else {
                OutlinedButton(onClick = { onPick(item) }) { Text(label(item), fontSize = 12.sp) }
            }
        }
    }
}
