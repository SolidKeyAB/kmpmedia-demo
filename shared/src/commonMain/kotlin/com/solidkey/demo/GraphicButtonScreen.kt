package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.gesture.OGHitArea
import com.solidkey.painpoints.gesture.OGPressEffect
import com.solidkey.painpoints.gesture.ogButton
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.image.loading.OGSvgResourceFileType
import com.solidkey.painpoints.image.svg.OGSVGView
import com.solidkey.painpoints.shape.OGPolygonShape

/** The head lasso (normalized 0..1), reused from the "Add Your Head" screen — cuts just the head of the bundled sample. */
private val HEAD_LASSO: OGPolygonShape = OGPolygonShape.of(
    0.48f to 0.02f, 0.64f to 0.04f, 0.78f to 0.10f, 0.86f to 0.22f,
    0.88f to 0.38f, 0.84f to 0.54f, 0.73f to 0.69f, 0.59f to 0.79f,
    0.48f to 0.83f, 0.37f to 0.79f, 0.25f to 0.68f, 0.16f to 0.53f,
    0.13f to 0.38f, 0.15f to 0.22f, 0.24f to 0.09f, 0.35f to 0.03f,
)

/** A diamond outline (normalized 0..1) — used both as the clip and as the matching hit area. */
private val DIAMOND: OGPolygonShape = OGPolygonShape.of(
    0.5f to 0f, 1f to 0.5f, 0.5f to 1f, 0f to 0.5f,
)

/**
 * The 5-pointed star outline (normalized 0..1), taken straight from `star.svg`'s `<polygon>` points
 * (a 0..100 viewBox ÷ 100). The SVG is drawn to fill the whole 104dp box, so these normalized points
 * line up exactly with what's on screen — giving the star button a shape-aware hit area that matches
 * the glyph (tap a point → fires; tap the gaps between the arms → falls through).
 */
private val STAR: OGPolygonShape = OGPolygonShape.of(
    0.50f to 0.08f, 0.5999f to 0.3625f, 0.899f to 0.37f, 0.6617f to 0.5525f,
    0.747f to 0.8398f, 0.50f to 0.67f, 0.253f to 0.8398f, 0.3383f to 0.5525f,
    0.1006f to 0.3702f, 0.4001f to 0.3625f,
)

/**
 * 🔘 **Any graphic → a button** — the tap twin of the Interactive (drag) screen. `Modifier.ogButton`
 * turns any graphic (an SVG, a lasso-cut photo, a plain shape) into a real pressable button: a
 * shape-aware tap (only the actual silhouette, via `OGHitArea`) + a pressed-state visual + `onClick`,
 * with no UI component and zero library dependency. Same code on Android & iOS.
 */
@Composable
fun GraphicButtonScreen() {
    var taps by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf("—") }
    val register: (String) -> Unit = { label ->
        taps += 1
        last = label
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Any graphic → a button", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "KMPMedia ships no button component. One Modifier.ogButton makes any graphic pressable: a " +
                "shape-aware tap (only inside the real silhouette, via OGHitArea), a pressed-state visual, " +
                "and onClick. It's the tap twin of ogInteractive's drag — same OGHitArea, same zero-dep, 60fps.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        // ── Live tap readout (so a press is visibly registering) ───────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Last press: $last", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Total taps: $taps", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ── Three graphics, each turned into a button with a different press effect ─────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            // 1) An SVG graphic → a button (Scale press), SHAPE-AWARE on the star outline.
            ButtonCell("SVG · Scale") {
                // OGSVGView's width/height are its drawing extent in PIXELS (it scales the viewBox to
                // fill them, then the Canvas is clamped to the parent box). Passing the box's px size
                // makes the star fill the whole 104dp box on ANY density, so the drawn glyph lines up
                // with the STAR hit area 1:1 (at dp-as-px it rendered tiny in the top-left and taps missed).
                val boxPx = with(LocalDensity.current) { 104.dp.toPx() }
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .ogButton(
                            hitArea = OGHitArea.polygon(STAR),
                            pressEffect = OGPressEffect.Scale(),
                        ) { register("Star (SVG)") },
                    contentAlignment = Alignment.Center,
                ) {
                    OGSVGView(source = OGSvgResourceFileType("star"), width = boxPx, height = boxPx, onError = {})
                }
            }

            // 2) A lasso-cut photo → a button (Brutalist hard-shadow press, SHAPE-AWARE: only the head).
            //    The image fills the card so the tappable head silhouette is the whole visible target
            //    (only the cream corners outside the lasso fall through).
            ButtonCell("Lasso photo · Brutalist") {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFFFFF3C4), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    OGImageView(
                        source = OGImageResourceFileType("avatar_ozge", OGImageFormat.JPEG),
                        clipShape = HEAD_LASSO,
                        modifier = Modifier
                            .size(120.dp)
                            .ogButton(
                                hitArea = OGHitArea.polygon(HEAD_LASSO),
                                pressEffect = OGPressEffect.Brutalist(offset = 6.dp, shadowColor = Color(0xFF7B2FF7)),
                                onLongClick = { register("Head — LONG press") },
                            ) { register("Head (lasso, shape-aware)") },
                        onError = {},
                        onEventTriggered = { _, _ -> },
                    )
                }
            }

            // 3) A plain shape → a button (Dim press, shape-aware on the diamond).
            ButtonCell("Diamond · Dim") {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .ogButton(hitArea = OGHitArea.DIAMOND, pressEffect = OGPressEffect.Dim()) { register("Diamond") },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(104.dp)
                            .clip(DIAMOND)
                            .background(Brush.linearGradient(listOf(Color(0xFF00C2A8), Color(0xFF7B2FF7)))),
                    )
                }
            }
        }

        Text(
            "Try it: every graphic here is shape-aware — tap ON the shape and it fires, tap the gaps " +
                "around it and nothing happens (the touch falls through). The star responds only on its " +
                "arms, the photo only inside the head outline (the same lasso that clips it), the diamond " +
                "only on the diamond. Long-press the head for a second action.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Spacer(Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                "Under the hood: Modifier.ogButton gates the tap with OGHitArea.containsNormalized on the " +
                    "down event (outside the silhouette it falls through), drives the press visual through one " +
                    "graphicsLayer (Brutalist additionally draws its own hard-offset shadow from the hit-area " +
                    "outline, collapsing on press), and exposes a Role.Button + activatable onClick for screen " +
                    "readers. No double-tap (it would add latency to every press). The same code on Android & iOS.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun ButtonCell(caption: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(caption, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
    }
}
