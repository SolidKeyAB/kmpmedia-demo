package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.depth.OGParallaxConfig
import com.solidkey.painpoints.depth.ogParallax
import kotlin.math.roundToInt

/**
 * Dogfoods 1.21.0 parallax. A little scene whose elements sit at different depths; drag inside the
 * window (or tap a preset) to move the "viewpoint" and watch the layers slide by different amounts —
 * far pinned, near moving most — via Modifier.ogParallax. One GPU graphicsLayer per layer, 60fps.
 */
@Composable
fun ParallaxScreen() {
    var viewpoint by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Parallax: each layer sits at a different depth. Drag inside the window to move the " +
                "viewpoint — the far sky stays put, nearer layers slide more. Modifier.ogParallax on a " +
                "single GPU graphicsLayer, built on the depth axis.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF42A5F5))))
                .pointerInput(Unit) {
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    detectDragGestures { change, drag ->
                        change.consume()
                        viewpoint = Offset(
                            (viewpoint.x + drag.x / w * 2.5f).coerceIn(-1f, 1f),
                            (viewpoint.y + drag.y / h * 2.5f).coerceIn(-1f, 1f),
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // depth 0 = far (sky, pinned) → depth 1 = near (foreground, moves most)
            Layer("✦ ✦ ✦ ✦ ✦", depth = 0.0f, viewpoint, align = Alignment.TopCenter, dy = 24.dp, size = 20.sp)
            Layer("☁️        ☁️", depth = 0.25f, viewpoint, align = Alignment.TopCenter, dy = 70.dp, size = 34.sp)
            Layer("⛰️ ⛰️ ⛰️", depth = 0.5f, viewpoint, align = Alignment.Center, dy = 20.dp, size = 56.sp)
            Layer("🌲   🌲   🌲   🌲", depth = 0.78f, viewpoint, align = Alignment.BottomCenter, dy = (-40).dp, size = 40.sp)
            Layer("🦊", depth = 1.0f, viewpoint, align = Alignment.BottomCenter, dy = (-16).dp, size = 48.sp)
        }

        Text(
            "viewpoint = (${(viewpoint.x * 100).roundToInt() / 100f}, ${(viewpoint.y * 100).roundToInt() / 100f})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Button(onClick = { viewpoint = Offset(-1f, 0f) }, modifier = Modifier.fillMaxWidth()) { Text("Look left") }
        Button(onClick = { viewpoint = Offset(1f, 0f) }, modifier = Modifier.fillMaxWidth()) { Text("Look right") }
        Button(onClick = { viewpoint = Offset.Zero }, modifier = Modifier.fillMaxWidth()) { Text("Center") }
    }
}

@Composable
private fun Layer(
    text: String,
    depth: Float,
    viewpoint: Offset,
    align: Alignment,
    dy: androidx.compose.ui.unit.Dp,
    size: androidx.compose.ui.unit.TextUnit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = align) {
        Text(
            text,
            fontSize = size,
            modifier = Modifier
                .offset(y = dy)
                .ogParallax(depth = depth, viewpoint = viewpoint, config = OGParallaxConfig(maxShift = 60.dp)),
        )
    }
}
