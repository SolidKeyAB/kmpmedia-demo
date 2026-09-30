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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.shape.DiamondShape
import com.solidkey.painpoints.shape.OGClipOp
import com.solidkey.painpoints.shape.OGClipRegion
import com.solidkey.painpoints.shape.OGMultiRegionShape
import com.solidkey.painpoints.shape.OGShapeType
import kotlin.math.roundToInt

// The three mask flavors this screen dogfoods, all landing in the same OGImageView.
private enum class MaskMode(val label: String, val glyph: String) {
    SOFT("Soft edge", "🪶"),
    GRADIENT("Gradient", "🌈"),
    MULTI("Multi-region", "🔵🔵"),
}

// Gradient-mask presets — each a Brush running to Color.Transparent (only the alpha matters, DstIn).
private data class GradientChoice(val label: String, val brush: Brush)

private val GRADIENT_CHOICES = listOf(
    GradientChoice(
        "Fade bottom",
        Brush.verticalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
    ),
    GradientChoice(
        "Fade right",
        Brush.horizontalGradient(0f to Color.Black, 0.6f to Color.Black, 1f to Color.Transparent),
    ),
    GradientChoice(
        "Spotlight",
        Brush.radialGradient(0f to Color.Black, 0.5f to Color.Black, 1f to Color.Transparent),
    ),
)

// Multi-region presets — each an OGMultiRegionShape built from placed sub-shapes + a path op.
private data class MultiChoice(val label: String, val shape: OGMultiRegionShape)

private val MULTI_CHOICES = listOf(
    MultiChoice(
        "Two portholes",
        OGMultiRegionShape.of(
            OGClipRegion(CircleShape, left = 0.02f, top = 0.24f, right = 0.48f, bottom = 0.76f),
            OGClipRegion(CircleShape, left = 0.52f, top = 0.24f, right = 0.98f, bottom = 0.76f),
            op = OGClipOp.UNION,
        ),
    ),
    MultiChoice(
        "Diamond − bite",
        OGMultiRegionShape.of(
            OGClipRegion(DiamondShape()),
            OGClipRegion(CircleShape, left = 0.55f, top = 0.55f, right = 1.05f, bottom = 1.05f),
            op = OGClipOp.DIFFERENCE,
        ),
    ),
    MultiChoice(
        "Triple",
        OGMultiRegionShape.of(
            OGClipRegion(CircleShape, left = 0f, top = 0.30f, right = 0.40f, bottom = 0.70f),
            OGClipRegion(CircleShape, left = 0.30f, top = 0.05f, right = 0.70f, bottom = 0.45f),
            OGClipRegion(CircleShape, left = 0.60f, top = 0.30f, right = 1f, bottom = 0.70f),
            op = OGClipOp.UNION,
        ),
    ),
)

/**
 * **Soft & gradient masks, multi-region clips** (lib 1.12.0). Three ways to shape a clip mask beyond
 * a single hard-edged shape, all landing in the SAME [OGImageView] on a real photo:
 *
 *  • **Soft edge** — `OGImageView(softEdge = …)` feathers the shape's boundary so the photo fades out
 *    into a vignette instead of a crisp cut.
 *  • **Gradient** — `OGImageView(maskBrush = …)` multiplies the photo's alpha by a gradient (an edge
 *    fade or a spotlight). Both are one offscreen BlendMode.DstIn pass — no blur, no API floor.
 *  • **Multi-region** — `OGImageView(clipShape = OGMultiRegionShape(...))` combines several placed
 *    sub-shapes with a path op (union / difference), so one clip shows two portholes or a bitten
 *    diamond. It's a plain Shape, so it drops into the same clipShape slot (photos, GIFs, video).
 *
 * All zero-per-frame for a still photo. See docs/SOFT_MASKS.md.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SoftMaskScreen() {
    var photo by remember { mutableStateOf<PhotoSource>(PHOTO_CHOICES.first().toPhotoSource()) }
    var mode by remember { mutableStateOf(MaskMode.SOFT) }

    // Soft-edge state
    var softShape by remember { mutableStateOf(OGShapeType.CIRCLE) }
    var feather by remember { mutableStateOf(28f) }
    // Gradient state
    var gradient by remember { mutableStateOf(0) }
    // Multi-region state
    var multi by remember { mutableStateOf(0) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Soft & multi-region masks",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "A clip mask no longer has to be one shape with a hard edge. Feather the edge into a " +
                "vignette, fade the photo along a gradient, or clip it to more than one region at once " +
                "— all through the same OGImageView, no per-frame cost.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Photo picker (shared with the crop screen) ────────────────────────────────
        Text("Photo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        PhotoSourcePicker(
            selected = photo,
            onSelected = { photo = it; errorMessage = null },
            onError = { errorMessage = it }
        )

        // ── Mode ──────────────────────────────────────────────────────────────────────
        Text("Mask", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MaskMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    leadingIcon = { Text(m.glyph, fontSize = 14.sp) },
                    label = { Text(m.label) }
                )
            }
        }

        // ── Stage ───────────────────────────────────────────────────────────────────
        val source = remember(photo.key) { photo.toSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(cropStageBrush),
            contentAlignment = Alignment.Center
        ) {
            when (mode) {
                MaskMode.SOFT -> OGImageView(
                    source = source,
                    displayShape = softShape,
                    softEdge = feather.dp,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    onError = { errorMessage = it },
                    onEventTriggered = { _, _ -> },
                )
                MaskMode.GRADIENT -> OGImageView(
                    source = source,
                    maskBrush = GRADIENT_CHOICES[gradient].brush,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    onError = { errorMessage = it },
                    onEventTriggered = { _, _ -> },
                )
                MaskMode.MULTI -> OGImageView(
                    source = source,
                    clipShape = MULTI_CHOICES[multi].shape,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    onError = { errorMessage = it },
                    onEventTriggered = { _, _ -> },
                )
            }
        }

        // ── Per-mode controls ─────────────────────────────────────────────────────────
        when (mode) {
            MaskMode.SOFT -> {
                Text("Shape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CROP_SHAPE_CHOICES.forEach { choice ->
                        FilterChip(
                            selected = softShape == choice.type,
                            onClick = { softShape = choice.type },
                            leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                            label = { Text(choice.label) }
                        )
                    }
                }
                Text("Feather: ${feather.roundToInt()}dp", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Slider(value = feather, onValueChange = { feather = it }, valueRange = 0f..72f)
                Text(
                    "softEdge fades the photo out over this band near the shape's edge (0dp = a plain " +
                        "hard clip). One offscreen BlendMode.DstIn pass — the radial falloff is built " +
                        "once per size, so animating under it stays 60fps.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            MaskMode.GRADIENT -> {
                Text("Gradient", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GRADIENT_CHOICES.forEachIndexed { i, choice ->
                        FilterChip(
                            selected = gradient == i,
                            onClick = { gradient = i },
                            label = { Text(choice.label) }
                        )
                    }
                }
                Text(
                    "maskBrush multiplies the photo's alpha by a Brush running to Color.Transparent — " +
                        "an edge fade or a spotlight. Only the brush's alpha matters. Pass any " +
                        "Brush.linearGradient / radialGradient you like.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            MaskMode.MULTI -> {
                Text("Regions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MULTI_CHOICES.forEachIndexed { i, choice ->
                        FilterChip(
                            selected = multi == i,
                            onClick = { multi = i },
                            label = { Text(choice.label) }
                        )
                    }
                }
                Text(
                    "OGMultiRegionShape places several sub-shapes in normalized 0..1 sub-rects and " +
                        "combines them with a path op (UNION for portholes, DIFFERENCE for the bite). " +
                        "It's a plain Compose Shape → the same clipShape slot, so it also works on a " +
                        "running video or GIF. Combined once per size, never per frame.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        errorMessage?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                "⚠️ $it",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Under the hood: soft masks live in com.solidkey.painpoints.mask (Modifier.ogSoftClip / " +
                "ogGradientMask), surfaced on OGImageView as softEdge / maskBrush; multi-region is " +
                "com.solidkey.painpoints.shape.OGMultiRegionShape. Zero new dependencies, same code on " +
                "Android & iOS. See docs/SOFT_MASKS.md.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
