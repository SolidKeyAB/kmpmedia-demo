package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.compositor.OGComposition
import com.solidkey.painpoints.compositor.OGCompositionLayer
import com.solidkey.painpoints.compositor.OGCompositionView
import com.solidkey.painpoints.compositor.OGEasing
import com.solidkey.painpoints.compositor.OGKeyframe
import com.solidkey.painpoints.compositor.OGKeyframedFloat
import com.solidkey.painpoints.compositor.OGLayerContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import com.solidkey.painpoints.compositor.OGGifDither
import com.solidkey.painpoints.compositor.exportFrames
import com.solidkey.painpoints.compositor.exportGif
import com.solidkey.painpoints.compositor.exportMp4
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFileType
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.video.loading.OGVideoFileType
import com.solidkey.painpoints.video.playing.OGAVPlayer
import com.solidkey.painpoints.video.playing.OGAVPlayerAction
import com.solidkey.painpoints.video.playing.OGPlayerConfig
import com.solidkey.painpoints.video.playing.OGVideoPlaybackConfig
import com.solidkey.painpoints.video.playing.OGVideoScale
import com.solidkey.painpoints.shape.TriangleDirection
import com.solidkey.painpoints.shape.TriangleShape
import com.solidkey.painpoints.shape.DiamondShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

// ── The demo composition ──────────────────────────────────────────────────────────────────────
// A 288×288, 2s, 20fps scene: a full-canvas gradient backdrop (so the GIF dither toggle is visible)
// plus SOLID colour layers clipped to KMPMedia shapes, all animated on ONE timeline. Only density-independent
// clips are used (CircleShape / DiamondShape(0dp) / TriangleShape(0dp) / RoundedCornerShape(percent))
// so the on-screen preview (screen density) and the exported GIF (density 1) match exactly — a Dp-based
// corner would render rounder in the preview than in the file. All positions/sizes are raw pixels.
private const val CANVAS = 288f
private val TEAL = Color(0xFF2DD4BF)
private val AMBER = Color(0xFFF59E0B)
private val PINK = Color(0xFFEC4899)
private val PAPER = Color(0xFFF8FAFC)
private val BACKDROP = Color(0xFF0F1330)

/** A keyframe track from `(timeMs, value, easing)` triples; the ramp INTO each stop uses its easing. */
private fun track(vararg stops: Triple<Long, Float, OGEasing>): OGKeyframedFloat = OGKeyframedFloat(
    default = stops.firstOrNull()?.second ?: 0f,
    keyframes = stops.map { OGKeyframe(it.first, it.second, it.third) },
)

private fun demoComposition(gradient: ImageBitmap): OGComposition {
    val e = OGEasing.EASE_IN_OUT
    val lin = OGEasing.LINEAR

    // Full-canvas gradient backdrop. A smooth gradient is exactly where a 256-colour GIF palette BANDS —
    // so this layer makes the Floyd–Steinberg dither toggle visible: off = steppy bands, on = smooth.
    val backdrop = OGCompositionLayer(
        id = "gradient",
        content = OGLayerContent.Image(gradient),
        width = CANVAS, height = CANVAS,
        x = OGKeyframedFloat.const(0f),
        y = OGKeyframedFloat.const(0f),
        opacity = OGKeyframedFloat.const(1f),
    )

    // Hero circle: bobs vertically, pulses in scale, fades in over the first 300ms.
    val circle = OGCompositionLayer(
        id = "circle",
        content = OGLayerContent.Solid(TEAL),
        width = 150f, height = 150f,
        x = OGKeyframedFloat.const((CANVAS - 150f) / 2f),
        y = track(Triple(0L, 84f, e), Triple(1000L, 40f, e), Triple(2000L, 84f, e)),
        scale = track(Triple(0L, 0.6f, e), Triple(1000L, 1.12f, e), Triple(2000L, 0.6f, e)),
        opacity = OGKeyframedFloat.of(0L to 0f, 300L to 1f),
        clip = CircleShape,
    )

    // Amber diamond: slides all the way across (enters from the left edge, exits the right) while rotating.
    val diamond = OGCompositionLayer(
        id = "diamond",
        content = OGLayerContent.Solid(AMBER),
        width = 100f, height = 100f,
        x = track(Triple(0L, -110f, lin), Triple(2000L, 300f, lin)),
        y = OGKeyframedFloat.const((CANVAS - 100f) / 2f),
        rotationDeg = track(Triple(0L, 0f, lin), Triple(2000L, 180f, lin)),
        opacity = OGKeyframedFloat.const(0.95f),
        clip = DiamondShape(),
    )

    // Pink triangle: enters at 500ms (a real [startMs] window), then spins in place.
    val triangle = OGCompositionLayer(
        id = "triangle",
        content = OGLayerContent.Solid(PINK),
        width = 88f, height = 88f,
        x = OGKeyframedFloat.const(28f),
        y = OGKeyframedFloat.const(28f),
        rotationDeg = track(Triple(500L, 0f, lin), Triple(2000L, 300f, lin)),
        opacity = OGKeyframedFloat.of(500L to 0f, 900L to 0.95f),
        clip = TriangleShape(TriangleDirection.UP),
        startMs = 500L,
    )

    // Paper badge: enters even later (1000ms), pulses — makes the enter/leave timeline obvious.
    val badge = OGCompositionLayer(
        id = "badge",
        content = OGLayerContent.Solid(PAPER),
        width = 66f, height = 66f,
        x = OGKeyframedFloat.const(190f),
        y = OGKeyframedFloat.const(190f),
        scale = track(Triple(1000L, 0.4f, e), Triple(1500L, 1.1f, e), Triple(2000L, 0.7f, e)),
        opacity = OGKeyframedFloat.of(1000L to 0f, 1200L to 0.92f),
        clip = RoundedCornerShape(percent = 28),
        startMs = 1000L,
    )

    return OGComposition(
        width = CANVAS.toInt(),
        height = CANVAS.toInt(),
        durationMs = 2000L,
        fps = 20,
        background = BACKDROP,
        layers = listOf(backdrop, circle, diamond, triangle, badge),
    )
}

/** A full-canvas diagonal gradient `ImageBitmap` — the content a GIF palette bands without dithering. */
private fun gradientBitmap(density: androidx.compose.ui.unit.Density): ImageBitmap {
    val w = CANVAS.toInt(); val h = CANVAS.toInt()
    val bmp = ImageBitmap(w, h)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bmp), Size(w.toFloat(), h.toFloat())) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF1E3A8A), TEAL, AMBER, PINK),
            )
        )
    }
    return bmp
}

/**
 * **On-device compositor + export** (lib 1.13.0 — roadmap Bet 3). Several solid-colour layers, each
 * clipped to a KMPMedia shape and animated with keyframed x / y / scale / rotation / opacity on ONE
 * `OGComposition` timeline:
 *
 *  • **Preview** — `OGCompositionView` plays the timeline live on the Compose frame clock (60fps), or,
 *    in Scrub mode, shows exactly the instant a slider points at (`positionMs`). No intermediate bitmap.
 *  • **Export** — `OGComposition.exportGif()` renders every frame offscreen and encodes ONE looping
 *    animated GIF with the pure-Kotlin `OGGifEncoder` (median-cut palette + LZW, zero deps, identical
 *    bytes on both platforms). The bytes are saved via the demo's [rememberGifSaver] and then played
 *    back through the platform's own GIF decoder in an [OGImageView] below — proving the export is a
 *    real, decodable file. See docs/COMPOSITOR.md.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompositorScreen() {
    val density = LocalDensity.current
    val gradient = remember(density) { gradientBitmap(density) }
    val composition = remember(gradient) { demoComposition(gradient) }
    val scope = rememberCoroutineScope()
    val gifSaver = rememberGifSaver()
    val mp4Saver = rememberMp4Saver()

    var playing by remember { mutableStateOf(true) }
    var scrub by remember { mutableStateOf(false) }
    var scrubMs by remember { mutableStateOf(0f) }

    var dither by remember { mutableStateOf(true) }

    var exporting by remember { mutableStateOf(false) }
    var exportInfo by remember { mutableStateOf<String?>(null) }
    var exportFile by remember { mutableStateOf<String?>(null) }
    var exportNonce by remember { mutableStateOf(0) }

    var mp4Info by remember { mutableStateOf<String?>(null) }
    var mp4File by remember { mutableStateOf<String?>(null) }
    var mp4Nonce by remember { mutableStateOf(0) }

    var frames by remember { mutableStateOf<List<ImageBitmap>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Compositor + export",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Compose several layers on one timeline, animate them with keyframes, preview live at " +
                "60fps — then export the whole thing to a shareable animated GIF or an H.264 MP4, all on " +
                "device. No other KMP library does compose-and-export.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Live preview / scrubber ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFF05060F)),
            contentAlignment = Alignment.Center
        ) {
            OGCompositionView(
                composition = composition,
                modifier = Modifier.fillMaxSize().padding(8.dp),
                isPlaying = playing,
                loop = true,
                positionMs = if (scrub) scrubMs.toLong() else null,
                contentScale = ContentScale.Fit,
            )
        }

        // ── Transport ───────────────────────────────────────────────────────────────────
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = playing && !scrub,
                onClick = { scrub = false; playing = true },
                leadingIcon = { Text("▶", fontSize = 14.sp) },
                label = { Text("Play") },
            )
            FilterChip(
                selected = !playing && !scrub,
                onClick = { scrub = false; playing = false },
                leadingIcon = { Text("⏸", fontSize = 14.sp) },
                label = { Text("Pause") },
            )
            FilterChip(
                selected = scrub,
                onClick = { scrub = true; playing = false },
                leadingIcon = { Text("🎚️", fontSize = 14.sp) },
                label = { Text("Scrub") },
            )
        }
        if (scrub) {
            Text(
                "Time: ${scrubMs.toLong()} / ${composition.durationMs} ms",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Slider(
                value = scrubMs,
                onValueChange = { scrubMs = it },
                valueRange = 0f..composition.durationMs.toFloat(),
            )
            Text(
                "Scrub feeds positionMs straight into OGCompositionView — the same composable is a " +
                    "player and a scrubber. Every layer's tracks are evaluated at exactly this instant.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // ── Export ────────────────────────────────────────────────────────────────────
        // Dither toggle: Floyd–Steinberg (1.26.0) vs plain nearest-colour. Re-export to compare the
        // gradient backdrop — off bands into steps, on stays smooth at the same 256-colour palette.
        FilterChip(
            selected = dither,
            onClick = { dither = !dither },
            leadingIcon = { Text(if (dither) "✨" else "▦", fontSize = 14.sp) },
            label = { Text(if (dither) "Dither: on (smooth gradients)" else "Dither: off (bands)") },
        )
        Button(
            onClick = {
                if (exporting) return@Button
                scope.launch {
                    exporting = true
                    exportInfo = "Rendering ${composition.frameCount} frames…"
                    yield() // let the "Rendering…" label paint before the one-shot encode
                    val mode = if (dither) OGGifDither.FLOYD_STEINBERG else OGGifDither.NONE
                    val bytes = composition.exportGif(dither = mode)
                    val name = "compositor_export_$exportNonce"
                    val ok = gifSaver.save(name, bytes)
                    if (ok) {
                        exportFile = name
                        exportNonce++
                        exportInfo = "Exported ${composition.frameCount} frames · " +
                            "${bytes.size / 1024} KB GIF · looping · dither ${if (dither) "on" else "off"}"
                    } else {
                        exportInfo = "Save failed"
                    }
                    exporting = false
                }
            },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Exporting…" else "Export to animated GIF")
        }
        exportInfo?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Play the exported GIF back through the platform's OWN decoder — proof it's a valid file.
        exportFile?.let { name ->
            Text(
                "Played back through the platform GIF decoder:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF05060F))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OGImageView(
                    source = OGImageFileType(name, OGImageFormat.GIF),
                    // SQUARE is a rectangle clip (visually a no-op for a square GIF), but it flips
                    // OGImageView onto its shaped path so the Image fills + centers the box with Fit
                    // instead of sitting at its intrinsic pixel size in the top-left corner.
                    displayShape = OGShapeType.SQUARE,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(280.dp),
                    onError = { exportInfo = "Playback error: $it" },
                    onEventTriggered = { _, _ -> },
                )
            }
        }

        // ── Export to MP4 (H.264 via the OS encoder) ────────────────────────────────────
        Button(
            onClick = {
                if (exporting) return@Button
                scope.launch {
                    exporting = true
                    mp4Info = "Rendering ${composition.frameCount} frames → H.264…"
                    yield() // let the label paint before the blocking encode
                    // exportMp4 drives MediaCodec / AVAssetWriter — run it off the main thread.
                    val bytes = withContext(Dispatchers.Default) { composition.exportMp4() }
                    val name = "compositor_export_mp4_$mp4Nonce"
                    val ok = bytes.isNotEmpty() && mp4Saver.save(name, bytes)
                    if (ok) {
                        mp4File = "$name.mp4"
                        mp4Nonce++
                        mp4Info = "Exported ${composition.frameCount} frames · " +
                            "${bytes.size / 1024} KB MP4 (H.264)"
                    } else {
                        mp4Info = if (bytes.isEmpty()) "Encode produced no data" else "Save failed"
                    }
                    exporting = false
                }
            },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Exporting…" else "Export to MP4 (H.264)")
        }
        mp4Info?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // Play the exported MP4 back through the platform's OWN video player — proof it's a valid file.
        mp4File?.let { fileName ->
            Text(
                "Played back through the platform video player (OGAVPlayer):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF05060F)),
                contentAlignment = Alignment.Center
            ) {
                OGAVPlayer(
                    action = OGAVPlayerAction.PLAY,
                    source = OGVideoFileType(fileName),
                    config = OGPlayerConfig(
                        showDefaultControls = false,
                        displayMovable = false,
                        backgroundColor = Color.Transparent,
                        contentScale = OGVideoScale.FIT,
                        playbackConfig = OGVideoPlaybackConfig(autoStart = true, autoRepeat = true),
                    ),
                    modifier = Modifier.fillMaxSize(),
                    onError = { },
                )
            }
        }

        // ── Export frame sequence (the individual frames) ───────────────────────────────
        Button(
            onClick = {
                if (exporting) return@Button
                scope.launch {
                    exporting = true
                    // exportFrames() renders one bitmap per frame — run it off the main thread.
                    frames = withContext(Dispatchers.Default) { composition.exportFrames() }
                    exporting = false
                }
            },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Exporting…" else "Export frame sequence (${composition.frameCount} frames)")
        }
        if (frames.isNotEmpty()) {
            Text(
                "${frames.size} rendered frames — the filmstrip (scroll →):",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                frames.forEachIndexed { i, bmp ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "frame $i",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(64.dp).background(Color(0xFF05060F)),
                        )
                        Text("$i", fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Under the hood: com.solidkey.painpoints.compositor — OGComposition (canvas + durationMs/fps " +
                "timeline + back-to-front layers), OGCompositionLayer (each track an OGKeyframedFloat, any " +
                "Shape clip, a [startMs,endMs] window), OGCompositionView (frame-clock preview / scrubber), " +
                "exportGif() → the pure-Kotlin OGGifEncoder (Floyd–Steinberg dithered by default, 1.26.0), " +
                "exportMp4() → the OS H.264 encoder " +
                "(MediaCodec / AVAssetWriter), and exportFrames() → the raw frame sequence. Zero new " +
                "dependencies, identical on Android & iOS. " +
                "See docs/COMPOSITOR.md.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
