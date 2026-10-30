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
import com.solidkey.painpoints.compositor.OGKeyframedFloat
import com.solidkey.painpoints.motion.OGCloths
import com.solidkey.painpoints.particle.OGParticles
import com.solidkey.painpoints.stage.OGStage
import com.solidkey.painpoints.stage.OGStageView
import com.solidkey.painpoints.stage.OGStages
import com.solidkey.painpoints.style.OGStyleOp
import com.solidkey.painpoints.style.OGStyleSpec

/**
 * 🌬️ **Wind on the stage — OGStage Cloth + Image layers + a gusty particle breeze.** Three 1.48.0 stage
 * additions on ONE timeline: a wind-blown **cloth strip** (OGStageContent.Cloth — a Verlet banner that
 * hangs and flutters), a raster **image cutout** (OGStageContent.Image — a hand-drawn PNG as its own
 * pixels), and a particle layer carrying the pack's new **wind / gust** fields so leaves drift downwind in
 * organic gusts. Every piece feels the same breeze. Authored as pure OGStage data, round-tripped through
 * the JSON codec, deterministic → preview and export match. Same code on Android & iOS.
 */
@Composable
fun ClothImageStageScreen() {
    val stage = remember { OGStages.decode(OGStages.encode(buildWindyStage())) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "🌬️ Wind on the stage",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "One OGStage timeline exercising all three new 1.48.0 layer kinds: a Verlet cloth banner " +
                "fluttering on a prevailing wind, a hand-drawn hero cutout riding the stage as real pixels, and " +
                "a gusty particle breeze (the pack's new wind / windAmplitude / windFrequency fields). All blown " +
                "by the same breeze, all authored as data.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
        )

        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            OGStageView(stage = stage, modifier = Modifier.fillMaxSize(), isPlaying = true, loop = true)
        }

        Text(
            "From JSON: ${stage.layers.size} layers · decoded → played (the real OGStages wire path)",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(2.dp))
        Text("What's on the timeline", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        ClothImageBullet("① cloth banner — OGStageContent.Cloth: a 1-D Verlet strip pinned at its head, hanging under gravity and streaming out on a prevailing wind with turbulent gusts. The motion family's OGClothStrip, placed on the stage.")
        ClothImageBullet("② image cutout — OGStageContent.Image: the hand-drawn hero placed as its OWN pixels (a bundled PNG, not a redrawn vector), fit in the frame, drifting on a gentle keyframe bob.")
        ClothImageBullet("③ wind particles — a leaf emitter whose spec carries the new wind + windAmplitude + windFrequency fields, so the leaves accelerate downwind and swell/fade through organic fractal-noise gusts (mirrors the cloth's wind model, so they blow alike).")

        Spacer(Modifier.height(4.dp))
        Text(
            "The cloth is re-simulated deterministically from a seed and the particles ride a deterministic " +
                "gust field, so the preview and a baked GIF/MP4 export match frame-for-frame. Pure OGStage " +
                "data over shipped primitives; same code on Android & iOS.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ClothImageBullet(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f))
    }
}

/**
 * The windy stage, authored as OGStage data (then round-tripped through the JSON codec). A dusk backdrop,
 * a wind-blown cloth banner strung across the top, a drifting hero cutout, a styled title, and a gusty
 * leaf breeze — all on a 6s loop. The cloth + the leaves share a rightward breeze.
 */
private fun buildWindyStage(): OGStage = OGStage.of(
    OGStages.solid("bg", "#20314F"),

    // A banner strung between two points across the top, flapping on a strong rightward wind + gusts.
    OGStages.cloth(
        id = "banner",
        cloth = OGCloths.preset("banner")!!.copy(wind = 0.9f, windAmplitude = 0.7f, windFrequency = 0.9f),
        anchorX = 0.16f, anchorY = 0.16f,
        tailX = 0.84f, tailY = 0.16f,
        color = "#FF6D3A",
        width = 0.08f,
        taper = false,
    ),

    // The hand-drawn hero cutout as real pixels, with a gentle drift (y bob over the loop).
    OGStages.imageStill(
        id = "hero",
        resource = "superx_fig",
        widthFrac = 0.64f,
        heightFrac = 0.5f,
        x = OGKeyframedFloat.const(0f),
        y = OGStages.eased(OGEasing.EASE_IN_OUT, 0L to 0.08f, 3000L to 0.12f, 6000L to 0.08f),
    ),

    // A gusty leaf breeze: the particle pack's new wind fields push the leaves downwind in organic gusts.
    OGStages.particles(
        id = "leaves",
        spec = OGParticles.preset("snow")!!.copy(
            startColor = "#F2B135",
            endColor = "#C25A1E",
            wind = 0.5f,
            windAmplitude = 0.4f,
            windFrequency = 0.8f,
            windSeed = 5,
        ),
    ),

    OGStages.text(
        id = "title",
        text = "WIND",
        size = 0.09f,
        color = "#FFE0B2",
        bold = true,
        y = OGKeyframedFloat.const(0.82f),
        style = OGStyleSpec(ops = listOf(OGStyleOp(op = "roughen", amplitude = 0.012f, detail = 3))),
    ),

    width = 1080,
    height = 1080,
    durationMs = 6000L,
)
