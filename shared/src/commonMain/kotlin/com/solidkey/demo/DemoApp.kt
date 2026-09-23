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
    Video("Video Playback", "🎬", "Stream a clip through one cross-platform OGAVPlayer (ExoPlayer on Android, AVPlayer on iOS) and re-frame it live inside any shape — circle, triangle, diamond — with transport controls, loop, and load any URL."),
    CropShape("Crop a Photo into a Shape", "✂️", "Pick a photo and crop it into a circle, triangle or diamond — the image twin of 'Video in any shape'. It's one Compose GPU clip drawn once, so there's zero performance cost."),
    Gif("Animated GIF", "🎞️", "Point one OGImageView at a .gif and it plays — looping frames on Android (AnimatedImageDrawable) and iOS (Skia Codec), the same code. The shape clip that crops a photo works on the moving image too."),
    Joint("Jointed Shapes (a rig)", "🦾", "Connect shape-clipped photos at a single pixel with a movable angular limit. Drag a two-link arm — each joint stops at its limit — then hit 'Let it swing' to watch a clamped pendulum. The building block for articulated rigs and motion dynamics."),
    BodyRig("Add Your Head to a Body", "🧍", "The arms, legs and torso are already rigged. Pick a photo, clip it to a shape and it becomes the head — pinned at the neck joint you set — then tap Wave / Walk / Jumping jacks / Dance and watch the whole body move, your head riding along."),
    Playground("Feature Playground", "🎛️", "Load images, SVGs, video, audio & layers from any URL/resource — tune every config live."),
    Game("UFO Dodge (mini-game)", "🛸", "Every sprite is a static SVG animated by KMPMedia: dodge tumbling meteorites & hostile ships, grab stars to repair — and watch your UFO shake harder as damage rises. Tap ⚙️ Customize to crop your own photos into shapes and drop them into the field as game objects."),
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
                        DemoScreen.Video -> VideoScreen()
                        DemoScreen.CropShape -> CropShapeScreen()
                        DemoScreen.Gif -> GifScreen()
                        DemoScreen.Joint -> JointScreen()
                        DemoScreen.BodyRig -> BodyRigScreen()
                        DemoScreen.Playground -> TestMainUI()
                        DemoScreen.Game -> UfoGameScreen()
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
