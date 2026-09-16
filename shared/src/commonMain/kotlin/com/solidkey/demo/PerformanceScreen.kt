package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.image.loading.OGSvgResourceFileType
import com.solidkey.painpoints.image.svg.OGSVGView
import com.solidkey.painpoints.image.svg.SVGScalingBehavior
import com.solidkey.painpoints.image.svg.animation.OGSVGAnimationPlayer
import kotlin.math.roundToInt

private enum class PerfKind(val label: String) {
    ANIMATED("Animated SVG"), STATIC_SVG("Static SVG"), IMAGE("Image")
}

/**
 * Live rendering benchmark. Render N library sprites and watch the frame rate.
 * Animated SVGs invalidate every frame (SMIL), so they are the heaviest — a real
 * throughput signal for the library's vector renderer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen() {
    var count by remember { mutableStateOf(8f) }
    var kind by remember { mutableStateOf(PerfKind.ANIMATED) }

    var fps by remember { mutableStateOf(0f) }
    var frameMs by remember { mutableStateOf(0f) }
    var maxMs by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        var acc = 0f
        var frames = 0
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val ms = (now - last) / 1_000_000f
                    frameMs = ms
                    if (ms > maxMs) maxMs = ms
                    acc += ms
                    frames++
                    if (acc >= 500f) {
                        fps = frames * 1000f / acc
                        acc = 0f
                        frames = 0
                    }
                }
                last = now
            }
        }
    }

    val n = count.roundToInt()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        // HUD
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Stat("FPS", fps.roundToInt().toString(), if (fps >= 50) Color(0xFF2E7D32) else if (fps >= 30) Color(0xFFF9A825) else Color(0xFFC62828))
            Stat("frame", "${frameMs.round1()} ms", MaterialTheme.colorScheme.onSurface)
            Stat("worst", "${maxMs.round1()} ms", MaterialTheme.colorScheme.onSurface)
            Stat("sprites", n.toString(), MaterialTheme.colorScheme.onSurface)
        }

        Spacer(Modifier.height(12.dp))
        Text("Sprite type", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerfKind.entries.forEach { k ->
                FilterChip(
                    selected = kind == k,
                    onClick = { kind = k; maxMs = 0f },
                    label = { Text(k.label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Count: $n", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Slider(
            value = count,
            onValueChange = { count = it; maxMs = 0f },
            valueRange = 1f..48f
        )

        Spacer(Modifier.height(8.dp))
        // Sprite field — renders ALL n sprites (not lazily) so the load is real.
        val cols = 6
        val cell = 56.dp
        Column {
            var i = 0
            while (i < n) {
                Row {
                    var c = 0
                    while (c < cols && i < n) {
                        Box(Modifier.size(cell).padding(2.dp)) {
                            PerfSprite(kind)
                        }
                        c++; i++
                    }
                }
            }
        }
    }
}

@Composable
private fun PerfSprite(kind: PerfKind) {
    val px = 120f
    when (kind) {
        PerfKind.ANIMATED -> OGSVGAnimationPlayer(
            source = remember { OGSvgResourceFileType("spin") },
            width = px, height = px,
            modifier = Modifier.size(52.dp),
            isPlaying = true, loop = true,
            // CLIP renders the vector at true scale; FIT double-applies the viewBox
            // scale and shrinks the sprite into the corner.
            scalingBehavior = SVGScalingBehavior.CLIP,
            onError = { }
        )
        PerfKind.STATIC_SVG -> OGSVGView(
            source = remember { OGSvgResourceFileType("atom") },
            width = px, height = px,
            modifier = Modifier.size(52.dp),
            scalingBehavior = SVGScalingBehavior.FIT,
            onError = { }
        )
        PerfKind.IMAGE -> OGImageView(
            source = remember { OGImageResourceFileType("happy_icon", OGImageFormat.PNG) },
            modifier = Modifier.size(52.dp),
            onError = { },
            onEventTriggered = { _, _ -> }
        )
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.wrapContentWidth()) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Float.round1(): String {
    val v = (this * 10).roundToInt()
    return "${v / 10}.${v % 10}"
}
