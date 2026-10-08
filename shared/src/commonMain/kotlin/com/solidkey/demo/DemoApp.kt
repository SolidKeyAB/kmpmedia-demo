package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ui.test.TestMainUI

/** Top-level demo destinations. */
enum class DemoScreen(val title: String, val emoji: String, val blurb: String) {
    Animate("Animate a Static Image", "✨", "Take a fully-static SVG and wrap it in KMPMedia's animation primitives — toggle Scale/Rotate/Fade/Slide and watch it come alive."),
    RuntimeSvg("Runtime-editable SVG", "🎨", "Load an SVG once, then change any node by id at runtime — recolour, rotate, move — bound to Compose state. A live gauge whose needle and zones follow a slider. 'SVG as a live template', the same code on Android & iOS."),
    AiVector("AI vector (describe → shape)", "🤖", "KMPMedia's vector primitives are just data, so a language model can produce them. Decode a model's JSON reply into a live polygon lasso clipping a photo, and into an SVG patch driving a gauge — via OGAiVector, with no AI SDK or network in the library."),
    Video("Video Playback", "🎬", "Stream a clip through one cross-platform OGAVPlayer (ExoPlayer on Android, AVPlayer on iOS) and re-frame it live inside any shape — circle, triangle, diamond — with transport controls, loop, and load any URL."),
    MorphClip("Morph the clip itself", "🫧", "The clip MASK animates circle → diamond → triangle → lasso while a video (and a GIF) keep playing underneath. One OGMorphShape drives every surface's clipShape — clipping moving media to a morphing outline, 60fps, same code on Android & iOS."),
    BoilingLines("Boiling lines (moving style)", "🖊️", "A prototype 'moving style': OGBoil jitters every vertex of an outline a few times a second — the hand-drawn 'living line'. Drawn as a stroke it's a wiggly line; dropped into a clipShape it's a living media edge. Pure, deterministic, zero-dependency. The first member of a procedural com.solidkey.painpoints.style family."),
    Particles("Particles & parametric shapes", "🎆", "Two data-defined generators from com.solidkey.painpoints.particle / .shape. A particle effect (rate, gravity, colour-over-life, shape) that OGParticleView compiles once and simulates on the frame clock — six presets + a live JSON editor. And parametric outlines (star / gear / flower / squircle / blob) wrapped in an OGPolygonShape clipShape, generated once and spun on the GPU. Both author-as-JSON, both 60fps, same code on Android & iOS."),
    Slash("Breathing-slash ribbons", "🗡️", "The signature anime-action effect: a glowing tapered arc that draws on along a path and stays alive with a flowing edge + breathing width. One OGSlashView primitive wears three elements chosen by data — a water sweep, a flame lick, a thunder bolt — composable with droplet/ember/spark particles. The centreline is sampled once; each frame only rebuilds the tapered outline + an additive-glow gradient fill. Author-as-JSON, 60fps, frame-identical on Android & iOS."),
    ActionFx("Action FX pack", "⚡", "The stylised motion-FX family from com.solidkey.painpoints.fx: forked lightning (OGLightningView), manga speed/impact lines (OGSpeedLinesView), elemental particle presets (petals/embers/droplets/leaves), the ogGlow + ogAfterImage modifiers, and a composed 'action beat' assembling several on one stage. Each effect is plain serializable data; all pure maths, 60fps, identical on Android & iOS."),
    Looks("Live looks", "🌈", "A colour grade is just data: an OGLookSpec (brightness / contrast / saturation / temperature / tint / hue) compiles once into one ColorFilter you drop on ANY graphic with Modifier.ogLook — plus blend modes for compositor layers. One GPU colour op, zero per-frame allocation, 60fps, pixel-identical on Android & iOS. Looks serialize to a .look JSON a model can author, like OGAiVector / OGStyles."),
    SoftMask("Soft & multi-region masks", "🪶", "Beyond a single hard-edged shape: feather a photo's edge into a vignette (softEdge), fade it along a gradient (maskBrush), or clip it to more than one region at once — two portholes, a bitten diamond — with OGMultiRegionShape. One offscreen DstIn pass / a path op, zero per-frame cost, same code on Android & iOS."),
    Compositor("Compositor + export", "📽️", "The flagship: compose several layers on ONE timeline, animate them with keyframed x/y/scale/rotation/opacity, preview live at 60fps — then export the whole scene to a single shareable animated GIF, all on device. OGComposition + OGCompositionView + exportGif(), pure commonMain, zero deps. No other KMP library does compose-and-export."),
    Interactive("Interactive shapes", "🤹", "Grab a shape-clipped photo and drag it, pinch to zoom, twist to rotate — then fling it and watch it spring back. Modifier.ogInteractive adds the transform + momentum + spring; OGHitArea means only touches inside the actual silhouette grab it, not its bounding box. Zero-dep, 60fps, same code on Android & iOS."),
    Scrabble("Scrabble tiles (drag & drop)", "🔠", "A drag-and-drop tile game built only from shipped primitives: drag lettered tiles from the rack onto a real 15×15 board, they snap into the grid and click as they land. The drag is one Modifier.ogInteractive; the click is an OGAudioSprite slice. Spell K·M·P·M·E·D·I·A across the ★. The rules (grid, snap, occupancy) are ~30 lines of plain app code — showing where the library ends and your game begins."),
    GraphicButton("Any graphic → a button", "🔘", "KMPMedia ships no button component — but one Modifier.ogButton turns any graphic (an SVG, a lasso-cut photo, a plain shape) into a real pressable button: a shape-aware tap (only inside the silhouette, via OGHitArea), a pressed-state visual (Scale / Dim / a pop-art hard-shadow 'Brutalist' push-in), and onClick. The tap twin of ogInteractive's drag — zero-dep, same code on Android & iOS."),
    CropShape("Crop a Photo into a Shape", "✂️", "Pick a photo and crop it into a circle, triangle or diamond — the image twin of 'Video in any shape'. It's one Compose GPU clip drawn once, so there's zero performance cost."),
    AutoCutout("Auto-cutout (remove background)", "🪄", "Drop a photo, get the subject clipped out. The library turns a segmentation mask into a live polygon lasso (contour trace + simplify, pure commonMain) — with a zero-dependency chroma/luma-key segmenter for plain backgrounds, and a pluggable OGSegmenter for ML Kit / Vision / cloud models on cluttered scenes. No ML dependency in the library."),
    Gif("Animated GIF", "🎞️", "Point one OGImageView at a .gif and it plays — looping frames on Android (AnimatedImageDrawable) and iOS (Skia Codec), the same code. The shape clip that crops a photo works on the moving image too."),
    Camera("Live camera in any shape", "📷", "Point the live camera feed through any shape — a circle, triangle or diamond window on the world (the AR-sticker primitive). OGCameraPreview wires the platform camera with zero third-party dependency: Camera2 on Android, AVFoundation on iOS. Toggle the shape and front/back camera."),
    Parallax("Depth parallax", "🏔️", "A layered scene where each element sits at a different depth — drag to move the viewpoint and the layers slide by different amounts (far pinned, near moving most). Modifier.ogParallax on one GPU graphicsLayer, built on the same depth axis as Modifier.ogDepth. 60fps, same code on Android & iOS."),
    Joint("Jointed Shapes (a rig)", "🦾", "Connect shape-clipped photos at a single pixel with a movable angular limit. Drag a two-link arm — each joint stops at its limit — then hit 'Let it swing' to watch a clamped pendulum. The building block for articulated rigs and motion dynamics."),
    BodyRig("Add Your Head to a Body", "🧍", "The arms, legs and torso are already rigged. Pick a photo, clip it to a shape and it becomes the head — pinned at the neck joint you set — then tap Wave / Walk / Jumping jacks / Dance and watch the whole body move, your head riding along."),
    Playground("Feature Playground", "🎛️", "Load images, SVGs, video, audio & layers from any URL/resource — tune every config live."),
    Game("UFO Dodge (mini-game)", "🛸", "Every sprite is a static SVG animated by KMPMedia: dodge tumbling meteorites & hostile ships, grab stars to repair — and watch your UFO shake harder as damage rises. Tap ⚙️ Customize to crop your own photos into shapes and drop them into the field as game objects."),
    Exif("EXIF auto-rotate", "🔄", "Photos tagged with a non-default EXIF orientation (shot in portrait / upside down) decode UPRIGHT automatically — the decoder reads tag 0x0112 and rotates on both Android & iOS. The standard 8-orientation test set, rendered right-way-up."),
    EdgeCases("Edge Cases", "🧪", "Deliberately broken inputs — bad URLs, huge images, malformed SVG, unsupported formats — to verify onError fires cleanly."),
    Performance("Performance", "⚡", "Load-timing, many-layer stress and rapid transform benchmarks with live numbers.")
}

@Composable
fun DemoApp() {
    DemoTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            var current by remember { mutableStateOf<DemoScreen?>(null) }
            when (val screen = current) {
                null -> HomeScreen(onSelect = { current = it })
                else -> DemoScaffold(title = screen.title, onBack = { current = null }) {
                    when (screen) {
                        DemoScreen.Animate -> AnimateScreen()
                        DemoScreen.RuntimeSvg -> RuntimeSvgScreen()
                        DemoScreen.AiVector -> AiVectorScreen()
                        DemoScreen.Video -> VideoScreen()
                        DemoScreen.MorphClip -> MorphClipScreen()
                        DemoScreen.BoilingLines -> BoilingLinesScreen()
                        DemoScreen.Particles -> ParticlesScreen()
                        DemoScreen.Slash -> SlashScreen()
                        DemoScreen.ActionFx -> ActionFxScreen()
                        DemoScreen.Looks -> LooksScreen()
                        DemoScreen.SoftMask -> SoftMaskScreen()
                        DemoScreen.Compositor -> CompositorScreen()
                        DemoScreen.Interactive -> InteractiveScreen()
                        DemoScreen.Scrabble -> ScrabbleScreen()
                        DemoScreen.GraphicButton -> GraphicButtonScreen()
                        DemoScreen.CropShape -> CropShapeScreen()
                        DemoScreen.AutoCutout -> AutoCutoutScreen()
                        DemoScreen.Gif -> GifScreen()
                        DemoScreen.Camera -> CameraScreen()
                        DemoScreen.Parallax -> ParallaxScreen()
                        DemoScreen.Joint -> JointScreen()
                        DemoScreen.BodyRig -> BodyRigScreen()
                        DemoScreen.Playground -> TestMainUI()
                        DemoScreen.Game -> UfoGameScreen()
                        DemoScreen.Exif -> ExifScreen()
                        DemoScreen.EdgeCases -> EdgeCasesScreen()
                        DemoScreen.Performance -> PerformanceScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(onSelect: (DemoScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(Modifier.padding(vertical = 8.dp)) {
                Text("KMPMedia", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Cross-platform media & vector playground", color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(4.dp))
                Text("Built against the local library source.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
        items(DemoScreen.entries.size) { i ->
            val s = DemoScreen.entries[i]
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(s) },
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.emoji, fontSize = 34.sp)
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(s.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(2.dp))
                        Text(s.blurb, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

/** Simple back-bar scaffold that avoids experimental Material3 TopAppBar APIs. */
@Composable
fun DemoScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "← Back",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBack() }.padding(end = 16.dp)
            )
            Text(title, color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxSize()) { content() }
    }
}
