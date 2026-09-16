package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.shape.OGShapeType

/**
 * Shared crop-editor building blocks — used by BOTH the standalone "Crop a Photo into a Shape"
 * screen ([CropShapeScreen]) and the in-game object editor ([GameObjectSettings]). Keeping the
 * shapes, photos, alignment grid, zoom range, the zoom-aware [ContentScale] and the interactive
 * [CropStage] in one place means the two features stay in lock-step (same shapes, same 0.25×–4×
 * pinch-zoom, same drag-to-pan) with no library change — exactly the DRY spirit of the feature.
 */

/** A pickable frame shape, mapping a friendly label to the library's [OGShapeType]. */
data class CropShapeChoice(val type: OGShapeType, val label: String, val glyph: String)

// SQUARE and RECTANGLE both resolve to a rounded rect in the library, so we expose one
// "Rounded" chip and the four genuinely distinct silhouettes — exactly like the video screen.
val CROP_SHAPE_CHOICES = listOf(
    CropShapeChoice(OGShapeType.RECTANGLE, "Rounded", "▭"),
    CropShapeChoice(OGShapeType.CIRCLE, "Circle", "●"),
    CropShapeChoice(OGShapeType.TRIANGLE_UP, "Triangle", "▲"),
    CropShapeChoice(OGShapeType.TRIANGLE_DOWN, "Inverted", "▽"),
    CropShapeChoice(OGShapeType.DIAMOND, "Diamond", "◆"),
)

/** A bundled sample JPEG. These stand alongside the user's own photos (device gallery / URL). */
data class PhotoChoice(val res: String, val label: String)

/** Bridge a bundled sample to the richer [PhotoSource] the crop flows now take. */
fun PhotoChoice.toPhotoSource(): PhotoSource.Resource = PhotoSource.Resource(res, label)

// Mix of aspect ratios on purpose: Fit vs Crop only differ when the photo's aspect ratio differs
// from the (square) stage. A square photo fills a square stage identically either way — so we lead
// with a genuinely landscape + portrait photo to make the toggle obvious.
val PHOTO_CHOICES = listOf(
    PhotoChoice("sample_landscape", "Landscape 16:9"),
    PhotoChoice("sample_portrait", "Portrait 9:16"),
    PhotoChoice("welcome_slide", "Square 1:1"),
    PhotoChoice("cuppy_slide1", "Cuppy 1:1"),
)

/** One of the 9 focal presets, as a bias [Offset] (x,y each in -1..1) fed to a BiasAlignment.
 *  Picks which part of an over-scaled (Crop) photo stays in frame; dragging tunes the same bias. */
data class AlignChoice(val bias: Offset, val glyph: String)

// A 3×3 grid mirroring the photo itself: tap a corner to keep that corner, ↑ to keep the top
// (a face), etc. Bias -1/0/1 = start/center/end on each axis — a BiasAlignment, which IS an
// Alignment, so it drops straight into OGImageView's alignment param (no new library API).
val ALIGN_GRID: List<List<AlignChoice>> = listOf(
    listOf(AlignChoice(Offset(-1f, -1f), "↖"), AlignChoice(Offset(0f, -1f), "↑"), AlignChoice(Offset(1f, -1f), "↗")),
    listOf(AlignChoice(Offset(-1f, 0f), "←"), AlignChoice(Offset(0f, 0f), "•"), AlignChoice(Offset(1f, 0f), "→")),
    listOf(AlignChoice(Offset(-1f, 1f), "↙"), AlignChoice(Offset(0f, 1f), "↓"), AlignChoice(Offset(1f, 1f), "↘")),
)

// Zoom range for the crop stage. Below 1× the photo is drawn SMALLER than the shape (it shrinks
// inward and the gradient shows around it, like a Fit letterbox pulled in); 1× = plain fill/fit;
// above 1× it's zoomed in past fill. Shared by both the pinch gesture and the slider.
const val MIN_ZOOM = 0.25f
const val MAX_ZOOM = 4f

/**
 * Build the crop [ContentScale] for a given fill mode + [zoom].
 *
 * Zoom is DRY: ContentScale is a fun-interface, so we wrap the base Crop/Fit and multiply its
 * scale factor by [zoom]. A bigger factor overflows the shape more; the caller's BiasAlignment then
 * picks which zoomed-in region shows — so pinch/slider reuse OGImageView's EXISTING contentScale
 * param with NO new library API (exactly as the pan reuses `alignment`). zoom == 1f → base as-is.
 */
fun cropContentScale(crop: Boolean, zoom: Float): ContentScale {
    val base = if (crop) ContentScale.Crop else ContentScale.Fit
    if (zoom == 1f) return base
    return object : ContentScale {
        override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
            val f = base.computeScaleFactor(srcSize, dstSize)
            return ScaleFactor(f.scaleX * zoom, f.scaleY * zoom)
        }
    }
}

/** The gradient painted BEHIND a shaped photo, so a circle/triangle/diamond reads as a floating
 *  cut-out (the shape clips the photo; the gradient shows through the clipped corners). */
val cropStageBrush: Brush
    get() = Brush.linearGradient(listOf(Color(0xFF00C2A8), Color(0xFF7B2FF7), Color(0xFFFF5DA2)))

/**
 * The interactive crop stage: a photo cropped into [shape], floating on the gradient, with
 * **pinch-to-zoom (0.25×–4×) AND one-finger drag-to-pan** unified in a single transform gesture.
 * Both the zoom and the pan are reported back through [onTransform] so the caller owns the state
 * (and can mirror it onto a Zoom slider / alignment grid). This is the exact same interaction the
 * standalone crop screen uses — reused verbatim so the in-game editor behaves identically.
 */
@Composable
fun CropStage(
    photo: PhotoSource,
    shape: OGShapeType,
    cornerRadiusDp: Float,
    crop: Boolean,
    zoom: Float,
    bias: Offset,
    onTransform: (zoom: Float, bias: Offset) -> Unit,
    modifier: Modifier = Modifier,
    stagePadding: Dp = 24.dp,
    onError: (String) -> Unit = {},
) {
    val source = remember(photo.key) { photo.toSource() }
    // detectTransformGestures runs in a long-lived coroutine (keyed on Unit), so read the LATEST
    // zoom/bias each event via rememberUpdatedState rather than the values captured at setup.
    val zoomState = rememberUpdatedState(zoom)
    val biasState = rememberUpdatedState(bias)
    Box(
        modifier = modifier
            .background(cropStageBrush)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val z = zoomState.value
                    val b = biasState.value
                    val newZoom = (z * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val newBias = Offset(
                        (b.x - pan.x * 2f / size.width.coerceAtLeast(1)).coerceIn(-1f, 1f),
                        (b.y - pan.y * 2f / size.height.coerceAtLeast(1)).coerceIn(-1f, 1f),
                    )
                    onTransform(newZoom, newBias)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        OGImageView(
            source = source,
            displayShape = shape,
            cornerRadius = cornerRadiusDp.dp,
            contentScale = cropContentScale(crop, zoom),
            alignment = BiasAlignment(bias.x, bias.y),
            modifier = Modifier.fillMaxSize().padding(stagePadding),
            onError = onError,
            onEventTriggered = { _, _ -> }
        )
    }
}

/**
 * The shared photo-source picker used by BOTH crop flows: the bundled sample chips, a
 * **"📁 Pick from device"** chip that opens the system gallery, and an **image-URL** field. Whatever
 * the user chooses is reported as a [PhotoSource] via [onSelected], and the current choice is
 * highlighted. Reused verbatim so "Crop a Photo into a Shape" and the in-game "Customize objects"
 * editor can both take a real user photo — a local file or a URL — not just the bundled samples.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhotoSourcePicker(
    selected: PhotoSource,
    onSelected: (PhotoSource) -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var urlText by remember { mutableStateOf("") }
    val picker = rememberPhotoPicker(onPicked = onSelected, onError = onError)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PHOTO_CHOICES.forEach { choice ->
                val isSel = selected is PhotoSource.Resource && selected.res == choice.res
                FilterChip(
                    selected = isSel,
                    onClick = { onSelected(choice.toPhotoSource()) },
                    label = { Text(choice.label) }
                )
            }
            // Opens the device gallery; the picked image comes back as a PhotoSource.LocalFile.
            FilterChip(
                selected = selected is PhotoSource.LocalFile,
                onClick = { picker.launch() },
                leadingIcon = { Text("📁", fontSize = 14.sp) },
                label = { Text(if (selected is PhotoSource.LocalFile) "My photo ✓" else "Pick from device") }
            )
        }
        // Or load any image URL — OGImageView's URL loader fetches it directly.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("or paste an image URL") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { if (urlText.isNotBlank()) onSelected(PhotoSource.Url(urlText.trim())) },
                enabled = urlText.isNotBlank()
            ) { Text("Load") }
        }
    }
}
