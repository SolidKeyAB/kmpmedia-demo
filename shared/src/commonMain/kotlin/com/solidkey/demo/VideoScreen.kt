package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.video.loading.OGVideoUrlType
import com.solidkey.painpoints.video.playing.OGAVPlayer
import com.solidkey.painpoints.video.playing.OGAVPlayerAction
import com.solidkey.painpoints.video.playing.OGCue
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.video.playing.OGPlayerConfig
import com.solidkey.painpoints.video.playing.OGVideoPlaybackConfig
import com.solidkey.painpoints.video.playing.OGVideoScale
import com.solidkey.painpoints.video.playing.rememberOGAVPlayerController

// Big Buck Bunny, 1280x720 H.264/AAC — the picture FILLS the whole frame (no letterbox baked into
// the pixels), so contentScale=FILL can fill any shape with ZERO empty/black areas. Two hazards
// this choice deliberately avoids:
//  1. Baked-in bars: the old Sintel trailer (media.w3.org/2010/05/sintel/trailer.mp4) is 2.35:1
//     content hard-encoded inside a 16:9 frame with BLACK BARS burned into the video — no player
//     setting can strip pixels that are part of the clip, so it always showed black top/bottom.
//  2. Odd dimensions: media.w3.org/2010/05/bunny/movie.mp4 is 853x480 — the ODD width makes some
//     H.264 decoders (incl. the Android emulator's) fail MediaCodec.configure with
//     IllegalArgumentException → the clip renders solid black. 1280x720 is a multiple of 16, so it
//     decodes everywhere. (The old gtv-videos-bucket BigBuckBunny.mp4 now 403s.)
private const val SAMPLE_URL =
    "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"

/** A pickable frame shape, mapping a friendly label to the library's [OGShapeType]. */
private data class ShapeChoice(val type: OGShapeType, val label: String, val glyph: String)

// SQUARE and RECTANGLE both resolve to a rounded rect in the library, so we expose one
// "Rounded" chip and the four genuinely distinct silhouettes.
private val SHAPE_CHOICES = listOf(
    ShapeChoice(OGShapeType.RECTANGLE, "Rounded", "▭"),
    ShapeChoice(OGShapeType.CIRCLE, "Circle", "●"),
    ShapeChoice(OGShapeType.TRIANGLE_UP, "Triangle", "▲"),
    ShapeChoice(OGShapeType.TRIANGLE_DOWN, "Inverted", "▽"),
    ShapeChoice(OGShapeType.DIAMOND, "Diamond", "◆"),
)

/** A named point on the timeline — seek target (chip) + cue marker (auto-highlight when crossed). */
private data class Chapter(val atMs: Long, val label: String)

// The bundled sample is a ~10s clip, so chapters sit every 2.5s. These drive BOTH the seek chips
// (tap → controller.seekTo) AND the library cues (OGCue.at → highlight as playback crosses each).
private val CHAPTERS = listOf(
    Chapter(0L, "Intro"),
    Chapter(2_500L, "Meadow"),
    Chapter(5_000L, "Chase"),
    Chapter(7_500L, "Finale"),
)

/** Milliseconds → "m:ss" for the scrubber readout. */
private fun fmtTime(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

/**
 * The headline video showcase, now fully configurable:
 *  • **Any shape** — [OGPlayerConfig.displayShape] re-masks the running clip live (no restart).
 *  • **Fill vs fit** — [OGPlayerConfig.contentScale] crops the video to fill the shape (kills the
 *    empty letterbox areas) or letterboxes it.
 *  • **Backdrop** — [OGPlayerConfig.backgroundColor] paints the empty areas any color (e.g. match
 *    the page so a circle reads as a floating circle).
 *  • **Detached controls** — the player is chrome-less by default; a `rememberOGAVPlayerController()`
 *    drives it from buttons rendered *outside* the component (the games / custom-HUD use case). An
 *    optional on-video overlay is one toggle away.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoScreen() {
    var urlField by remember { mutableStateOf(SAMPLE_URL) }
    var loadedUrl by remember { mutableStateOf(SAMPLE_URL) }
    var repeat by remember { mutableStateOf(true) }
    var shape by remember { mutableStateOf(OGShapeType.CIRCLE) }
    // Default to FILL so the shape opens completely filled — no empty/letterbox areas at all.
    var fill by remember { mutableStateOf(true) }
    var overlayControls by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val controller = rememberOGAVPlayerController()
    val source = remember(loadedUrl) { OGVideoUrlType(loadedUrl) }

    // ── Interactive timeline (new in 1.2.0) ──────────────────────────────────────────────────────
    // Live status comes straight off the controller; scrubbing + chapter highlighting are all library.
    val status by controller.status
    var currentChapter by remember { mutableStateOf(0) }   // updated by the chapter cues below
    var highlight by remember { mutableStateOf(false) }    // toggled by a range cue [4s, 6s)
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableStateOf(0f) }
    // The cues handed to the player: a point cue per chapter (fires as playback crosses it) plus a
    // range cue that flags an "action scene" between 4s and 6s. Pure declarations — the library fires
    // them off the playback position, identically on Android and iOS.
    val cues = remember {
        CHAPTERS.mapIndexed { i, ch -> OGCue.at(ch.atMs) { currentChapter = i } } +
            OGCue.range(4_000L, 6_000L, onEnter = { highlight = true }, onExit = { highlight = false })
    }

    // Backdrop swatches. "Match page" blends the empty areas into the screen so a shaped, letterboxed
    // clip looks like it's floating. "Transparent" paints nothing at all, so the empty areas show
    // whatever sits BEHIND the player — here the gradient stage below (proving it's truly see-through,
    // not just a color that happens to match).
    val pageColor = MaterialTheme.colorScheme.background
    val bgChoices = listOf(
        "Black" to Color.Black,
        "White" to Color.White,
        "Match page" to pageColor,
        "Indigo" to Color(0xFF3F3D9E),
        "Transparent" to Color.Transparent,
    )
    var background by remember { mutableStateOf(Color.Black) }

    // A distinctive gradient painted BEHIND the shaped player. With a solid backdrop the empty areas
    // hide it; with "Transparent" they let it show through — an unmistakable see-through demo.
    val stageBrush = Brush.linearGradient(
        listOf(Color(0xFF00C2A8), Color(0xFF7B2FF7), Color(0xFFFF5DA2))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Video in any shape",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "One OGAVPlayer, fully configurable: pick a shape, fill or letterbox it, set the backdrop, " +
                "and drive it from controls that live outside the component. Same code on Android " +
                "(ExoPlayer) and iOS (AVPlayer).",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Shape picker ────────────────────────────────────────────────────────────
        Text("Frame shape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SHAPE_CHOICES.forEach { choice ->
                FilterChip(
                    selected = shape == choice.type,
                    onClick = { shape = choice.type },
                    leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                    label = { Text(choice.label) }
                )
            }
        }

        // ── Stage: chrome-less player, clipped to the chosen shape (auto-plays via action = PLAY) ─
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(stageBrush), // the gradient a "Transparent" backdrop reveals
            contentAlignment = Alignment.Center
        ) {
            OGAVPlayer(
                action = OGAVPlayerAction.PLAY,
                source = source,
                config = OGPlayerConfig(
                    showDefaultControls = false,
                    displayMovable = false,
                    displayShape = shape,
                    cornerRadius = 18.dp,
                    backgroundColor = background,
                    contentScale = if (fill) OGVideoScale.FILL else OGVideoScale.FIT,
                    controlPosition = Alignment.BottomCenter,
                    playbackConfig = OGVideoPlaybackConfig(
                        autoStart = true,
                        autoRepeat = repeat
                    )
                ),
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                // On-video overlay is opt-in; by default the component ships NO controls.
                onCustomizeControls = if (overlayControls) {
                    { _, onAction -> TransportBar { onAction(it) } }
                } else null,
                onError = { error -> errorMessage = error.message },
                cues = cues
            )

            // A cue-driven badge overlaid on the video: the library flips `highlight` on/off as
            // playback enters/leaves the 4s–6s range — no timers or position math in the app.
            if (highlight) {
                Text(
                    "★ Action scene!",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                        .background(Color(0xCCFF5DA2), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // ── Interactive timeline: live position + seek + cue markers (new in 1.2.0) ───────────────
        Text("Interactive timeline · new in 1.2.0", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Live scrubber — reads controller.status.progress and seeks on release.
                val sliderValue = if (isScrubbing) scrubFraction else status.progress
                Slider(
                    value = sliderValue.coerceIn(0f, 1f),
                    onValueChange = { isScrubbing = true; scrubFraction = it },
                    onValueChangeFinished = {
                        if (status.durationMs > 0L) {
                            controller.seekTo((scrubFraction * status.durationMs).toLong())
                        }
                        isScrubbing = false
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${fmtTime(status.positionMs)} / ${fmtTime(status.durationMs)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (status.isPlaying) "▶ playing" else "⏸ paused",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // ±10s buttons via controller.seekBy — clamped to [0, duration] by the library.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { controller.seekBy(-10_000L) }) { Text("⏪ 10s") }
                    OutlinedButton(onClick = { controller.seekBy(10_000L) }) { Text("10s ⏩") }
                }
                // Chapter chips: tap to seek; the highlighted one is chosen by the cues as they fire.
                Text("Chapters", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CHAPTERS.forEachIndexed { i, ch ->
                        FilterChip(
                            selected = currentChapter == i,
                            onClick = { controller.seekTo(ch.atMs) },
                            label = { Text("${fmtTime(ch.atMs)}  ${ch.label}") }
                        )
                    }
                }
            }
        }
        Text(
            "The scrubber reads controller.status (live position/duration) and seeks on release; the " +
                "±10s buttons call controller.seekBy. Chapter chips seek via controller.seekTo, and the " +
                "highlighted chip + \"Action scene\" badge are driven by library cues (OGCue.at / .range) " +
                "that fire as playback crosses each marker — same code on Android and iOS.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Detached controls: rendered OUTSIDE the player, driving it via the controller ─────────
        Text("Detached controls", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { controller.rewind() }) { Text("⏮") }
                OutlinedButton(onClick = { controller.play() }) { Text("▶ Play") }
                OutlinedButton(onClick = { controller.pause() }) { Text("⏸ Pause") }
                OutlinedButton(onClick = { controller.stop() }) { Text("⏹ Stop") }
            }
        }
        Text(
            "These buttons live outside the video — wire them anywhere (a game HUD, a bottom bar). " +
                "The player itself is headless by default.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Options ─────────────────────────────────────────────────────────────────
        Text("Options", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = repeat, onClick = { repeat = !repeat }, label = { Text("Loop") })
            FilterChip(
                selected = fill,
                onClick = { fill = !fill },
                label = { Text(if (fill) "Fill (crop)" else "Fit (letterbox)") }
            )
            FilterChip(
                selected = overlayControls,
                onClick = { overlayControls = !overlayControls },
                label = { Text("Controls on video") }
            )
        }

        // ── Backdrop ────────────────────────────────────────────────────────────────
        Text("Backdrop (empty areas)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bgChoices.forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { background = color }.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(color, CircleShape)
                            .border(
                                width = if (background == color) 3.dp else 1.dp,
                                color = if (background == color) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                    Text(label, fontSize = 13.sp)
                }
            }
        }
        Text(
            "Fit + \"Match page\" makes a shaped clip look like it's floating. \"Transparent\" paints " +
                "nothing — the empty areas show the gradient behind the player, so the video reads as a " +
                "see-through cut-out you can drop over any content.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Load any URL ────────────────────────────────────────────────────────────
        Text("Load a video URL", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = urlField,
            onValueChange = { urlField = it },
            label = { Text("MP4 / HLS URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                errorMessage = null
                loadedUrl = urlField.trim()
            }) { Text("Load") }
            TextButton(onClick = {
                errorMessage = null
                urlField = SAMPLE_URL
                loadedUrl = SAMPLE_URL
            }) { Text("Reset to sample") }
        }

        errorMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    "⚠️ $it",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Under the hood: OGAVPlayer is an expect/actual Composable. displayShape/backgroundColor/" +
                "contentScale flow to a TextureView on Android (in-hierarchy, so the shape clip is " +
                "honored) and an AVPlayerLayer on iOS. rememberOGAVPlayerController() hoists play/" +
                "pause/stop/rewind so any external UI can drive it — no built-in chrome required.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

/** Themed, translucent transport bar overlaid on the player (opt-in). */
@Composable
private fun TransportBar(onAction: (OGAVPlayerAction) -> Unit) {
    Row(
        modifier = Modifier
            .padding(10.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton("⏮") { onAction(OGAVPlayerAction.REWIND) }
        TransportButton("▶") { onAction(OGAVPlayerAction.PLAY) }
        TransportButton("⏸") { onAction(OGAVPlayerAction.PAUSE) }
        TransportButton("⏹") { onAction(OGAVPlayerAction.STOP) }
    }
}

@Composable
private fun TransportButton(glyph: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(glyph, color = Color.White, fontSize = 20.sp)
    }
}
