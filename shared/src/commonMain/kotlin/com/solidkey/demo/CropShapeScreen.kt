package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.shape.OGShapeType
import kotlin.math.roundToInt

// The shapes, photos, alignment grid, zoom range, the zoom-aware ContentScale and the interactive
// CropStage all live in CropEditor.kt now — shared verbatim with the in-game object editor
// (GameObjectSettings) so both crop flows behave identically. See that file.

/**
 * The image twin of "Video in any shape": pick a photo, pick a shape, and OGImageView crops the
 * picture into that silhouette. The whole feature is three new params on OGImageView —
 * [OGImageView] `displayShape` / `cornerRadius` / `contentScale` — and the clip is a Compose GPU
 * mask drawn ONCE for a still image, so there is no runtime cost (the same mechanism the video
 * screen runs live at 60fps).
 *
 *  • **Crop (fill)** — `contentScale = ContentScale.Crop` scales the photo up until it fills the
 *    shape and clips the overflow, so you see a cropped *piece* of the picture with no empty areas.
 *  • **Fit (letterbox)** — `contentScale = ContentScale.Fit` shows the whole photo inside the shape.
 *
 * A real app would pass a device-picked photo via `OGImageFileType(path)` or `OGImageUrlType(url)`;
 * the bundled JPEGs here stand in for that "picked" image.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CropShapeScreen() {
    var shape by remember { mutableStateOf(OGShapeType.CIRCLE) }
    // The photo can now be a bundled sample, a device-gallery pick, OR a pasted URL — see
    // PhotoSourcePicker below. Default to the first bundled sample so the stage opens with content.
    var photo by remember { mutableStateOf<PhotoSource>(PHOTO_CHOICES.first().toPhotoSource()) }
    // Default to crop-to-fill so the shape opens completely filled — the "cropped piece" look.
    var crop by remember { mutableStateOf(true) }
    // Focal bias (x,y in -1..1) → BiasAlignment: which part of an over-scaled (Crop) photo stays in
    // frame. Zero = center (old behavior). A grid preset or a drag on the stage both just set this.
    var bias by remember { mutableStateOf(Offset.Zero) }
    // Zoom factor (MIN_ZOOM..MAX_ZOOM; 1x = plain fill/fit). Pinch on the stage OR the slider both set
    // this; it multiplies the base ContentScale's factor — >1 makes the photo bigger inside the shape,
    // <1 makes it SMALLER than the shape (shrinks inward, gradient shows around it).
    var zoom by remember { mutableStateOf(1f) }
    // Start clearly rounded so the "Rounded" shape reads as a rounded card, not a plain rectangle
    // (an 18dp radius on a ~345dp stage was too subtle to notice).
    var cornerRadius by remember { mutableStateOf(36f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Crop a photo into a shape",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Hand OGImageView any photo and a shape — it crops the picture into that silhouette. " +
                "Same primitives as the video player; the clip is a GPU mask drawn once, so it's free.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Photo picker: bundled samples · pick from device · paste a URL ────────────
        Text("Photo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        PhotoSourcePicker(
            selected = photo,
            onSelected = { photo = it; errorMessage = null },
            onError = { errorMessage = it }
        )

        // ── Shape picker ────────────────────────────────────────────────────────────
        Text("Crop shape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CROP_SHAPE_CHOICES.forEach { choice ->
                FilterChip(
                    selected = shape == choice.type,
                    onClick = { shape = choice.type },
                    leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                    label = { Text(choice.label) }
                )
            }
        }

        // ── Stage: the photo, cropped to the chosen shape, floating on a gradient ─────
        // Pinch to zoom (0.25×–4×) AND drag with one finger to pan — the shared CropStage owns that
        // interaction; it reports the new zoom/bias back so the Zoom slider + alignment grid stay
        // in sync. (The same stage is reused by the in-game object editor.)
        CropStage(
            photo = photo,
            shape = shape,
            cornerRadiusDp = cornerRadius,
            crop = crop,
            zoom = zoom,
            bias = bias,
            onTransform = { z, b -> zoom = z; bias = b },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            onError = { errorMessage = it }
        )

        // ── Options ─────────────────────────────────────────────────────────────────
        Text("Fill mode", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = crop,
                onClick = { crop = !crop },
                label = { Text(if (crop) "Crop (fill the shape)" else "Fit (whole photo)") }
            )
        }
        Text(
            "Crop scales the photo up to fill the shape (you see a cropped piece); Fit scales it down " +
                "so the whole photo shows, letterboxed — the empty area is transparent, so the gradient " +
                "shows through. Try it with the Landscape/Portrait photos: a Square photo fills a square " +
                "stage identically either way, so the toggle only bites when the aspect ratios differ.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Alignment: tap a preset OR drag the photo to frame it freely ──────────────
        Text("Alignment — tap a preset, or drag the photo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ALIGN_GRID.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { choice ->
                        val selected = bias == choice.bias
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { bias = choice.bias },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                choice.glyph,
                                fontSize = 18.sp,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Text(
            "With Crop the photo is scaled bigger than the shape — tap a cell to snap the focal point " +
                "to an edge/corner, or just DRAG the photo in the frame above to pan it freely. Both " +
                "feed one BiasAlignment (x,y in -1..1) into OGImageView's alignment param — no new " +
                "library API, the drag is pure demo code.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        // ── Zoom: pinch on the stage OR drag this slider (both set the same `zoom`) ───
        Text(
            "Zoom: ${(zoom * 10).roundToInt() / 10f}×",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Slider(value = zoom, onValueChange = { zoom = it }, valueRange = MIN_ZOOM..MAX_ZOOM)
        Text(
            "Pinch the photo in the frame above, or drag this slider, from 0.25× up to 4×. Above 1× you " +
                "zoom into a cropped detail (then drag to pan to it — more room the more you zoom); below " +
                "1× the whole photo shrinks smaller than the shape, so the gradient shows around it. " +
                "Zoom is DRY: it just multiplies the base fill scale, so it drops into OGImageView's " +
                "existing contentScale param — no new library API.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Text("Corner radius: ${cornerRadius.toInt()}dp", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Slider(value = cornerRadius, onValueChange = { cornerRadius = it }, valueRange = 0f..96f)
        Text(
            "Corner radius rounds the Rounded/Triangle/Diamond corners; a Circle ignores it.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

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
            "Under the hood: OGImageView(displayShape = …, contentScale = ContentScale.Crop). " +
                "displayShape resolves to a Compose Shape via the shared com.solidkey.painpoints.shape " +
                "package (the same one the video player uses) and is applied with Modifier.clip — one " +
                "GPU mask, drawn once for a still image, so cropping into a shape costs nothing at runtime.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
