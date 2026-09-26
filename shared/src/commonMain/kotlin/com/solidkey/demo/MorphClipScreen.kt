package com.solidkey.demo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageUrlType
import com.solidkey.painpoints.shape.DiamondShape
import com.solidkey.painpoints.shape.OGPoint
import com.solidkey.painpoints.shape.OGPolygonShape
import com.solidkey.painpoints.shape.TriangleDirection
import com.solidkey.painpoints.shape.TriangleShape
import com.solidkey.painpoints.shape.ogMorphSequence
import com.solidkey.painpoints.video.loading.OGVideoUrlType
import com.solidkey.painpoints.video.playing.OGAVPlayer
import com.solidkey.painpoints.video.playing.OGAVPlayerAction
import com.solidkey.painpoints.video.playing.OGPlayerConfig
import com.solidkey.painpoints.video.playing.OGVideoPlaybackConfig
import com.solidkey.painpoints.video.playing.OGVideoScale
import com.solidkey.painpoints.video.playing.rememberOGAVPlayerController
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 1280x720, fills the frame (no baked letterbox) so FILL crops cleanly into the morphing mask.
private const val MORPH_VIDEO_URL =
    "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"

// A looping animated GIF (Wikimedia Commons) — proves the SAME morphing clip works on moving frames.
private const val MORPH_GIF_URL =
    "https://upload.wikimedia.org/wikipedia/commons/e/e3/Animhorse.gif"

/** Human-readable names for the clip's morph stops (parallel to the shapes list below). */
private val STOP_NAMES = listOf("Circle", "Diamond", "Triangle", "Lasso", "Circle")

/**
 * A free-form 5-point **star** lasso in normalized 0..1 space — the "AI/hand-drawn outline" endpoint
 * of the morph (distinct from the built-in geometric shapes so the shape-shift is obvious).
 */
private fun buildStarLasso(): OGPolygonShape {
    val pts = (0 until 10).map { i ->
        val outer = i % 2 == 0
        val r = if (outer) 0.5f else 0.22f
        val ang = (-90.0 + i * 36.0) * PI / 180.0
        OGPoint(0.5f + r * cos(ang).toFloat(), 0.5f + r * sin(ang).toFloat())
    }
    return OGPolygonShape(pts)
}

/**
 * 🫧 **Morph the clip itself** (roadmap Bet 2). The *mask* animates circle → diamond → triangle →
 * lasso → circle while the media keeps playing underneath. One [ogMorphSequence] (built on the new
 * `OGMorphShape`) is fed to a running video's `OGPlayerConfig.clipShape` AND an animated GIF's
 * `OGImageView.clipShape` — the exact same `Shape` slot a static circle uses — so clipping *moving*
 * media to a *morphing* outline needs no new surface API. Parse-once / lerp-per-frame holds 60fps.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MorphClipScreen() {
    var auto by remember { mutableStateOf(true) }
    var manual by remember { mutableStateOf(0f) }

    val transition = rememberInfiniteTransition(label = "morphClip")
    val autoProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // One full loop = the whole circle→…→circle chain; Restart snaps 1→0 on the same shape (circle),
        // so the loop is seamless.
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "progress"
    )
    val progress = if (auto) autoProgress else manual

    // Stable identities so the resample cache hits every frame (per-frame work = just the lerp).
    val star = remember { buildStarLasso() }
    val stops = remember(star) {
        listOf(CircleShape, DiamondShape(), TriangleShape(TriangleDirection.UP), star, CircleShape)
    }
    // Re-created each frame (progress changes) — cheap: a new OGMorphShape wrapping cached samples.
    val clip = ogMorphSequence(stops, progress)

    // Which two stops we're between right now (for the live label).
    val segs = stops.size - 1
    val segIndex = (progress.coerceIn(0f, 1f) * segs).toInt().coerceIn(0, segs - 1)
    val fromName = STOP_NAMES[segIndex]
    val toName = STOP_NAMES[segIndex + 1]

    val controller = rememberOGAVPlayerController()
    val source = remember { OGVideoUrlType(MORPH_VIDEO_URL) }
    val stageBrush = Brush.linearGradient(
        listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Morph the clip itself",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "The mask animates circle → diamond → triangle → lasso while the media keeps playing. One " +
                "OGMorphShape feeds the video's clipShape and the GIF's clipShape — the same slot a " +
                "static circle uses. Clipping moving media to a morphing shape, same code on Android & iOS.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Controls ────────────────────────────────────────────────────────────────
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = auto,
                onClick = { auto = true },
                label = { Text("Auto-morph") }
            )
            FilterChip(
                selected = !auto,
                onClick = { auto = false },
                label = { Text("Manual") }
            )
        }
        if (!auto) {
            Slider(value = manual, onValueChange = { manual = it })
        }
        Text(
            "Now morphing:  $fromName → $toName",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Hero stage: a running VIDEO clipped to the morphing shape ─────────────────
        Text("Video · OGPlayerConfig.clipShape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(stageBrush),
            contentAlignment = Alignment.Center
        ) {
            OGAVPlayer(
                action = OGAVPlayerAction.PLAY,
                source = source,
                config = OGPlayerConfig(
                    showDefaultControls = false,
                    displayMovable = false,
                    backgroundColor = Color.Transparent,
                    contentScale = OGVideoScale.FILL,
                    clipShape = clip, // ← the morphing mask
                    playbackConfig = OGVideoPlaybackConfig(autoStart = true, autoRepeat = true)
                ),
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                onError = { }
            )
        }

        // ── Second stage: an animated GIF clipped to the SAME morphing shape ──────────
        Text("Animated GIF · OGImageView.clipShape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            OGImageView(
                source = OGImageUrlType(MORPH_GIF_URL),
                modifier = Modifier.size(200.dp),
                clipShape = clip, // ← same Shape, moving frames
                contentScale = ContentScale.Crop,
                onEventTriggered = { _, _ -> },
                onError = { }
            )
        }

        // ── Third stage: no media at all — the morph shape over a gradient (always renders) ──
        Text("Any Modifier.clip · a gradient tile", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(clip)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFF5DA2), Color(0xFF7B2FF7), Color(0xFF00C2A8))
                        )
                    )
            )
        }

        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                "OGMorphShape resamples each endpoint outline once (cached by shape+size) then lerps the " +
                    "point lists per frame — the same sample-once / lerp-per-frame budget as the 1.9.0 SVG " +
                    "path morph, so it holds 60fps. Endpoints can be any Shape: the built-ins, a " +
                    "RoundedCornerShape, or an OGPolygonShape lasso from a finger/AI outline.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
