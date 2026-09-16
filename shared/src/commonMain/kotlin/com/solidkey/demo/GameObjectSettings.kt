package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.shape.OGShapeType
import kotlin.math.roundToInt

/** How a custom cropped object behaves in the game. */
enum class ObjectRole(val label: String, val glyph: String) {
    COLLECT("Collect (bonus)", "＋"),   // catch it for points
    DODGE("Dodge (hazard)", "☄"),      // hitting it damages you
}

/**
 * A user-authored game object: a bundled sample photo cropped into a [shape] with the same
 * zoom/pan/corner controls as the "Crop a Photo into a Shape" screen, plus a [role] that decides
 * whether it's a bonus to catch or a hazard to dodge. Everything needed to (a) re-preview it and
 * (b) spawn it live in [UfoGameScreen] is captured here — no library change, it's the existing
 * OGImageView crop params (displayShape / cornerRadius / contentScale / alignment) carried as data.
 */
data class CustomObjectSpec(
    val id: Long,
    val source: PhotoSource,
    val shape: OGShapeType,
    val cornerRadiusDp: Float,
    val crop: Boolean,
    val zoom: Float,
    val biasX: Float,
    val biasY: Float,
    val role: ObjectRole,
)

/** Space-theme palette so the editor matches the game it overlays. */
private val PanelBg = Color(0xFF0B0F2B)
private val CardBg = Color(0xFF141A3A)
private val Ink = Color.White
private val InkDim = Color(0xFFB9C0E0)

/**
 * The in-game **"Customize objects"** screen — the "settings of the game" the user asked for.
 *
 * It reuses the shared [CropStage] (identical pinch-to-zoom 0.25×–4× + drag-to-pan) and the shared
 * shape/photo/zoom building blocks, so cropping an object here works exactly like the standalone
 * crop screen — the answer to "can the user zoom in and out as well?" is *yes*, both by pinching
 * the preview and by dragging the Zoom slider. Tap **Add to game** to drop the current crop into
 * the field as a live game object; the game then spawns your photos among the meteorites.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameObjectSettings(
    objects: List<CustomObjectSpec>,
    onAdd: (CustomObjectSpec) -> Unit,
    onRemove: (Long) -> Unit,
    onDone: () -> Unit,
    nextId: () -> Long,
) {
    // The object's photo can be a bundled sample, a device-gallery pick, OR a pasted URL.
    var photo by remember { mutableStateOf<PhotoSource>(PHOTO_CHOICES.first().toPhotoSource()) }
    var shape by remember { mutableStateOf(OGShapeType.CIRCLE) }
    var crop by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(1f) }
    var bias by remember { mutableStateOf(Offset.Zero) }
    var cornerRadius by remember { mutableStateOf(36f) }
    var role by remember { mutableStateOf(ObjectRole.COLLECT) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PanelBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("⚙️ Customize objects", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "✕ Close",
                color = InkDim,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onDone() }
                    .background(CardBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Text(
            "Crop one of your photos into a shape and drop it into the game as a live object. " +
                "It falls among the meteorites, cropped to your shape and spinning as it drifts — " +
                "the same “crop a photo into a shape” + “animate” combo, now inside gameplay.",
            color = InkDim, fontSize = 13.sp
        )

        // ── Photo picker: bundled samples · pick from device · paste a URL ──────────────
        SectionLabel("Photo")
        PhotoSourcePicker(
            selected = photo,
            onSelected = { photo = it; errorMessage = null },
            onError = { errorMessage = it }
        )
        errorMessage?.let {
            Text("⚠️ $it", color = Color(0xFFFF8A8A), fontSize = 11.sp)
        }

        // ── Shape picker ──────────────────────────────────────────────────────────────
        SectionLabel("Shape")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CROP_SHAPE_CHOICES.forEach { choice ->
                FilterChip(
                    selected = shape == choice.type,
                    onClick = { shape = choice.type },
                    leadingIcon = { Text(choice.glyph, fontSize = 14.sp) },
                    label = { Text(choice.label) }
                )
            }
        }

        // ── Live crop preview: pinch to zoom, drag to pan (shared with the crop screen) ─
        CropStage(
            photo = photo,
            shape = shape,
            cornerRadiusDp = cornerRadius,
            crop = crop,
            zoom = zoom,
            bias = bias,
            onTransform = { z, b -> zoom = z; bias = b },
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        // ── Role: catch it or dodge it ────────────────────────────────────────────────
        SectionLabel("Behaviour")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ObjectRole.entries.forEach { r ->
                FilterChip(
                    selected = role == r,
                    onClick = { role = r },
                    leadingIcon = { Text(r.glyph, fontSize = 14.sp) },
                    label = { Text(r.label) }
                )
            }
        }
        Text(
            if (role == ObjectRole.COLLECT) "Catch it with your UFO for bonus points."
            else "It behaves like a meteorite — hitting it costs you a life.",
            color = InkDim, fontSize = 11.sp
        )

        // ── Fill mode ─────────────────────────────────────────────────────────────────
        SectionLabel("Fill mode")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = crop,
                onClick = { crop = !crop },
                label = { Text(if (crop) "Crop (fill the shape)" else "Fit (whole photo)") }
            )
        }

        // ── Zoom: pinch on the preview OR drag this slider (both set the same zoom) ─────
        SectionLabel("Zoom: ${(zoom * 10).roundToInt() / 10f}×")
        Slider(value = zoom, onValueChange = { zoom = it }, valueRange = MIN_ZOOM..MAX_ZOOM)
        Text(
            "Pinch the preview above OR drag this slider, 0.25×–4×. Above 1× zooms into a detail " +
                "(then drag to pan to it); below 1× shrinks the photo inside the shape.",
            color = InkDim, fontSize = 11.sp
        )

        // ── Corner radius ─────────────────────────────────────────────────────────────
        SectionLabel("Corner radius: ${cornerRadius.toInt()}dp")
        Slider(value = cornerRadius, onValueChange = { cornerRadius = it }, valueRange = 0f..96f)

        // ── Add ───────────────────────────────────────────────────────────────────────
        Button(
            onClick = {
                onAdd(
                    CustomObjectSpec(
                        id = nextId(),
                        source = photo,
                        shape = shape,
                        cornerRadiusDp = cornerRadius,
                        crop = crop,
                        zoom = zoom,
                        biasX = bias.x,
                        biasY = bias.y,
                        role = role,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("➕ Add this object to the game") }

        // ── Already added ─────────────────────────────────────────────────────────────
        if (objects.isEmpty()) {
            Text(
                "No custom objects yet — add one above and it'll appear in the field when you play.",
                color = InkDim, fontSize = 12.sp
            )
        } else {
            SectionLabel("In the game (${objects.size}) — tap ✕ to remove")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                objects.forEach { spec -> CustomObjectThumb(spec = spec, onRemove = { onRemove(spec.id) }) }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (objects.isEmpty()) "✓ Done" else "✓ Done — back to launch") }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

/** A small live preview of one added object — the actual cropped photo, its role badge, and a ✕. */
@Composable
private fun CustomObjectThumb(spec: CustomObjectSpec, onRemove: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp), contentAlignment = Alignment.TopEnd) {
            OGImageView(
                source = remember(spec.id) { spec.source.toSource() },
                displayShape = spec.shape,
                cornerRadius = spec.cornerRadiusDp.dp,
                contentScale = cropContentScale(spec.crop, spec.zoom),
                alignment = BiasAlignment(spec.biasX, spec.biasY),
                modifier = Modifier.fillMaxSize(),
                onError = { },
                onEventTriggered = { _, _ -> }
            )
            Text(
                "✕",
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onRemove() }
                    .background(Color(0xCCFF3B3B), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(spec.role.glyph, color = InkDim, fontSize = 12.sp)
    }
}
