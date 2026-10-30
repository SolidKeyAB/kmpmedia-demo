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
    Stage("One JSON → a whole movie", "🎞️", "The OGStage capstone: a single serializable document = a timeline of layers (scene + particles + action-FX + styled-text titles) that ONE generator emits and ONE player plays. Tap a one-line prompt to watch a movie a language model wrote from it (STORM / HANAMI / RUSH), decoded from its raw JSON reply and played unchanged. Authored as data, played at 60fps, scrubbable, exportable to GIF/MP4. A media toolkit evolved into a model-authorable animation engine. Same code on Android & iOS."),
    Describe("Describe → movie (live)", "🪄", "Type a sentence and get a real, playable movie ON THE SPOT — no key, no network. The describe→movie loop closed end-to-end: OGOfflineStageAuthor reads the words for mood + subject and assembles a scene, particles, an action-FX beat and a styled title (same words = same movie). A real language model writes into the SAME OGStage format through the same screen (bring your own key via OGStageAuthoring.modelAuthor). Deterministic, 60fps, exportable, same code on Android & iOS. No other KMP animation library is model-authorable."),
    SuperX("Tap a hand-drawn hero (cutout)", "🦸", "OGStage's new raster-cutout layer: a REAL hand-drawn PNG placed on the stage as its own pixels (not a redrawn vector) and animated by frame-swap. One hero head blinks (open → half → shut bundled frames), drifts (bob + sway keyframe tracks) and springs with a nod when you TAP it (onTap → OGSpring BOUNCE). The paper-doll / Spine-style puppet primitive — a drawing turned movable, authored as pure OGStage data and round-tripped through the JSON codec. Same code on Android & iOS."),
    SuperXRig("SUPER X rig (a drawing, rigged)", "🦸", "The hand-drawn hero as a MOVABLE puppet, done the honest way: the whole clean flying DRAWING is the body (head, torso, cape, legs always correctly joined) and the raised fist is one separate layer cut from that SAME drawing — so it sockets back exactly and swings from the real shoulder. Pick a move (Idle / Wave / Fly / Dance): the figure floats + banks and the fist pumps. Rig-parts sliders nudge where a layer sits on its joint; Export dumps the layout to bake. Pure commonMain over the lib's resource loader + Compose transforms, same code on Android & iOS."),
    LivingRig("Poke a living vector (rig)", "🫳", "A compiled illustration grabbable BY PART: OGRig.sceneJoints drops one semantic pin per region (body / head / arm / tail), OGSceneRigView warps the mesh under your finger and springs it home — or flip to code-driven and a scripted wave drives the SAME rig through the new `pose` param. Pure linear-blend skinning, 60fps, same code on Android & iOS."),
    Skin("Procedural skins (scales · hide · weave)", "🐊", "OGSkin grows a seamless, repeating material — reptile scales, an animal hide, a woven fabric — and fills any shape with Modifier.ogSkin. One cached tile + a GPU ImageShader (no per-pixel work, no runtime shader), plus optional raised-edge bumps. Author-as-JSON, 60fps, same code on Android & iOS."),
    Light("Light, shade, sheen & spot", "💡", "Four shape-aware lighting modifiers from com.solidkey.painpoints.light: a directional form-shade (ogLight), a lifted drop/contact shadow (ogShadow), a sweeping gloss (ogSheen) and a radial scene spotlight (ogSpot). Each a serializable spec with a JSON codec; no runtime shader, 60fps, same code on Android & iOS."),
    Fog("Fog · smoke · mist · steam", "🌫️", "OGFogView fills its bounds with soft radial-gradient puffs that drift and wander (fractal noise), wrapping toroidally into an endless seamless cloud. No per-pixel noise, no shader — deterministic from a seed, author-as-JSON (four presets), 60fps, same code on Android & iOS."),
    Control("One-call hand-drawn buttons", "🔘", "OGControl bundles a whole button — fill, a cached roughened ink edge, a tap + pressed-state, an optional seasonal garnish and a styled vector label — behind one composable + a serializable OGControlSpec skin. A control is configured, not hand-assembled. 60fps, same code on Android & iOS."),
    Glass("Look-behind-glass lens", "🔍", "OGGlass lays a round, movable lens over any content — a magnifier (zoom > 1), a minifier (zoom < 1) or coloured glass (a tint). Drag it by its grip; the body is pointer-transparent so the content keeps responding. The content beneath is recorded once into a GraphicsLayer and redrawn scaled about the lens centre, clipped to a circle, plus tint + rim + sheen — GPU only, no shader. An accessibility win as much as an effect; author-as-JSON, 60fps, same code on Android & iOS."),
    Flame("Flickering fire", "🔥", "OGFlameView draws a body of living, upward-licking flame: a handful of tapered tongues whose centrelines wave with fractal noise and whose heights flicker over time, each a GPU-filled path with a hot→cool gradient. No fluid sim, no per-pixel shader — deterministic from a seed, frame-identical on Android & iOS and bakes into an export byte-for-byte. Drive a live wind to bend the whole fire. Author-as-JSON (candle / campfire / torch / inferno)."),
    Puppet("Cut-out puppet (raster rig)", "🪁", "OGPuppet is the data-defined cut-out puppet — a hand-drawn figure placed as its own pixels and brought alive by the lib's pure, deterministic pose math: a hovering float, a breathing pulse, a lean + tilt, keyed off one clock and a −1..1 look target. The raster sibling of the particle / parametric data packs: author a .puppet pack of joints + motion, the library animates it — no per-app rigging code. Round-trips through the JSON codec; 60fps, same code on Android & iOS."),
    ClothImageStage("Wind on the stage (cloth · image · gusts)", "🌬️", "One OGStage timeline exercising all three new 1.48.0 layer kinds: a Verlet cloth banner (OGStageContent.Cloth) fluttering on a prevailing wind, a hand-drawn hero cutout (OGStageContent.Image) riding the stage as real pixels, and a particle breeze carrying the pack's new wind / windAmplitude / windFrequency fields so leaves drift downwind in organic gusts. All blown by the same breeze, authored as data, round-tripped through the codec. Deterministic → preview and export match; same code on Android & iOS."),
    Animate("Animate a Static Image", "✨", "Take a fully-static SVG and wrap it in KMPMedia's animation primitives — toggle Scale/Rotate/Fade/Slide and watch it come alive."),
    RuntimeSvg("Runtime-editable SVG", "🎨", "Load an SVG once, then change any node by id at runtime — recolour, rotate, move — bound to Compose state. A live gauge whose needle and zones follow a slider. 'SVG as a live template', the same code on Android & iOS."),
    AiVector("AI vector (describe → shape)", "🤖", "KMPMedia's vector primitives are just data, so a language model can produce them. Decode a model's JSON reply into a live polygon lasso clipping a photo, and into an SVG patch driving a gauge — via OGAiVector, with no AI SDK or network in the library."),
    Vectorized("Photo → vector, cranked", "🦁", "A photo traced into a dense triangle mesh — ~20,000 triangles, each one an OGSceneSpec polygon+fill decoded by the library — so a real lion renders from pure vector data. Then re-theme it day↔night with a shimmer, from a cached layer at 60fps: realistic AND still live, the far end of the realism↔editability dial."),
    SceneFx("Richer stills (texture · glow · scatter)", "🌄", "The 1.39.0 scene-paint fields: a region can carry a procedural noise TEXTURE (fur/grass), an additive GLOW halo (sun bloom, backlit rim), and a SCATTER that stamps one shape into a whole field (trees/tufts). Shown flat vs. with each field, driven through the real OGAiVector encode→decode→expand path. Pure data, 60fps, same code on Android & iOS."),
    MeshRig("Clickable joints (bend the mesh)", "🦁", "The SAME dense lion mesh as 'Photo → vector', but RIGGED. Twelve joints are bound to the triangle vertices by linear-blend skinning (OGRig); drag any pin and the mesh warps under your finger, then springs home. This is the one case the rasterise-once/cache trick can't cover — while you drag, every vertex is re-skinned and the mesh re-drawn each frame. Pure maths, same code on Android & iOS."),
    Video("Video Playback", "🎬", "Stream a clip through one cross-platform OGAVPlayer (ExoPlayer on Android, AVPlayer on iOS) and re-frame it live inside any shape — circle, triangle, diamond — with transport controls, loop, and load any URL."),
    MorphClip("Morph the clip itself", "🫧", "The clip MASK animates circle → diamond → triangle → lasso while a video (and a GIF) keep playing underneath. One OGMorphShape drives every surface's clipShape — clipping moving media to a morphing outline, 60fps, same code on Android & iOS."),
    BoilingLines("Boiling lines (moving style)", "🖊️", "A prototype 'moving style': OGBoil jitters every vertex of an outline a few times a second — the hand-drawn 'living line'. Drawn as a stroke it's a wiggly line; dropped into a clipShape it's a living media edge. Pure, deterministic, zero-dependency. The first member of a procedural com.solidkey.painpoints.style family."),
    Particles("Particles & parametric shapes", "🎆", "Two data-defined generators from com.solidkey.painpoints.particle / .shape. A particle effect (rate, gravity, colour-over-life, shape) that OGParticleView compiles once and simulates on the frame clock — six presets + a live JSON editor. And parametric outlines (star / gear / flower / squircle / blob) wrapped in an OGPolygonShape clipShape, generated once and spun on the GPU. Both author-as-JSON, both 60fps, same code on Android & iOS."),
    Slash("Breathing-slash ribbons", "🗡️", "The signature anime-action effect: a glowing tapered arc that draws on along a path and stays alive with a flowing edge + breathing width. One OGSlashView primitive wears three elements chosen by data — a water sweep, a flame lick, a thunder bolt — composable with droplet/ember/spark particles. The centreline is sampled once; each frame only rebuilds the tapered outline + an additive-glow gradient fill. Author-as-JSON, 60fps, frame-identical on Android & iOS."),
    ActionFx("Action FX pack", "⚡", "The stylised motion-FX family from com.solidkey.painpoints.fx: forked lightning (OGLightningView), manga speed/impact lines (OGSpeedLinesView), elemental particle presets (petals/embers/droplets/leaves), the ogGlow + ogAfterImage modifiers, and a composed 'action beat' assembling several on one stage. Each effect is plain serializable data; all pure maths, 60fps, identical on Android & iOS."),
    Looks("Live looks", "🌈", "A colour grade is just data: an OGLookSpec (brightness / contrast / saturation / temperature / tint / hue) compiles once into one ColorFilter you drop on ANY graphic with Modifier.ogLook — plus blend modes for compositor layers. One GPU colour op, zero per-frame allocation, 60fps, pixel-identical on Android & iOS. Looks serialize to a .look JSON a model can author, like OGAiVector / OGStyles."),
    AudioReactive("Audio-reactive vectors", "🎵", "Make any vector react to sound. A pure-Kotlin FFT (OGFft) folds PCM into bass/mid/treble/level + a beat flag (OGAudioAnalyzer), smoothed with attack/release; bind a band to any composable with Modifier.ogAudioReactive, or straight into a spec. The capture is a pluggable OGAudioSource — here a deterministic synthetic beat; plug your mic/music on device. Zero-dep, 60fps, same code on Android & iOS."),
    Motion("Motion design (draw-on, paths, stagger)", "✍️", "Three motion-graphics primitives from com.solidkey.painpoints.motion: draw-on (a stroke that draws itself on by arc length), motion paths (glide + turn any element along a curve, constant-speed by distance), and stagger (one timeline → per-element offset timing so a group cascades/waves/pops). The path + stagger are plain serializable data (OGMotions), the core is pure maths, 60fps, same code on Android & iOS."),
    Dynamics("Motion dynamics (robotic → alive)", "🌀", "The pure-maths 'feel' layer of com.solidkey.painpoints.motion. The same moving target is chased by a robotic twin (constant speed, stops dead) and an alive one fed through OGSpring (lag + overshoot + settle), OGSquash (stretch into the move), an OGFollowChain comet tail (follow-through) and OGSway (idle wander). Pick a feel from OGDynamics.presets — each is plain data a model can author via dynamicsPrompt. Spring is closed-form/time-addressable so the compositor can scrub & export it; 60fps, same code on Android & iOS."),
    Scene("Scene in motion (your head, walking)", "🎬", "A composed showreel at sunset: a walking FK rig wearing your lasso-clipped photo as its head, with real cloth (a 1-D OGClothStrip scarf + a 2-D OGCloth flag on a pole) rippling in the wind, idle head drift (OGSway), spring follow-through + footfall squash (OGSpring/OGSquash), manga speed lines (OGSpeedLines), an after-image trail (ogAfterImage) + rim glow (ogGlow), drifting petals (OGParticles) — all under a sun that sets while an ogLook grade shifts the frame day→dusk→night. Every effect toggles. Pure demo code over shipped primitives; same code on Android & iOS."),
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
                        DemoScreen.Stage -> StageScreen()
                        DemoScreen.Describe -> DescribeScreen()
                        DemoScreen.SuperX -> SuperXScreen()
                        DemoScreen.SuperXRig -> SuperXRigScreen()
                        DemoScreen.LivingRig -> LivingRigScreen()
                        DemoScreen.Skin -> SkinScreen()
                        DemoScreen.Light -> LightScreen()
                        DemoScreen.Fog -> FogScreen()
                        DemoScreen.Control -> ControlScreen()
                        DemoScreen.Glass -> GlassScreen()
                        DemoScreen.Flame -> FlameScreen()
                        DemoScreen.Puppet -> PuppetScreen()
                        DemoScreen.ClothImageStage -> ClothImageStageScreen()
                        DemoScreen.Animate -> AnimateScreen()
                        DemoScreen.RuntimeSvg -> RuntimeSvgScreen()
                        DemoScreen.AiVector -> AiVectorScreen()
                        DemoScreen.Vectorized -> VectorizedScreen()
                        DemoScreen.SceneFx -> SceneFxScreen()
                        DemoScreen.MeshRig -> MeshRigScreen()
                        DemoScreen.Video -> VideoScreen()
                        DemoScreen.MorphClip -> MorphClipScreen()
                        DemoScreen.BoilingLines -> BoilingLinesScreen()
                        DemoScreen.Particles -> ParticlesScreen()
                        DemoScreen.Slash -> SlashScreen()
                        DemoScreen.ActionFx -> ActionFxScreen()
                        DemoScreen.Looks -> LooksScreen()
                        DemoScreen.AudioReactive -> AudioReactiveScreen()
                        DemoScreen.Motion -> MotionScreen()
                        DemoScreen.Dynamics -> DynamicsScreen()
                        DemoScreen.Scene -> SceneScreen()
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
