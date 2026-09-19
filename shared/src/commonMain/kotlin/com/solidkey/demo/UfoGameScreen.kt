package com.solidkey.demo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.util.lerp
import com.solidkey.painpoints.audio.playing.OGAudioClip
import com.solidkey.painpoints.audio.playing.OGAudioSprite
import com.solidkey.painpoints.depth.OGDepthConfig
import com.solidkey.painpoints.depth.ogDepth
import com.solidkey.painpoints.source.OGSource
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.animating.OGAnimatedContainer
import com.solidkey.painpoints.image.animating.OGAnimatedImage
import com.solidkey.painpoints.image.animating.OGAnimationType
import com.solidkey.painpoints.image.loading.OGSvgResourceFileType
import com.solidkey.painpoints.video.loading.OGVideoUrlType
import com.solidkey.painpoints.video.playing.OGAVPlayer
import com.solidkey.painpoints.video.playing.OGAVPlayerAction
import com.solidkey.painpoints.video.playing.OGAVPlayerController
import com.solidkey.painpoints.video.playing.OGCue
import com.solidkey.painpoints.video.playing.rememberOGAVPlayerController
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.video.playing.OGPlayerConfig
import com.solidkey.painpoints.video.playing.OGVideoPlaybackConfig
import com.solidkey.painpoints.video.playing.OGVideoScale
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * "UFO Dodge" — a vertical flyer where **every sprite is a plain static SVG** and **all of
 * the motion comes from KMPMedia** wrapping those stills via [OGAnimatedImage]:
 *
 *  - the player's **UFO** (`ufo.svg`) shakes/flickers harder the more damage it has taken —
 *    the animation *set* and *intensity* are a pure function of the current damage level,
 *  - **meteorites** (`meteorite.svg`) tumble (ROTATE),
 *  - **hostile spacecraft** (`hostile.svg`) throb menacingly (SCALE),
 *  - **stars** (`star.svg`) twinkle (SCALE+ROTATE) and repair 1 point of damage when caught.
 *  - **cosmic anomalies** are the exception — **real streaming video** (space clips) clipped to a
 *    shape via [OGAVPlayer] (`contentScale = FILL`) and wrapped in [OGAnimatedContainer] so they
 *    spin/pulse while they drift. Each is its own decoder, so they're capped at [COSMIC_CAP].
 *
 * On top of the sprites, the game is a live demo of **layer management / depth**. Every object and
 * the UFO carry a `depth` in 0f..1f (0 = far/behind → "under", 1 = near/in-front → "over"). A
 * two-finger **pinch** grows/shrinks the UFO along that axis, and that single value drives three
 * things at once: (1) the UFO's `Modifier.zIndex`, so it literally flies OVER or UNDER each object;
 * (2) a depth-of-field **blur + dim** on everything off the UFO's focal plane; (3) a depth-gated
 * hitbox — only objects on ~the UFO's layer are solid, so pinching to a different layer is a real
 * dodge. The blur/dim/draw-order come from the library's `Modifier.ogDepth(depth, focalDepth)`
 * (KMPMedia 1.1.0 `depth` package); the depth-gated collision + the UFO's pinch-scale stay in the
 * game.
 *
 * The falling/steering/collision is a Compose frame loop; positions are applied with
 * `Modifier.offset{}` (layout phase) while KMPMedia's `graphicsLayer` animation rides on
 * top (draw phase) — so a sprite can be steered AND animated without being re-parsed.
 *
 * This is the same "hand it a static image, get an animated one back" library primitive the
 * "Animate a Static Image" screen shows — here it's wired to *game state* to demonstrate
 * state-driven animation (damage → violence, level-up → celebratory pop).
 *
 * Sprites load from bundled assets (Android `assets/`, iOS app bundle).
 */
private enum class Kind { METEORITE, HOSTILE, STAR, COSMIC, CUSTOM, WORMHOLE }
private enum class Phase { READY, PLAYING, GAME_OVER }

/** How the player changes the UFO's DEPTH: two-finger [PINCH] (default) or phone [TILT] (gyro). */
private enum class ControlMode { PINCH, TILT }

/**
 * The **black-hole warp** between sectors — the "dive into a wormhole, come out somewhere else in the
 * universe, keep flying" sequence. A little state machine:
 *  - [NONE]   — normal play.
 *  - [DIVE]   — the UFO spirals + shrinks into the wormhole it flew into (the field is cleared).
 *  - [TUNNEL] — a full-screen Star-Trek warp-streak tunnel plays while the **backdrop video swaps** to
 *               the next sector (a different point of the universe) — reloaded on the SAME decoder.
 *  - [EMERGE] — the UFO spins back out of a point in the new sector, grows to size, and play resumes.
 */
private enum class Warp { NONE, DIVE, TUNNEL, EMERGE }

/**
 * A **real-video** game object: a streaming space clip clipped to [shape] and wrapped in
 * [OGAnimatedContainer] so it also moves ([anims]). This is the headline "video in any shape,
 * animated" library combo, dropped straight into gameplay.
 */
private data class VideoSpec(
    val url: String,
    val shape: OGShapeType,
    val anims: Set<OGAnimationType>,
    val durationMs: Int,
)

/**
 * Each on-screen video object is its own [OGAVPlayer] → its own ExoPlayer/decoder, and decoders
 * are a scarce, device-limited resource. Cap how many run at once so we never exhaust them
 * (spawns fall back to the cheap static-SVG hazards while at the cap).
 *
 * The full-screen [SPACE_BACKDROP] now holds one permanent decoder for the whole game, so this
 * cap is 1 (down from 2) to keep the *concurrent* decoder count at the tested-safe ceiling of 2
 * (backdrop + one cosmic object).
 */
private const val COSMIC_CAP = 1

/**
 * Full-screen "space travel" backdrops — one per **sector** of the universe. The game starts in
 * sector 1 and every black-hole [Warp] swaps the single backdrop [OGAVPlayer]'s `source` to the next
 * entry (reloaded on the SAME decoder — see [OGAVPlayer]'s `LaunchedEffect(source)`), so warping
 * genuinely drops you into a different point of the universe with a different sky. Each is a **1280×720**
 * H.264 clip (even dims so MediaCodec accepts it), rendered `contentScale = FILL` + `autoRepeat`.
 * The first (32910 "Exploring the cosmos") is the smoothest looper (first↔last frame PSNR ≈ 35 dB) and
 * starts buffering the moment the screen opens — behind the READY overlay — so it's already playing by
 * the time the player taps Launch.
 */
private val SPACE_BACKDROPS = listOf(
    "https://assets.mixkit.co/videos/32910/32910-720.mp4",   // Sector 1 — deep-field flythrough
    "https://assets.mixkit.co/videos/19636/19636-720.mp4",   // Sector 2 — across the Milky Way
    "https://assets.mixkit.co/videos/14185/14185-720.mp4",   // Sector 3 — a glowing nebula
)

/** Human names for each sector, shown on the "new universe" banner after a warp. */
private val SECTOR_NAMES = listOf("Deep Field", "Milky Way", "Nebula Veil")

/**
 * The **wormhole portal** the UFO dives into to warp between sectors: a real spinning space clip
 * (Mixkit wormhole, 360p — small object so the light rendition buffers fast). Flying the UFO into its
 * centre triggers the [Warp] sequence. Reuses the same "video in any shape" primitive as the cosmic
 * objects — here a spinning CIRCLE with a glowing ring.
 */
private const val WORMHOLE_CLIP = "https://assets.mixkit.co/videos/18791/18791-360.mp4"

/** First wormhole appears after this many survived seconds; then one roughly every [PORTAL_EVERY_S]. */
private const val PORTAL_FIRST_S = 14f
private const val PORTAL_EVERY_S = 24f

/**
 * Curated space clips (Mixkit, H.264 **640×360** — even/​mult-of-16 dims so every decoder accepts
 * them; an odd width makes MediaCodec fail → black. The light 360p rendition buffers fast enough
 * to appear mid-game). Each pairs a clip with a shape + animation so the objects arrive in varied
 * shapes, sizes and motions — real video, not sprites.
 */
private val COSMIC_CLIPS = listOf(
    VideoSpec("https://assets.mixkit.co/videos/45033/45033-360.mp4", OGShapeType.CIRCLE, setOf(OGAnimationType.SCALE), 3000),                         // planet Earth — breathe
    VideoSpec("https://assets.mixkit.co/videos/4081/4081-360.mp4",   OGShapeType.CIRCLE, setOf(OGAnimationType.SCALE), 3400),                         // moon — breathe
    VideoSpec("https://assets.mixkit.co/videos/45020/45020-360.mp4", OGShapeType.DIAMOND, setOf(OGAnimationType.ROTATE), 5200),                       // black hole — spin
    VideoSpec("https://assets.mixkit.co/videos/18791/18791-360.mp4", OGShapeType.CIRCLE, setOf(OGAnimationType.ROTATE), 6000),                        // wormhole — portal spin
    VideoSpec("https://assets.mixkit.co/videos/14185/14185-360.mp4", OGShapeType.TRIANGLE_UP, setOf(OGAnimationType.SCALE), 1600),                    // nebula — pulse
    VideoSpec("https://assets.mixkit.co/videos/19636/19636-360.mp4", OGShapeType.DIAMOND, setOf(OGAnimationType.SCALE, OGAnimationType.ROTATE), 4200), // milky way — pulse + spin
)

/**
 * The intrinsic `width`/`height` to hand [OGAnimatedImage] so a sprite renders at exactly
 * [boxPx] on-screen pixels — i.e. it fills its layout box with no overflow or clipping.
 *
 * Our sprite SVGs all use `viewBox="0 0 100 100"`. [OGSVGView]'s FIT path multiplies the
 * viewBox→box scale in TWICE (once baked into the shapes, once in the canvas transform), so
 * the on-screen size is `width² / viewBoxWidth = width² / 100`. Inverting that: to land on
 * `boxPx`, pass `width = sqrt(100 · boxPx)`.
 */
private fun svgSideFor(boxPx: Float): Float = sqrt(100f * boxPx)

/** Hits the UFO can absorb before it's destroyed. Damage level = [MAX_HP] − hp (0..MAX_HP). */
private const val MAX_HP = 5

// ---- Depth / "layer management" tuning (the pinch-to-3D showcase) ----
// Everything on screen carries a `depth` in 0f..1f: 0 = deepest (far/behind, "under"),
// 1 = nearest (close/in-front, "over"). The UFO's own depth is pinch-controlled; an object's
// depth decides (a) its draw order relative to the UFO via Modifier.zIndex, (b) how blurred +
// dimmed it looks (distance from the UFO's focal plane = depth-of-field), and (c) whether it can
// collide at all — only objects on ~the UFO's layer are solid; the rest you fly over or under.
//
/** How far (in depth units) an object may be from the UFO and still collide. Outside this band the
 *  object is on "another layer" — the UFO passes over/under it (no hit). */
private const val DEPTH_HIT_BAND = 0.2f
/** Max blur (dp) applied to an object one full depth-unit away from the UFO's focal plane. */
private const val MAX_BLUR_DP = 16f
/** UFO on-screen scale at the deepest (0f) vs nearest (1f) depth — grow ≈ come closer, shrink ≈ recede. */
private const val SHIP_SCALE_MIN = 0.6f
private const val SHIP_SCALE_MAX = 1.5f

// Depth-of-field presets fed to the library's Modifier.ogDepth (KMPMedia 1.1.0 `depth` API), which
// applies zIndex + blur + dim from an object's depth vs the UFO's focal depth. Blurrable sprites
// (SVG / cropped photos) use the full effect; video objects use the Video preset (dim + z-order,
// no blur — a RenderEffect over a video surface is unreliable).
private val SPRITE_DEPTH = OGDepthConfig(maxBlur = MAX_BLUR_DP.dp, minAlpha = 0.35f, dimFalloff = 0.5f)
private val VIDEO_DEPTH = OGDepthConfig(blurContent = false, minAlpha = 0.35f, dimFalloff = 0.5f)

private class Sprite(
    val id: Long,
    val kind: Kind,
    val sizePx: Float,
    val xPx: Float,
    val speedPx: Float,
    val depth: Float = 0.5f,               // 0 = far/behind ("under") … 1 = near/in-front ("over")
    val video: VideoSpec? = null,          // non-null only for Kind.COSMIC (a real-video object)
    val custom: CustomObjectSpec? = null,  // non-null only for Kind.CUSTOM (a user-cropped photo)
) {
    var yPx by mutableStateOf(-sizePx)
}

private val Kind.asset: String
    get() = when (this) {
        Kind.METEORITE -> "meteorite"
        Kind.HOSTILE -> "hostile"
        Kind.STAR -> "star"
        Kind.COSMIC -> ""   // video object — no SVG asset (rendered via OGAVPlayer)
        Kind.CUSTOM -> ""   // user photo — no SVG asset (rendered via OGImageView)
        Kind.WORMHOLE -> "" // portal video — no SVG asset (rendered via OGAVPlayer)
    }

/** Per-hazard "personality": each SVG kind is a static image that KMPMedia animates differently. */
private fun Kind.animations(): Set<OGAnimationType> = when (this) {
    Kind.METEORITE -> setOf(OGAnimationType.ROTATE)                    // slow tumble
    Kind.HOSTILE -> setOf(OGAnimationType.SCALE)                       // menacing throb
    Kind.STAR -> setOf(OGAnimationType.SCALE, OGAnimationType.ROTATE)  // twinkle
    Kind.COSMIC -> emptySet()                                          // motion comes from VideoSpec
    Kind.CUSTOM -> emptySet()                                          // wrapped in its own container
    Kind.WORMHOLE -> emptySet()                                        // wrapped in its own container
}

private fun Kind.durationMillis(): Int = when (this) {
    Kind.METEORITE -> 2600
    Kind.HOSTILE -> 700
    Kind.STAR -> 1500
    Kind.COSMIC -> 1200
    Kind.CUSTOM -> 2400
    Kind.WORMHOLE -> 4200
}

/**
 * Animations for a user-cropped object, keyed off its role so a bonus reads differently from a
 * hazard: a **collect** photo gently twinkles (scale+rotate) to say "grab me", a **dodge** photo
 * tumbles like a meteorite. Reuses the same [OGAnimatedContainer] primitive the cosmic objects use.
 */
private fun CustomObjectSpec.animations(): Set<OGAnimationType> = when (role) {
    ObjectRole.COLLECT -> setOf(OGAnimationType.SCALE, OGAnimationType.ROTATE)
    ObjectRole.DODGE -> setOf(OGAnimationType.ROTATE)
}

@Composable
fun UfoGameScreen() {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0F2B), Color(0xFF1B2450))))
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // The soft control-pad lives OUTSIDE the play field — reserve a strip at the bottom of
        // the screen for it, so the game (backdrop, sprites, UFO) only uses the field above.
        val padHeightDp = 190.dp
        val padHeightPx = with(density) { padHeightDp.toPx() }
        val fieldHeightPx = (heightPx - padHeightPx).coerceAtLeast(1f)

        val shipSize = remember(widthPx) { (widthPx * 0.12f).coerceIn(76f, 128f) }
        // The UFO now flies in 2 axes (the pad drives both). It may roam from just below the HUD
        // down to the bottom of the field.
        val minShipY = with(density) { 72.dp.toPx() }
        val maxShipY = (fieldHeightPx - shipSize - with(density) { 8.dp.toPx() }).coerceAtLeast(minShipY)
        // Full-tilt travel speed of the analog stick, device-independent (dp/s).
        val padSpeedPxPerSec = with(density) { 620.dp.toPx() }

        var phase by remember { mutableStateOf(Phase.READY) }
        var shipX by remember(widthPx) { mutableStateOf((widthPx - shipSize) / 2f) }
        var shipY by remember(fieldHeightPx, shipSize) { mutableStateOf(maxShipY) }
        // Analog-stick vector from the pad, normalised to -1..1 per axis (y+ = "backward",
        // toward the player). The game loop reads it each frame to move the UFO.
        var padVec by remember { mutableStateOf(Offset.Zero) }
        // The UFO's DEPTH (0 = deepest/behind, 1 = nearest/in-front). Pinch OUT to grow it toward the
        // camera (rise ABOVE objects), pinch IN to shrink it away (drop BELOW them). This is the whole
        // "layer management" showcase: it drives the UFO's on-screen scale, its z-order vs every object,
        // and which objects are close enough (in depth) to actually hit it.
        var shipDepth by remember { mutableStateOf(0.5f) }
        // ── Black-hole warp state (the "dive into a wormhole → new universe → keep flying" feature) ──
        // Which sector's backdrop is showing, a rising sector counter for the banner, and the warp
        // phase. warpScale/warpSpin override the UFO's transform during DIVE/EMERGE (spiral into / out
        // of the hole); holeCenter is where on-screen the UFO gets sucked in.
        var backdropIndex by remember { mutableStateOf(0) }
        var sector by remember { mutableStateOf(1) }
        var warp by remember { mutableStateOf(Warp.NONE) }
        var warpScale by remember { mutableStateOf(1f) }
        var warpSpin by remember { mutableStateOf(0f) }
        var holeCenter by remember { mutableStateOf(Offset.Zero) }
        // Control scheme for DEPTH: two-finger pinch (default) or phone tilt (gyro). The Tilt option
        // only appears on devices with a motion sensor; otherwise it stays pinch-only.
        val tiltSupported = deviceTiltSupported()
        var controlMode by remember { mutableStateOf(ControlMode.PINCH) }
        val tiltActive = controlMode == ControlMode.TILT && phase == Phase.PLAYING && tiltSupported
        val tiltPitch by rememberDevicePitch(enabled = tiltActive)   // -1..1, 0 when disabled
        var score by remember { mutableStateOf(0f) }
        var best by remember { mutableStateOf(0f) }
        var hp by remember { mutableStateOf(MAX_HP) }
        var hitTick by remember { mutableStateOf(0) }   // bumps on every hazard hit → flash
        val sprites = remember { mutableStateListOf<Sprite>() }

        // ── Sound effects (dogfoods KMPMedia 1.5.0: audio sprites) ──────────────────────────────
        // ONE tiny sfx.mp3 packs three sounds back to back; OGAudioSprite fires the right SLICE per
        // game event — a bright chime on a catch (0–280ms), a low thud on a hit (400–750ms), a sweep
        // on level-up (900ms→end) — overlapping via its voice pool. No per-sound files or loading.
        val sfx = OGAudioSprite.create()
        DisposableEffect(Unit) {
            sfx.load(
                source = OGSource.Resource("sfx"),
                clips = listOf(
                    OGAudioClip("collect", startMs = 0, endMs = 280),
                    OGAudioClip("hit", startMs = 400, endMs = 750),
                    OGAudioClip("levelup", startMs = 900),   // no end → plays to the file end
                ),
                onError = { },
            )
            onDispose { sfx.release() }
        }

        // ---- Player-authored objects (the "settings of the game") ----
        // Cropped photos the player added via the ⚙️ Customize screen. They spawn among the
        // hazards during play; kept across restarts of a session so tuning them is quick.
        val customObjects = remember { mutableStateListOf<CustomObjectSpec>() }
        var showSettings by remember { mutableStateOf(false) }
        var nextCustomId by remember { mutableStateOf(0L) }

        // ── Cosmic event system (dogfoods KMPMedia 1.2.0: cues + live position) ───────────────────
        // The looping space-backdrop video CONDUCTS the game. A controller exposes its live position,
        // and OGCue markers on its timeline fire recurring, authored "cosmic events" — synced to the
        // visuals and re-firing every loop. Pure library features, zero engine hacks.
        val backdropController = rememberOGAVPlayerController()
        var stormActive by remember { mutableStateOf(false) }     // range cue [30%,62%) of the clip
        var starfallPending by remember { mutableStateOf(0) }     // point cue → drop N bonus stars
        var eventBanner by remember { mutableStateOf<String?>(null) }
        // Duration is 0 until the clip reports it; derive it so cues are (re)built exactly once when known.
        val loopMs by remember { derivedStateOf { backdropController.status.value.durationMs } }
        val cues = remember(loopMs) {
            if (loopMs <= 0L) emptyList()
            else listOf(
                // A stretch of the flythrough = a meteor storm: hazards spawn faster (see loop below).
                OGCue.range(
                    fromMs = loopMs * 30 / 100,
                    toMs = loopMs * 62 / 100,
                    onEnter = { stormActive = true; eventBanner = "☄️ METEOR STORM" },
                    onExit = { stormActive = false; if (eventBanner?.startsWith("☄") == true) eventBanner = null },
                ),
                // A beat near the end = a starfall: a burst of catchable bonus stars rains down.
                OGCue.at(atMs = loopMs * 82 / 100) {
                    starfallPending = STARFALL_COUNT
                    eventBanner = "🌟 STARFALL — grab the stars!"
                },
            )
        }
        // Auto-dismiss transient banners (STARFALL 🌟, wormhole hint 🌀, new-sector 🌌). The storm
        // banner (☄) is the exception — it stays up for the whole storm and clears on the cue's onExit.
        LaunchedEffect(eventBanner) {
            val b = eventBanner
            if (b != null && !b.startsWith("☄")) {
                delay(2200)
                if (eventBanner == b) eventBanner = null
            }
        }

        // Level rises every 10 survived seconds; used for difficulty + the level-up celebration.
        val level = 1 + (score / 10f).toInt()
        val damage = MAX_HP - hp   // 0 = pristine … MAX_HP = destroyed

        // ---- Game loop ----
        LaunchedEffect(phase) {
            if (phase != Phase.PLAYING) return@LaunchedEffect
            sprites.clear()
            score = 0f
            hp = MAX_HP
            hitTick = 0
            shipX = (widthPx - shipSize) / 2f
            shipY = maxShipY
            padVec = Offset.Zero
            shipDepth = 0.5f
            starfallPending = 0
            eventBanner = null
            // fresh run → sector 1, no warp in flight
            backdropIndex = 0
            sector = 1
            warp = Warp.NONE
            warpScale = 1f
            warpSpin = 0f
            var last = 0L
            var spawnAcc = 0f
            var nextId = 0L
            var nextPortalAt = PORTAL_FIRST_S
            while (true) {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    last = now
                    score += dt
                    val ramp = 1f + score / 18f

                    // While a warp is in flight the UFO is on rails (DIVE/EMERGE choreography) and the
                    // field is empty — freeze steering, spawning and collisions until it resolves.
                    if (warp != Warp.NONE) return@withFrameNanos

                    // steer the UFO from the pad's analog stick (velocity: hold to keep moving;
                    // release → knob springs to centre → vector zero → the UFO stops where it is).
                    val v = padVec
                    if (v.x != 0f || v.y != 0f) {
                        val step = padSpeedPxPerSec * dt
                        shipX = (shipX + v.x * step).coerceIn(0f, (widthPx - shipSize).coerceAtLeast(0f))
                        shipY = (shipY + v.y * step).coerceIn(minShipY, maxShipY)
                    }

                    // TILT control: the phone's pitch drives the UFO's DEPTH directly (top tipped
                    // back = rise/over, forward = dive/under). In PINCH mode the overlay gesture owns
                    // shipDepth instead, so this is skipped.
                    if (controlMode == ControlMode.TILT) {
                        shipDepth = (0.5f + tiltPitch * 0.5f).coerceIn(0f, 1f)
                    }

                    // move + cull off-screen (the FIELD, not the whole screen — the pad is below).
                    // The wormhole portal drifts at a steady slow pace (exempt from the difficulty
                    // ramp) so it's always reachable no matter how high the level has climbed.
                    val gone = ArrayList<Sprite>()
                    for (s in sprites) {
                        s.yPx += s.speedPx * (if (s.kind == Kind.WORMHOLE) 1f else ramp) * dt
                        if (s.yPx > fieldHeightPx + s.sizePx) gone.add(s)
                    }
                    sprites.removeAll(gone)

                    // spawn (faster over time) — now MODULATED by the backdrop video's cue events:
                    //  • STARFALL (point cue) → a quick burst of guaranteed bonus stars.
                    //  • METEOR STORM (range cue) → hazards spawn roughly twice as fast.
                    spawnAcc += dt
                    val interval = when {
                        starfallPending > 0 -> 0.22f                                  // rapid star burst
                        stormActive -> max(0.32f, (1.15f - score / 45f) * 0.55f)      // storm = faster
                        else -> max(0.5f, 1.15f - score / 45f)                        // normal ramp
                    }
                    if (spawnAcc >= interval) {
                        spawnAcc = 0f
                        if (starfallPending > 0) {
                            sprites.add(spawnStar(nextId++, widthPx, density.density))
                            starfallPending -= 1
                        } else {
                            val videoCount = sprites.count { it.kind == Kind.COSMIC }
                            sprites.add(spawn(nextId++, widthPx, density.density, videoCount, customObjects))
                        }
                    }

                    // Wormhole portal scheduler: drop one every so often (only if none is on-screen),
                    // announce it, and let the player choose to dive in for a sector warp.
                    if (score >= nextPortalAt && sprites.none { it.kind == Kind.WORMHOLE }) {
                        sprites.add(spawnWormhole(nextId++, widthPx, density.density))
                        nextPortalAt = score + PORTAL_EVERY_S
                        eventBanner = "🌀 Wormhole — fly IN to warp!"
                    }

                    // collision — forgiving hitboxes, and ONLY against objects on ~the UFO's LAYER.
                    // An object more than DEPTH_HIT_BAND away in depth is on a different plane, so the
                    // UFO flies over/under it (no hit) — pinch to change layer is a real dodge. The
                    // hitbox tracks the UFO's pinch-scaled visual size, centred on the ship.
                    val effShip = shipSize * (SHIP_SCALE_MIN + shipDepth * (SHIP_SCALE_MAX - SHIP_SCALE_MIN))
                    val shipCx = shipX + shipSize / 2f
                    val shipCy = shipY + shipSize / 2f

                    // WORMHOLE dive check (depth-AGNOSTIC — you fly straight in regardless of layer):
                    // if the UFO's centre reaches the portal's core, kick off the black-hole warp. Clear
                    // the field + old-sector event state so the new universe starts clean.
                    val portal = sprites.firstOrNull { it.kind == Kind.WORMHOLE }
                    if (portal != null) {
                        val pcx = portal.xPx + portal.sizePx / 2f
                        val pcy = portal.yPx + portal.sizePx / 2f
                        if (hypot(shipCx - pcx, shipCy - pcy) < portal.sizePx * 0.42f) {
                            holeCenter = Offset(pcx, pcy)
                            sprites.clear()
                            stormActive = false
                            starfallPending = 0
                            eventBanner = null
                            warp = Warp.DIVE
                            return@withFrameNanos
                        }
                    }

                    val shalf = effShip / 2f * 0.82f
                    val sx1 = shipCx - shalf; val sx2 = shipCx + shalf
                    val sy1 = shipCy - shalf; val sy2 = shipCy + shalf
                    val hits = ArrayList<Sprite>()
                    for (s in sprites) {
                        if (s.kind == Kind.WORMHOLE) continue                     // portal isn't a hazard — handled above
                        if (abs(s.depth - shipDepth) > DEPTH_HIT_BAND) continue   // different layer → fly over/under
                        val ipad = s.sizePx * 0.18f
                        val ix1 = s.xPx + ipad; val ix2 = s.xPx + s.sizePx - ipad
                        val iy1 = s.yPx + ipad; val iy2 = s.yPx + s.sizePx - ipad
                        if (sx1 < ix2 && sx2 > ix1 && sy1 < iy2 && sy2 > iy1) hits.add(s)
                    }
                    if (hits.isNotEmpty()) {
                        sprites.removeAll(hits)
                        var didCollect = false   // caught a star / bonus photo → chime
                        var didHit = false       // took damage → thud
                        for (s in hits) {
                            when {
                                s.kind == Kind.STAR -> {
                                    hp = (hp + 1).coerceAtMost(MAX_HP)   // repair
                                    score += 2f                          // bonus
                                    didCollect = true
                                }
                                // A player's "collect" photo is a bonus — catching it scores, no damage.
                                s.kind == Kind.CUSTOM && s.custom?.role == ObjectRole.COLLECT -> {
                                    score += 3f
                                    didCollect = true
                                }
                                else -> {
                                    hp -= 1                              // damage (hazards + "dodge" photos)
                                    hitTick += 1
                                    didHit = true
                                }
                            }
                        }
                        // Fire one SFX slice per event type this frame; the sprite's voice pool lets a
                        // chime and a thud overlap if both happened at once.
                        if (didCollect) sfx.play("collect")
                        if (didHit) sfx.play("hit")
                        if (hp <= 0) {
                            best = max(best, score)
                            phase = Phase.GAME_OVER
                        }
                    }
                }
            }
        }

        // ---- Black-hole warp choreography ----
        // Runs when `warp` changes and chains the phases DIVE → TUNNEL → EMERGE → NONE. The DIVE/EMERGE
        // legs drive the UFO's transform (`warpScale`/`warpSpin` + position) with a short `animate`; the
        // TUNNEL leg swaps the backdrop `source` to the next sector (reloads on the same decoder) and
        // holds while the Star-Trek streak tunnel plays over it.
        LaunchedEffect(warp) {
            when (warp) {
                Warp.DIVE -> {
                    val sx0 = shipX; val sy0 = shipY
                    val tx = holeCenter.x - shipSize / 2f
                    val ty = holeCenter.y - shipSize / 2f
                    animate(0f, 1f, animationSpec = tween(720, easing = FastOutSlowInEasing)) { t, _ ->
                        shipX = lerp(sx0, tx, t)
                        shipY = lerp(sy0, ty, t)
                        warpScale = 1f - t          // shrink into the hole
                        warpSpin = t * 720f         // two full spins as it's sucked in
                    }
                    warp = Warp.TUNNEL
                }
                Warp.TUNNEL -> {
                    // Swap to the next universe (source change → reload on the same backdrop decoder)
                    // and name the new sector. The tunnel streaks cover the swap/buffer.
                    backdropIndex = (backdropIndex + 1) % SPACE_BACKDROPS.size
                    sector += 1
                    warpScale = 0f
                    eventBanner = "🌌 SECTOR $sector · ${SECTOR_NAMES[backdropIndex]}"
                    delay(1700)
                    warp = Warp.EMERGE
                }
                Warp.EMERGE -> {
                    // Reappear from a point near the top-centre of the new sector, un-spin and grow.
                    shipX = (widthPx - shipSize) / 2f
                    shipY = minShipY + (maxShipY - minShipY) * 0.35f
                    shipDepth = 0.5f
                    warpSpin = -360f
                    animate(0f, 1f, animationSpec = tween(720, easing = FastOutSlowInEasing)) { t, _ ->
                        warpScale = t
                        warpSpin = -360f * (1f - t)
                    }
                    warpScale = 1f; warpSpin = 0f
                    warp = Warp.NONE
                }
                Warp.NONE -> {
                    warpScale = 1f; warpSpin = 0f
                }
            }
        }

        // ---- Damage → animation mapping (this is the KMPMedia showcase) ----
        // As damage climbs, MORE primitives switch on AND their amplitude grows AND the cycle
        // shortens — a pristine UFO is perfectly still; a critical one shakes violently.
        // NOTE: the UFO deliberately NEVER uses ROTATE — a piloted craft rocks/shakes/flickers
        // under damage, it does not spin end-over-end. (The tumbling hazards do the spinning.)
        val shipAnims: Set<OGAnimationType> = when (damage) {
            0 -> emptySet()
            1 -> setOf(OGAnimationType.TRANSLATE)                                     // gentle rock
            2 -> setOf(OGAnimationType.TRANSLATE, OGAnimationType.SCALE)              // rock + strain
            else -> setOf(                                                            // + fade flicker
                OGAnimationType.TRANSLATE, OGAnimationType.SCALE, OGAnimationType.FADE,
            )
        }
        // Amplitude carries the "how hurt" signal since the primitive set caps at level 3+.
        // Kept gentle and CAPPED so a critical UFO *shakes harder* (faster + a touch more sway)
        // without ballooning in size (SCALE stays ≤ ~1.2×) — it reads as strain, not growth.
        val shipIntensity = (damage * 0.16f).coerceAtMost(0.55f)
        val shipDuration = (1100 - damage * 170).coerceAtLeast(360)

        // Pinch depth → the UFO's on-screen scale: grow it (toward 1f) to read as "closer to the
        // camera / higher", shrink it (toward 0f) to read as "further away / lower". Springed so a
        // pinch feels weighty rather than jumpy.
        val shipScale by animateFloatAsState(
            targetValue = SHIP_SCALE_MIN + shipDepth * (SHIP_SCALE_MAX - SHIP_SCALE_MIN),
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "shipScale"
        )

        // Level-up celebration: a springy scale-pop on the UFO + a transient banner.
        val pop = remember { Animatable(1f) }
        var banner by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(level, phase) {
            if (phase == Phase.PLAYING && level > 1) {
                banner = level
                sfx.play("levelup")             // celebratory sweep — the 3rd slice of the sprite
                pop.snapTo(1.35f)
                pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessLow))
                delay(1300)
                banner = null
            } else {
                banner = null
                pop.snapTo(1f)
            }
        }

        // Red damage flash on each hazard hit.
        val flash = remember { Animatable(0f) }
        LaunchedEffect(hitTick) {
            if (hitTick > 0) {
                flash.snapTo(0.55f)
                flash.animateTo(0f, tween(450))
            }
        }

        // ---- Layout: the game on top, the soft control-pad (OUT of the game) below ----
        Column(Modifier.fillMaxSize()) {

          // ===== PLAY FIELD (the game) — takes all the height above the pad =====
          Box(Modifier.fillMaxWidth().weight(1f)) {

            // ---- Space-travel backdrop (real streaming video, behind everything) ----
            // An OGAVPlayer playing SPACE_BACKDROP that fills the FIELD (not the pad strip below):
            // contentScale = FILL covers the field (no bars) and autoRepeat loops the 15s clip
            // seamlessly. It's declared first inside the field, so it draws underneath the
            // sprites/UFO/HUD. A transparent backgroundColor lets the outer gradient show through
            // while it buffers (and if it ever fails to load) — so there's never a black flash.
            OGAVPlayer(
                action = OGAVPlayerAction.PLAY,
                // The backdrop for the current SECTOR. A warp bumps backdropIndex → new source →
                // OGAVPlayer reloads the clip on the SAME decoder, so the sky changes with zero extra
                // decoders. Keyed on backdropIndex so the source object is rebuilt only on a real swap.
                source = remember(backdropIndex) { OGVideoUrlType(SPACE_BACKDROPS[backdropIndex]) },
                config = OGPlayerConfig(
                    showDefaultControls = false,
                    displayMovable = false,
                    displayShape = OGShapeType.RECTANGLE,
                    cornerRadius = 0.dp,                     // full-bleed, no rounded corners
                    backgroundColor = Color.Transparent,     // gradient shows through while buffering
                    contentScale = OGVideoScale.FILL,        // fill the field, crop the 16:9 sides
                    playbackConfig = OGVideoPlaybackConfig(autoStart = true, autoRepeat = true)
                ),
                modifier = Modifier.fillMaxSize(),
                controller = backdropController,   // exposes live position; also drives the cosmic clock
                cues = cues,                       // fires the storm / starfall events off the timeline
                onError = { }
            )
            // Storm vignette: a soft red edge-glow while the METEOR STORM cue is active.
            if (stormActive) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(9f)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color.Transparent, Color(0x55FF2D2D)),
                                radius = 1400f
                            )
                        )
                )
            }
            // falling sprites — keyed by stable id so culling/spawning doesn't recycle a
            // composable slot onto a different Sprite (which would swap its animation mid-flight).
            for (s in sprites) {
                key(s.id) {
                    val spec = s.video
                    val custom = s.custom
                    // Depth-of-field + draw order come from the library: Modifier.ogDepth(depth, focal)
                    // derives blur/dim from the object's distance to the UFO's focal depth AND sets its
                    // zIndex, so the UFO literally flies over or under each object as it pinches depth.
                    if (s.kind == Kind.CUSTOM && custom != null) {
                        // The player's own cropped photo, dropped into gameplay: OGImageView crops it
                        // to the chosen shape (zoom/pan/corner all carried on the spec) and an
                        // OGAnimatedContainer makes it spin/twinkle as it drifts — the "crop into a
                        // shape" + "animate" combo, now on a user image. The photo is decoded once
                        // (downscaled, off the UI thread) and reused across every copy, so many
                        // objects share one small bitmap — a GPU clip on top.
                        Box(
                            Modifier
                                .offset { IntOffset(s.xPx.roundToInt(), s.yPx.roundToInt()) }
                                .size(with(density) { s.sizePx.toDp() })
                                .ogDepth(s.depth, shipDepth, SPRITE_DEPTH)
                        ) {
                            OGAnimatedContainer(
                                animations = custom.animations(),
                                durationMillis = Kind.CUSTOM.durationMillis(),
                                intensity = 0.5f,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                OGImageView(
                                    source = remember(s.id) { custom.source.toSource() },
                                    displayShape = custom.shape,
                                    cornerRadius = custom.cornerRadiusDp.dp,
                                    contentScale = cropContentScale(custom.crop, custom.zoom),
                                    alignment = BiasAlignment(custom.biasX, custom.biasY),
                                    modifier = Modifier.fillMaxSize(),
                                    onError = { },
                                    onEventTriggered = { _, _ -> }
                                )
                            }
                        }
                    } else if (s.kind == Kind.WORMHOLE && spec != null) {
                        // The wormhole PORTAL: a real spinning space clip in a CIRCLE, framed by a
                        // glowing ring and rendered crisp + above the field (no depth blur) so it's an
                        // obvious "fly in here" target. OGAnimatedContainer makes the clip spin + pulse;
                        // the ring on the outer Box stays a steady halo. Flying the UFO into its centre
                        // triggers the black-hole warp (handled in the game loop).
                        Box(
                            Modifier
                                .offset { IntOffset(s.xPx.roundToInt(), s.yPx.roundToInt()) }
                                .size(with(density) { s.sizePx.toDp() })
                                .zIndex(8f)
                                .border(3.dp, Color(0xAAB794F6), CircleShape)
                        ) {
                            OGAnimatedContainer(
                                animations = setOf(OGAnimationType.ROTATE, OGAnimationType.SCALE),
                                durationMillis = spec.durationMs,
                                intensity = 0.5f,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                OGAVPlayer(
                                    action = OGAVPlayerAction.PLAY,
                                    source = remember(s.id) { OGVideoUrlType(spec.url) },
                                    config = OGPlayerConfig(
                                        showDefaultControls = false,
                                        displayMovable = false,
                                        displayShape = OGShapeType.CIRCLE,
                                        cornerRadius = 0.dp,
                                        backgroundColor = Color(0x66101A3A),
                                        contentScale = OGVideoScale.FILL,
                                        playbackConfig = OGVideoPlaybackConfig(
                                            autoStart = true,
                                            autoRepeat = true
                                        )
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                    onError = { }
                                )
                            }
                        }
                    } else if (s.kind == Kind.COSMIC && spec != null) {
                        // Real-video object: a space clip clipped to a shape (FILL, so it fills the
                        // shape edge-to-edge) and wrapped in OGAnimatedContainer so it also moves.
                        // A faint space-blue backdrop keeps the object visible while it buffers.
                        Box(
                            Modifier
                                .offset { IntOffset(s.xPx.roundToInt(), s.yPx.roundToInt()) }
                                .size(with(density) { s.sizePx.toDp() })
                                // Video preset = dim + z-order, no blur (a RenderEffect over a video
                                // surface is unreliable) — see OGDepthConfig.Video.
                                .ogDepth(s.depth, shipDepth, VIDEO_DEPTH)
                        ) {
                            OGAnimatedContainer(
                                animations = spec.anims,
                                durationMillis = spec.durationMs,
                                intensity = 0.5f,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                OGAVPlayer(
                                    action = OGAVPlayerAction.PLAY,
                                    source = remember(s.id) { OGVideoUrlType(spec.url) },
                                    config = OGPlayerConfig(
                                        showDefaultControls = false,
                                        displayMovable = false,
                                        displayShape = spec.shape,
                                        cornerRadius = 12.dp,
                                        backgroundColor = Color(0x66101A3A),
                                        contentScale = OGVideoScale.FILL,
                                        playbackConfig = OGVideoPlaybackConfig(
                                            autoStart = true,
                                            autoRepeat = true
                                        )
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                    onError = { }
                                )
                            }
                        }
                    } else {
                        OGAnimatedImage(
                            source = remember(s.kind) { OGSvgResourceFileType(s.kind.asset) },
                            animations = s.kind.animations(),
                            durationMillis = s.kind.durationMillis(),
                            intensity = if (s.kind == Kind.STAR) 0.5f else 0.55f,
                            width = svgSideFor(s.sizePx),
                            height = svgSideFor(s.sizePx),
                            modifier = Modifier
                                .offset { IntOffset(s.xPx.roundToInt(), s.yPx.roundToInt()) }
                                .size(with(density) { s.sizePx.toDp() })
                                .ogDepth(s.depth, shipDepth, SPRITE_DEPTH),
                            onError = { }
                        )
                    }
                }
            }

            // the UFO — static ufo.svg, animated ENTIRELY by KMPMedia off the damage level,
            // with a level-up pop layered on via an outer graphicsLayer.
            Box(
                Modifier
                    .offset { IntOffset(shipX.roundToInt(), shipY.roundToInt()) }
                    .size(with(density) { shipSize.toDp() })
                    // zIndex = the UFO's pinch depth → it slots into the SAME z-stack as the objects,
                    // so growing it (depth→1) lifts it in FRONT of them and shrinking it (depth→0)
                    // drops it BEHIND. shipScale grows/shrinks the sprite for the 3D "closer/further"
                    // read; the level-up pop multiplies on top. During a warp the UFO jumps above the
                    // tunnel overlay (zIndex 55) and its transform switches to the warp choreography
                    // (spiral spin + shrink into / grow out of the hole).
                    .zIndex(if (warp == Warp.NONE) shipDepth else 55f)
                    .graphicsLayer {
                        val s = pop.value * (if (warp == Warp.NONE) shipScale else warpScale)
                        scaleX = s; scaleY = s
                        rotationZ = warpSpin
                    }
            ) {
                OGAnimatedImage(
                    source = remember { OGSvgResourceFileType("ufo") },
                    animations = shipAnims,
                    durationMillis = shipDuration,
                    intensity = shipIntensity,
                    width = svgSideFor(shipSize),
                    height = svgSideFor(shipSize),
                    // Small horizontal jitter for the "rock": at max intensity the sway is only
                    // ~±7px, so a damaged UFO never slides far enough to clip at a screen edge.
                    // (The UFO deliberately has no ROTATE — it rocks/shakes, it does not spin.)
                    slidePx = 12f,
                    modifier = Modifier.fillMaxSize(),
                    onError = { }
                )
            }

            // ---- Black-hole warp tunnel (Star-Trek streak field) ----
            // A full-field radial streak overlay (Compose Canvas, GPU-only — no decoder) that plays only
            // during a warp. Its intensity is tied to the UFO's warpScale: streaks build as the ship is
            // sucked in (DIVE), peak while the backdrop swaps (TUNNEL), and recede as it flies back out
            // (EMERGE). Sits below the UFO's warp zIndex (55) so you watch the ship dive into / out of it.
            if (warp != Warp.NONE) {
                WarpTunnel(
                    intensity = (1f - warpScale).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxSize().zIndex(50f)
                )
            }

            // ---- HUD (top-left of the field) — zIndex above the depth-stacked sprites ----
            Column(Modifier.zIndex(10f).padding(16.dp)) {
                Text(
                    "❤".repeat(hp.coerceAtLeast(0)) + "🖤".repeat((MAX_HP - hp).coerceIn(0, MAX_HP)),
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "LVL $level   ·   ⏱ ${score.format1()}s   ·   🏆 ${best.format1()}s",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (phase == Phase.PLAYING) {
                    Spacer(Modifier.height(6.dp))
                    DepthMeter(shipDepth)
                    Spacer(Modifier.height(6.dp))
                    // The backdrop video's live position + where the cue events fire on its loop.
                    CosmicClock(
                        controller = backdropController,
                        stormFrom = 0.30f, stormTo = 0.62f, starAt = 0.82f,
                        stormActive = stormActive,
                    )
                }
            }

            // ---- Level-up banner (centred in the field) ----
            banner?.let { lvl ->
                Box(Modifier.fillMaxSize().zIndex(10f), contentAlignment = Alignment.Center) {
                    Text(
                        "⭐ LEVEL $lvl ⭐",
                        color = Color(0xFFFDE047),
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        modifier = Modifier
                            .background(Color(0xAA141A3A), RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }

            // ---- Cosmic-event banner (top of field): fired by the backdrop video's cues ----
            eventBanner?.let { text ->
                val storm = text.startsWith("☄")
                Box(Modifier.fillMaxSize().zIndex(12f), contentAlignment = Alignment.TopCenter) {
                    Text(
                        text,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .padding(top = 92.dp)
                            .background(
                                if (storm) Color(0xFFE11D2A) else Color(0xFFF59E0B),  // fully opaque
                                RoundedCornerShape(22.dp)
                            )
                            .border(
                                2.dp,
                                Color.White.copy(alpha = 0.9f),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    )
                }
            }

            // ---- Pinch layer: a transparent, top-of-field capture surface for the two-finger
            // pinch that drives the UFO's DEPTH. It sits at the highest zIndex so it's hit-tested
            // first (ahead of the video backdrop's own clickable) — but it only reacts to a real
            // pinch (zoom != 1): single-finger taps/drags aren't consumed and fall through, so
            // steering stays entirely on the pad below. Pinch OUT → grow/rise/over, IN → shrink/
            // recede/under. Only present while PLAYING.
            if (phase == Phase.PLAYING && controlMode == ControlMode.PINCH && warp == Warp.NONE) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    shipDepth = (shipDepth + (zoom - 1f) * 2.4f).coerceIn(0f, 1f)
                                }
                            }
                        }
                )
            }
          }

          // ===== SOFT CONTROL-PAD (out of the game) — steers the UFO in 2 axes =====
          SoftPad(
              // Steering is frozen mid-warp (the UFO is on the dive/emerge rails) — re-enabled on EMERGE.
              enabled = phase == Phase.PLAYING && warp == Warp.NONE,
              onVector = { padVec = it },
              modifier = Modifier.fillMaxWidth().height(padHeightDp)
          )
        }

        // ---- Damage flash overlay (full-screen, above the layout) ----
        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().background(Color(0xFFFF3B3B).copy(alpha = flash.value)))
        }

        // ---- Ready / Game-over overlay ----
        if (phase != Phase.PLAYING && !showSettings) {
            val customLine = if (customObjects.isNotEmpty())
                " ✨ Your ${customObjects.size} cropped photo${if (customObjects.size == 1) "" else "s"} " +
                    "join the field too — collect the bonus ones, dodge the hazard ones."
            else ""
            Overlay(
                title = if (phase == Phase.READY) "🛸 UFO Dodge" else "💥 Destroyed!",
                subtitle = if (phase == Phase.READY)
                    "Use the control pad below the screen to fly your UFO — push left/right to " +
                        "steer, up/down to move forward and back. 🤏 Pinch with two fingers to change " +
                        "DEPTH: grow the UFO to rise ABOVE objects and fly over them, shrink it to drop " +
                        "BELOW and slip under — objects on another layer blur out of focus and can't " +
                        "touch you. Dodge tumbling meteorites ☄️, hostile ships 🛸, and 🌌 cosmic " +
                        "anomalies — real space video clipped into circles, diamonds & triangles, " +
                        "spinning and pulsing as they drift. Every hit damages you; grab ⭐ stars to " +
                        "repair. ${MAX_HP} hits and you're done. 🌀 And every so often a WORMHOLE opens — " +
                        "fly straight into it to dive through a black hole (warp-speed star streaks!) and " +
                        "come out in a whole new sector of the universe, then keep flying." +
                        customLine
                else
                    "Reached level $level · survived ${score.format1()}s · best ${best.format1()}s",
                button = if (phase == Phase.READY) "Launch" else "Play again",
                onClick = { phase = Phase.PLAYING },
                secondaryButton = "⚙️ Customize objects" +
                    if (customObjects.isNotEmpty()) " (${customObjects.size})" else "",
                onSecondary = { showSettings = true },
                // Control-scheme picker — only offered when the device has a motion sensor.
                extra = if (tiltSupported) {
                    { ControlPicker(controlMode) { controlMode = it } }
                } else null
            )
        }

        // ---- Settings: crop your own photos into game objects (drawn on top of everything) ----
        if (showSettings) {
            GameObjectSettings(
                objects = customObjects,
                onAdd = { customObjects.add(it) },
                onRemove = { id -> customObjects.removeAll { it.id == id } },
                onDone = { showSettings = false },
                nextId = { nextCustomId++ }
            )
        }
    }
}

/**
 * Tiny HUD readout of the UFO's current depth "layer": a rail with a thumb that slides from
 * UNDER (left, deepest — the UFO is behind everything) to OVER (right, nearest — in front of
 * everything). Purely a visualisation of [depth] so the pinch → layer relationship is legible.
 */
@Composable
private fun DepthMeter(depth: Float) {
    val density = LocalDensity.current
    val trackW = 116.dp
    val thumb = 12.dp
    val travelPx = with(density) { (trackW - thumb).toPx() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("under", color = Color(0x99FFFFFF), fontSize = 10.sp)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .width(trackW)
                .height(8.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier
                    .offset { IntOffset((depth * travelPx).roundToInt(), 0) }
                    .size(thumb)
                    .background(Color(0xFF7DD3FC), CircleShape)
                    .border(1.dp, Color(0xAAFFFFFF), CircleShape)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("over", color = Color(0x99FFFFFF), fontSize = 10.sp)
    }
}

/**
 * A slim "cosmic clock" HUD bar: the backdrop video's live position ([OGAVPlayerController.status]
 * `.progress`), with the storm segment shaded and a starfall tick — so you can see a cue event
 * approaching on the loop. Reading the status INSIDE this composable scopes the ~5 Hz recomposition
 * to just this bar (the rest of the game stays on its 60 fps frame loop).
 */
@Composable
private fun CosmicClock(
    controller: OGAVPlayerController,
    stormFrom: Float,
    stormTo: Float,
    starAt: Float,
    stormActive: Boolean,
) {
    val density = LocalDensity.current
    val progress = controller.status.value.progress
    val trackW = 132.dp
    val trackPx = with(density) { trackW.toPx() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🛰", fontSize = 11.sp)
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .width(trackW)
                .height(9.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // storm segment band (where the METEOR STORM range cue is active)
            Box(
                Modifier
                    .offset { IntOffset((trackPx * stormFrom).roundToInt(), 0) }
                    .width(with(density) { (trackPx * (stormTo - stormFrom)).toDp() })
                    .height(9.dp)
                    .background(Color(0x55FF3B3B), RoundedCornerShape(4.dp))
            )
            // played fill (turns hot while a storm is live)
            Box(
                Modifier
                    .width(with(density) { (trackPx * progress).toDp() })
                    .height(9.dp)
                    .background(
                        if (stormActive) Color(0xFFFF6B6B) else Color(0xFF7DD3FC),
                        RoundedCornerShape(4.dp)
                    )
            )
            // starfall tick (where the point cue fires)
            Box(
                Modifier
                    .offset { IntOffset((trackPx * starAt).roundToInt(), 0) }
                    .width(3.dp)
                    .height(9.dp)
                    .background(Color(0xFFF59E0B))
            )
        }
    }
}

/**
 * The **Star-Trek warp tunnel** drawn over the field during a black-hole [Warp]: a field of light
 * streaks flying radially outward from the centre, over a dark vortex, slowly rotating — the classic
 * "jump to warp" look. Pure Compose [Canvas], GPU-only (no video decoder), so it layers cheaply on top
 * of the game while the backdrop video swaps to the next sector behind it.
 *
 * [intensity] (0..1) scales streak length, brightness and the vortex — the caller ramps it up as the
 * UFO is sucked in, holds it at 1 during the swap, and ramps it back down as the UFO flies out.
 */
@Composable
private fun WarpTunnel(intensity: Float, modifier: Modifier = Modifier) {
    // Fixed per-streak params (angle, staggered start phase, speed/length seed). The golden angle
    // (137.5°) spreads the angles evenly around the circle without random clumping.
    val streaks = remember {
        List(150) { i ->
            Triple(
                (i * 137.50776f) % 360f,          // angle (deg)
                (i * 0.61803399f) % 1f,           // start phase 0..1 (golden-ratio stagger)
                0.35f + (i % 7) / 7f * 0.65f,      // per-streak speed + length seed
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "warp")
    val t by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(750, easing = LinearEasing)), label = "streak"
    )
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "spin"
    )
    Box(
        modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    Color(0xFF000010).copy(alpha = intensity),
                    Color(0xFF0A0A2A).copy(alpha = intensity * 0.6f),
                    Color.Transparent,
                )
            )
        )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = hypot(cx, cy)
            rotate(spin, pivot = Offset(cx, cy)) {
                for ((angDeg, base, seed) in streaks) {
                    val phase = (base + t * (0.5f + seed)) % 1f       // travels outward, loops
                    val r = phase * maxR
                    val len = (28f + seed * 120f) * intensity * (0.25f + phase)  // longer as it flies out
                    val alpha = (intensity * (1f - abs(phase - 0.55f) * 1.4f)).coerceIn(0f, 1f)
                    if (alpha <= 0.01f) continue
                    val a = (angDeg / 180f * PI).toFloat()
                    val ca = cos(a); val sa = sin(a)
                    drawLine(
                        color = Color(0xFFBFE3FF).copy(alpha = alpha),
                        start = Offset(cx + ca * r, cy + sa * r),
                        end = Offset(cx + ca * (r + len), cy + sa * (r + len)),
                        strokeWidth = 1.5f + seed * 2.5f,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun Overlay(
    title: String,
    subtitle: String,
    button: String,
    onClick: () -> Unit,
    secondaryButton: String? = null,
    onSecondary: () -> Unit = {},
    extra: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .background(Color(0xFF141A3A), RoundedCornerShape(20.dp))
                .padding(28.dp)
        ) {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color(0xFFB9C0E0), fontSize = 14.sp)
            if (extra != null) {
                Spacer(Modifier.height(16.dp))
                extra()
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onClick) { Text(button) }
            if (secondaryButton != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    secondaryButton,
                    color = Color(0xFFB9C0E0),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .background(Color(0xFF20284D), RoundedCornerShape(12.dp))
                        .clickable { onSecondary() }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** Pinch-vs-Tilt control-scheme picker shown on the game's start/again overlay. */
@Composable
private fun ControlPicker(mode: ControlMode, onSelect: (ControlMode) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Depth control", color = Color(0xFF8C93B8), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ControlChip("🤏 Pinch", mode == ControlMode.PINCH) { onSelect(ControlMode.PINCH) }
            ControlChip("📱 Tilt", mode == ControlMode.TILT) { onSelect(ControlMode.TILT) }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (mode == ControlMode.TILT)
                "Tilt: tip the top back = rise/over, forward = dive/under. Joystick still steers."
            else "Pinch: two fingers out = rise/over, in = dive/under.",
            color = Color(0x99B9C0E0),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ControlChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFFB9C0E0),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        modifier = Modifier
            .background(
                if (selected) Color(0xFF3B57E0) else Color(0xFF20284D),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * A soft **analog control-pad** that lives OUTSIDE the play field (its own strip at the bottom of
 * the screen). Drag a finger on it to steer the UFO in BOTH axes — left/right AND forward/backward
 * — as a velocity stick: the further the knob is pushed from centre, the faster the UFO travels
 * that way; let go and the knob springs back to centre and the UFO stops where you left it.
 *
 * It's also a clean illustration of KMPMedia's **layering**: the layering itself is plain Compose
 * (a base ring, with the knob drawn on top by simple z-order — the same trick the game uses to put
 * sprites over the video backdrop), and KMPMedia supplies the *motion* — the knob is wrapped in
 * [OGAnimatedContainer] so it gently breathes (SCALE) to invite a touch when idle, the very same
 * "wrap any content, get animation" primitive the sprites use. Touch tracking is Compose gestures.
 *
 * @param enabled only steers while true (the game is PLAYING); ignores touches otherwise
 * @param onVector emits the normalised stick vector on every change: x,y ∈ -1..1, y+ = backward
 */
@Composable
private fun SoftPad(
    enabled: Boolean,
    onVector: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val knobDiameter = 64.dp
    val knobRadiusPx = with(density) { (knobDiameter / 2).toPx() }

    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var pointer by remember { mutableStateOf(Offset.Zero) }   // finger pos within the pad (px)
    var knobTarget by remember { mutableStateOf(Offset.Zero) } // knob offset from centre (px), clamped
    var active by remember { mutableStateOf(false) }
    // Spring-smoothed knob position for the visual (snappy while dragging, a little bounce on release).
    val knob by animateOffsetAsState(
        targetValue = knobTarget,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "knob"
    )

    // Largest distance the knob centre may travel from the pad centre (keeps the knob inside the pad).
    fun maxRadiusPx(): Float =
        (minOf(padSize.width, padSize.height) / 2f - knobRadiusPx).coerceAtLeast(1f)

    // Recompute the knob offset + emit the normalised stick vector from the current finger position.
    fun update() {
        val cx = padSize.width / 2f
        val cy = padSize.height / 2f
        val dx = pointer.x - cx
        val dy = pointer.y - cy
        val dist = hypot(dx, dy)
        val r = maxRadiusPx()
        val clamped = min(dist, r)
        val ux = if (dist > 0f) dx / dist else 0f
        val uy = if (dist > 0f) dy / dist else 0f
        knobTarget = Offset(ux * clamped, uy * clamped)
        onVector(Offset(ux * (clamped / r), uy * (clamped / r)))
    }

    fun release() {
        active = false
        knobTarget = Offset.Zero
        onVector(Offset.Zero)
    }

    // If steering gets disabled mid-touch (e.g. game over), recentre the knob and stop the UFO.
    LaunchedEffect(enabled) { if (!enabled) release() }

    Box(
        modifier = modifier
            .background(Color(0xFF0A0E24))
            .onSizeChanged { padSize = it }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos -> active = true; pointer = pos; update() },
                    onDrag = { change, drag -> change.consume(); pointer += drag; update() },
                    onDragEnd = { release() },
                    onDragCancel = { release() },
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "◀  steer  ▶     ▲ forward · back ▼",
            color = Color(0x66FFFFFF),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        )

        // ---- base ring (plain Compose — the bottom layer) ----
        Box(
            Modifier
                .size(140.dp)
                .background(Color(0x14FFFFFF), CircleShape)
                .border(1.dp, Color(0x33FFFFFF), CircleShape)
        )

        // ---- knob (KMPMedia-animated), layered on top of the ring, offset toward the finger ----
        Box(
            Modifier
                .offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                .size(knobDiameter)
        ) {
            OGAnimatedContainer(
                animations = if (active) emptySet() else setOf(OGAnimationType.SCALE),
                durationMillis = 1600,
                intensity = 0.35f,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF7DD3FC), Color(0xFF2563EB))),
                            CircleShape
                        )
                        .border(2.dp, Color(0x66FFFFFF), CircleShape)
                )
            }
        }
    }
}

/**
 * Weighted spawn. ~12% of the time (while under [COSMIC_CAP]) it's a big, slow **real-video**
 * cosmic object; if the player has added their own cropped photos, ~28% of the time it's one of
 * those ([Kind.CUSTOM]); otherwise a cheap static-SVG hazard (~55% meteorite · ~27% hostile · ~18%
 * star). Cosmic objects are larger and drift slower so they have time to buffer and be admired.
 */
/** How many bonus stars a 🌟 STARFALL cue drops. */
private const val STARFALL_COUNT = 6

/**
 * A guaranteed ⭐ STAR (collectible), used by the cue-driven STARFALL burst. Biased toward the
 * middle depth band so it lands near the UFO's usual layer and is actually catchable.
 */
private fun spawnStar(id: Long, widthPx: Float, densityScale: Float): Sprite {
    val sizePx = Random.nextInt(28, 46).toFloat() * densityScale
    val x = Random.nextFloat() * (widthPx - sizePx).coerceAtLeast(1f)
    val speed = Random.nextInt(120, 175).toFloat() * densityScale
    return Sprite(id, Kind.STAR, sizePx, x, speed, depth = 0.35f + Random.nextFloat() * 0.30f)
}

/**
 * A **wormhole portal** — a big, slow real-video circle the player flies INTO to trigger the
 * black-hole warp (see [Warp]). It drifts at a steady slow pace (the game loop exempts it from the
 * difficulty ramp) so it's reachable at any level, and carries the shared wormhole clip as its video.
 */
private fun spawnWormhole(id: Long, widthPx: Float, densityScale: Float): Sprite {
    val sizePx = Random.nextInt(150, 190).toFloat() * densityScale
    val x = Random.nextFloat() * (widthPx - sizePx).coerceAtLeast(1f)
    val speed = 62f * densityScale   // slow, ramp-exempt drift → easy to line up and dive in
    return Sprite(
        id, Kind.WORMHOLE, sizePx, x, speed,
        depth = 0.5f,
        video = VideoSpec(WORMHOLE_CLIP, OGShapeType.CIRCLE, setOf(OGAnimationType.ROTATE), 4200),
    )
}

private fun spawn(
    id: Long,
    widthPx: Float,
    densityScale: Float,
    videoCount: Int,
    customs: List<CustomObjectSpec>,
): Sprite {
    val roll = Random.nextInt(100)

    // Real-video cosmic object — gated by the concurrent-decoder cap.
    if (roll < 12 && videoCount < COSMIC_CAP) {
        val spec = COSMIC_CLIPS[Random.nextInt(COSMIC_CLIPS.size)]
        val sizePx = Random.nextInt(96, 150).toFloat() * densityScale   // big showcase
        val x = Random.nextFloat() * (widthPx - sizePx).coerceAtLeast(1f)
        val speed = Random.nextInt(70, 115).toFloat() * densityScale     // slow drift
        return Sprite(id, Kind.COSMIC, sizePx, x, speed, depth = Random.nextFloat(), video = spec)
    }

    // Player-authored cropped-photo object (only if any were added). Mid-sized so the crop is
    // clearly visible, mid-speed so it's catchable/dodgeable. The photo decodes once (downscaled,
    // cached) and every copy reuses it, so spawning many is cheap.
    if (customs.isNotEmpty() && roll < 40) {
        val spec = customs[Random.nextInt(customs.size)]
        val sizePx = Random.nextInt(72, 112).toFloat() * densityScale
        val x = Random.nextFloat() * (widthPx - sizePx).coerceAtLeast(1f)
        val speed = Random.nextInt(120, 185).toFloat() * densityScale
        return Sprite(id, Kind.CUSTOM, sizePx, x, speed, depth = Random.nextFloat(), custom = spec)
    }

    val kind = when {
        roll < 60 -> Kind.METEORITE
        roll < 85 -> Kind.HOSTILE
        else -> Kind.STAR
    }
    val sizePx = (Random.nextInt(24, 42).toFloat()) * densityScale
    val x = Random.nextFloat() * (widthPx - sizePx).coerceAtLeast(1f)
    // stars drift a touch slower so they're catchable; hazards are quicker.
    val base = if (kind == Kind.STAR) Random.nextInt(130, 190) else Random.nextInt(160, 270)
    val speed = base.toFloat() * densityScale
    return Sprite(id, kind, sizePx, x, speed, depth = Random.nextFloat())
}

private fun Float.format1(): String {
    val v = (this * 10).roundToInt()
    return "${v / 10}.${v % 10}"
}
