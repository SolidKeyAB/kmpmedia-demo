package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFileType
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.stage.OGOfflineStageAuthor
import com.solidkey.painpoints.stage.OGStageAuthoring
import com.solidkey.painpoints.stage.OGStageContent
import com.solidkey.painpoints.stage.OGStageView
import com.solidkey.painpoints.stage.OGStages
import com.solidkey.painpoints.stage.exportGif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Example asks that each hit a different branch of the offline director's grammar. */
private val EXAMPLE_ASKS = listOf(
    "a stormy night over the sea",
    "cherry blossoms over pink hills at sunset",
    "a neon city speed rush",
    "a snowy winter morning",
    "fireworks over the harbour",
    "autumn leaves in a quiet forest",
)

/**
 * 🪄 **Describe → movie, live.** Type a sentence and the library turns it into a real, playable
 * [com.solidkey.painpoints.stage.OGStage] — *on the spot, with no key and no network*. This is the AI-
 * authorship loop closed end-to-end: the honest floor runs through [OGOfflineStageAuthor], a
 * deterministic director that reads the words for mood + subject and assembles a backdrop scene,
 * atmosphere particles, an optional action-FX beat and a styled title. The SAME [OGStage] format and
 * player are what a real language model writes into — wire your own key via
 * [OGStageAuthoring.modelAuthor] and the words become a richer, model-written movie through this exact
 * screen. The movie is round-tripped through [OGStages.encode] → [OGStages.decode] (the real wire path
 * a model reply takes) before it plays, and it exports to a shareable GIF. Same code on Android & iOS.
 */
@Composable
fun DescribeScreen() {
    val author = remember { OGOfflineStageAuthor() }

    var draft by remember { mutableStateOf(EXAMPLE_ASKS.first()) }
    var instruction by remember { mutableStateOf(EXAMPLE_ASKS.first()) }
    var showPrompt by remember { mutableStateOf(false) }

    // Author → encode → decode, so what plays is literally what came off the wire (a model's path).
    val stage = remember(instruction) { OGStages.decode(OGStages.encode(author.direct(instruction))) }
    val ingredients = remember(stage) { describeIngredients(stage) }
    val jsonChars = remember(stage) { OGStages.encode(stage).length }

    // A down-sized / down-fps copy for a quick on-device GIF export.
    val exportStage = remember(stage) { stage.copy(width = 640, height = 360, fps = 12) }
    val gifSaver = rememberGifSaver()
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var exportInfo by remember { mutableStateOf<String?>(null) }
    var exportFile by remember { mutableStateOf<String?>(null) }
    var exportNonce by remember { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "🪄 Describe → movie",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Type a sentence; the library builds a real, playable movie from it — on the spot, with no key " +
                "and no network. This is the AI-authorship loop closed end-to-end. The offline director reads " +
                "the words for mood + subject and composes a scene, particles, an action-FX beat and a title. " +
                "A real language model writes into the SAME format through the same screen (bring your own key).",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        // ── The ask ──────────────────────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text("Describe a movie") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (ask in EXAMPLE_ASKS) {
                Chip(ask.substringBefore(" over").substringBefore(" at").take(16), ask == draft) {
                    draft = ask; instruction = ask
                }
            }
        }
        Button(onClick = { instruction = draft }, modifier = Modifier.fillMaxWidth()) {
            Text("🎬 Make the movie")
        }

        // ── The player ───────────────────────────────────────────────────────────────────────────
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            OGStageView(stage = stage, modifier = Modifier.fillMaxSize(), isPlaying = true, loop = true)
        }
        Text(
            "Built from “$instruction” → ${stage.layers.size} layers · $jsonChars chars · " +
                "decoded → played (the real OGStages wire path) · live = ${if (stage.isLive) "✓" else "✗"}",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
        )

        // ── What the director chose ────────────────────────────────────────────────────────────
        Text("What the words became", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        for (line in ingredients) Bullet2(line)

        // ── The prompt a real model would get ────────────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        Text(
            if (showPrompt) "▼ Hide the model prompt" else "▶ Show the model prompt (bring your own key)",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { showPrompt = !showPrompt },
        )
        if (showPrompt) {
            Text(
                "To get a model-written movie instead of the offline one, implement OGStageModel.complete " +
                    "with your LLM call and run OGStageAuthoring.modelAuthor(model).author(\"$instruction\"). " +
                    "That's the EXACT prompt below; the reply is decoded by the same OGStages.decode and played here.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Box(
                Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)).padding(12.dp),
            ) {
                Text(
                    OGStageAuthoring.promptFor(instruction),
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )
            }
        }

        // ── Export: describe → play → export, the loop no Lottie/Rive runtime does ───────────────
        Spacer(Modifier.height(8.dp))
        Text("Export this movie (describe → play → export)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Button(
            onClick = {
                if (exporting) return@Button
                scope.launch {
                    exporting = true
                    exportInfo = "Rendering ${exportStage.frameCount} frames…"
                    yield()
                    val bytes = withContext(Dispatchers.Default) { exportStage.exportGif() }
                    val name = "describe_export_$exportNonce"
                    val ok = gifSaver.save(name, bytes)
                    exportInfo = if (ok) {
                        exportFile = name
                        exportNonce++
                        "Exported ${exportStage.frameCount} frames · ${bytes.size / 1024} KB GIF · " +
                            "${exportStage.width}×${exportStage.height} @ ${exportStage.fps}fps"
                    } else {
                        "Save failed"
                    }
                    exporting = false
                }
            },
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Exporting…" else "Export to animated GIF")
        }
        exportInfo?.let { Text(it, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        exportFile?.let { name ->
            Text("Played back through the platform GIF decoder:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.fillMaxWidth().background(Color(0xFF05060F)).padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                OGImageView(
                    source = OGImageFileType(name, OGImageFormat.GIF),
                    displayShape = OGShapeType.SQUARE,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(260.dp),
                    onError = { exportInfo = "Playback error: $it" },
                    onEventTriggered = { _, _ -> },
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "The director is deterministic (same words → same movie) and pure data assembly done once, so " +
                "the result plays at 60fps like any hand-authored stage. The model path produces richer movies " +
                "through the identical format + player — no other KMP animation library is model-authorable.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

/** A short, human line per layer describing what the director put on the timeline. */
private fun describeIngredients(stage: com.solidkey.painpoints.stage.OGStage): List<String> =
    stage.layers.map { layer ->
        when (val c = layer.content) {
            is OGStageContent.Scene -> "🏞️ scene backdrop — ${c.scene.shapes.size} regions" +
                (c.scene.motion?.let { " + a day→time swing" } ?: "")
            is OGStageContent.Particles -> "✨ particles — ${c.particles.name ?: c.particles.shape} (${c.particles.shape})"
            is OGStageContent.Lightning -> "⚡ lightning — a forked bolt on the beat"
            is OGStageContent.SpeedLines -> "💨 speed lines — ${c.speedLines.count} converging streaks"
            is OGStageContent.Slash -> "🗡️ slash ribbon"
            is OGStageContent.Text -> "🔤 title — “${c.text}” (${c.style?.ops?.firstOrNull()?.op ?: "clean"})"
            is OGStageContent.Image -> "🖼️ cutout — ${c.frames.size} frame${if (c.frames.size == 1) "" else "s"}"
            is OGStageContent.Cloth -> "🌬️ cloth strip — a wind-blown ${c.cloth.name ?: "banner"}"
            is OGStageContent.Solid -> "🎨 solid fill"
        }
    }

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun Bullet2(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
    }
}
