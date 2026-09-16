package com.solidkey.demo

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.animating.OGAnimatedImage
import com.solidkey.painpoints.image.animating.OGAnimationType
import com.solidkey.painpoints.image.loading.OGSvgResourceFileType
import kotlin.math.roundToInt

/** A bundled SVG logo that contains ZERO `<animate>` tags — i.e. genuinely static. */
private data class StaticArt(val label: String, val resource: String)

private val STATIC_ART = listOf(
    StaticArt("Atom", "atom"),
    StaticArt("Apple", "apple"),
    StaticArt("Compass", "compas"),
)

/** Friendly labels for the library's four animation primitives (OGAnimationType). */
private val PRIMITIVES = listOf(
    OGAnimationType.SCALE to "Scale",
    OGAnimationType.ROTATE to "Rotate",
    OGAnimationType.FADE to "Fade",
    OGAnimationType.TRANSLATE to "Slide",
)

/**
 * Showcases the *wrapping power* of KMPMedia: pick a fully-static image, tap the
 * library's animation primitives, and watch the still artwork come alive. The
 * primitives compose — enable several at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimateScreen() {
    var artIndex by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(1200f) }
    val active = remember { mutableStateListOf<OGAnimationType>() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Wrapping power",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "This SVG is 100% static — zero <animate> tags in the file. Every bit of " +
                "motion you see is KMPMedia wrapping it with its own animation primitives. " +
                "Tap to bring it to life; combine several at once.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // ── Stage: the static image, wrapped + animated by the library ──────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                OGAnimatedImage(
                    source = remember(artIndex) { OGSvgResourceFileType(STATIC_ART[artIndex].resource) },
                    animations = active.toSet(),
                    durationMillis = duration.roundToInt(),
                    width = 512f,
                    height = 512f,
                    modifier = Modifier.size(200.dp),
                    onError = { }
                )
            }
        }

        // ── Primitive toggles ───────────────────────────────────────────────────
        Text("Animation primitives", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PRIMITIVES.forEach { (type, label) ->
                val selected = type in active
                FilterChip(
                    selected = selected,
                    onClick = { if (selected) active.remove(type) else active.add(type) },
                    label = { Text(label) },
                    leadingIcon = if (selected) {
                        { Text("✓", color = MaterialTheme.colorScheme.onSecondaryContainer) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }
        Text(
            if (active.isEmpty()) "↑ Nothing selected — the image is perfectly still."
            else "Active: " + active.joinToString(", ") { t -> PRIMITIVES.first { it.first == t }.second },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        // ── Speed ───────────────────────────────────────────────────────────────
        Text("Cycle duration: ${duration.roundToInt()} ms", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Slider(
            value = duration,
            onValueChange = { duration = it },
            valueRange = 300f..3000f
        )

        // ── Swap the static source ──────────────────────────────────────────────
        Text("Static source", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            STATIC_ART.forEachIndexed { i, art ->
                FilterChip(
                    selected = artIndex == i,
                    onClick = { artIndex = i },
                    label = { Text(art.label) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Under the hood: OGAnimatedImage wraps OGSVGView and drives graphicsLayer " +
                "from OGAnimationType.{SCALE, ROTATE, FADE, TRANSLATE} on an infinite " +
                "transition — the same primitives any app can wrap its own content with.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
