package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.compositor.OGEasing
import com.solidkey.painpoints.stage.OGStage
import com.solidkey.painpoints.stage.OGStageView
import com.solidkey.painpoints.stage.OGStages
import com.solidkey.painpoints.stage.OGTapAction
import com.solidkey.painpoints.style.OGStyleOp
import com.solidkey.painpoints.style.OGStyleSpec

/**
 * 🦸 **SUPER X — a hand-drawn cutout, alive & tappable (OGStage raster layer).** The first demo of the
 * engine's reserved image-bitmap slot: [OGStageContent.Image][com.solidkey.painpoints.stage.OGStageContent.Image]
 * places a REAL drawn PNG on the stage (not a redrawn vector) and animates it by **frame-swap**. Here one
 * hand-drawn hero head rides the stage as itself — it blinks (three bundled frames: open → half → shut),
 * drifts (a bob + a slow sway, plain keyframe tracks), and springs with a nod when you **tap** it
 * (`onTap` → a spring BOUNCE). A paper-doll / Spine-style puppet, authored as pure OGStage data and
 * round-tripped through the JSON codec before it plays. The lib CONSUMES clean part frames; a person (or
 * a segmenter) SEPARATES the art. Same code on Android & iOS.
 */
@Composable
fun SuperXScreen() {
    // Authored once, then encoded → decoded so what plays is literally what came off the wire (the real
    // OGStages path). The cutout frames reference app-bundled drawables (res/drawable → the loader).
    val stage = remember { OGStages.decode(OGStages.encode(buildSuperX())) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "🦸 SUPER X — a drawing that moves",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "The whole hand-drawn head, placed on the stage as ITS OWN pixels (a bundled PNG, not a redrawn " +
                "vector): it drifts (a gentle bob + sway) and springs with a nod when you poke it. This is " +
                "OGStage's new raster-cutout layer — the paper-doll primitive that turns a drawing into a " +
                "movable, tappable puppet. (Next: layer the separately-drawn eyes + mouth for a real blink.)",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        Text("👆 Tap the hero", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

        // The stage. A square hero frame; the head blinks & drifts on its own, and a tap nods it.
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            OGStageView(
                stage = stage,
                modifier = Modifier.fillMaxSize(),
                isPlaying = true,
                loop = true,
            )
        }

        Text(
            "From JSON: ${stage.layers.size} layers · decoded → played (the real OGStages wire path) · " +
                "live = ${if (stage.isLive) "✓" else "✗"}",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(2.dp))
        Text("What's on the timeline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Bullet("① image cutout — the whole head is one OGStageContent.Image: the real drawn pixels, centred and fit in the frame. One layer, so it stays one tappable unit (and a frame list would let it blink / mouth-shape by swap — wired, just not used here yet).")
        Bullet("② motion — the layer carries a gentle bob (a y keyframe track) and a slow sway (a rotation track). Real drawn pixels, moved by the compositor's keyframes — no re-vectorising.")
        Bullet("③ tap → nod — onTap fires a spring BOUNCE (the dynamics pack's OGSpring), so a poke pops the whole head and settles with physics. Pure data: the trigger round-trips through the codec.")

        Spacer(Modifier.height(4.dp))
        Text(
            "The unlock: a flat photo of a drawing can't be auto-clipped — the art must be SEPARATED (one " +
                "part per clean frame, transparent or on clean white). Once it is, the lib animates the real " +
                "pixels. Same requirement as Live2D / Spine / Rive; the differentiator is we move YOUR drawing, " +
                "not a redraw of it. 60fps, same code on Android & iOS.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
    }
}

// --- The puppet, authored as OGStage data (then round-tripped through the JSON codec) -----------

/**
 * The puppet stage: a dark hero backdrop, the full head cutout (drift + tap-to-nod), and a styled
 * "SUPER X" title. One 7.2s loop so the bob/sway return seamlessly. Authored as data —
 * [OGStages.imageStill] for the cutout, keyframe tracks for the drift. (The head is drawn portrait,
 * so [heightFrac] > [widthFrac]; it's centred and fit, aspect preserved.)
 */
private fun buildSuperX(): OGStage = OGStage.of(
    OGStages.solid("bg", "#0E1026"),
    OGStages.imageStill(
        id = "head",
        resource = "superx_head_full",
        widthFrac = 0.56f,
        heightFrac = 0.74f,
        // A gentle float up-and-back, twice over the loop (returns to 0 at the end = seamless).
        y = OGStages.eased(OGEasing.EASE_IN_OUT, 0L to 0f, 1800L to -0.02f, 3600L to 0f, 5400L to -0.02f, 7200L to 0f),
        // A slow head sway, one full swing over the loop.
        rotationDeg = OGStages.eased(OGEasing.EASE_IN_OUT, 0L to 0f, 1800L to 2.2f, 3600L to 0f, 5400L to -2.2f, 7200L to 0f),
        // Poke it: the whole head springs with an excited nod, then settles (OGSpring physics).
        onTap = OGStages.tap(OGTapAction.BOUNCE, strength = 1.0f, durationMs = 620L),
    ),
    OGStages.text(
        id = "title",
        text = "SUPER X",
        size = 0.072f,
        color = "#8FA6FF",
        bold = true,
        y = com.solidkey.painpoints.compositor.OGKeyframedFloat.const(0.44f),
        style = OGStyleSpec(ops = listOf(OGStyleOp(op = "roughen", amplitude = 0.012f, detail = 3))),
    ),
    width = 1080,
    height = 1080,
    durationMs = 7200L,
)
