package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageUrlType
import com.solidkey.painpoints.shape.OGShapeType

/**
 * 🎞️ Shows the library playing real **animated GIFs** — identical Compose Multiplatform code on
 * Android and iOS. Each GIF is a plain `OGImageView` pointed at a `.gif` URL: it auto-detects the
 * format and loops the frames (Android `AnimatedImageDrawable`, iOS Skia `Codec`). The same clip
 * that crops a still photo into a circle / triangle / diamond works on the *moving* image too.
 */
@Composable
fun GifScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Animated GIFs — one OGImageView call",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Point OGImageView at a .gif and it plays. Same code on Android & iOS — and the " +
                        "shape clip that crops a photo works on the moving frames too.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }
        }

        // Hero — a full-frame looping GIF from a URL, no bundling needed.
        item {
            GifBlock(label = "Straight up — a .gif URL, looping") {
                OGImageView(
                    source = OGImageUrlType(CRADLE),
                    modifier = Modifier.size(220.dp),
                    displayShape = OGShapeType.RECTANGLE,
                    cornerRadius = 16.dp,
                    contentScale = ContentScale.Fit,
                    onEventTriggered = { _, _ -> },
                    onError = { }
                )
            }
        }

        // The moving image cropped into shapes — the same GPU clip used for still photos.
        item {
            GifBlock(label = "Animating inside a shape clip") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OGImageView(
                        source = OGImageUrlType(HORSE),
                        modifier = Modifier.size(140.dp),
                        displayShape = OGShapeType.CIRCLE,
                        contentScale = ContentScale.Crop,
                        onEventTriggered = { _, _ -> },
                        onError = { }
                    )
                    OGImageView(
                        source = OGImageUrlType(HORSE),
                        modifier = Modifier.size(140.dp),
                        displayShape = OGShapeType.DIAMOND,
                        contentScale = ContentScale.Crop,
                        onEventTriggered = { _, _ -> },
                        onError = { }
                    )
                }
            }
        }

        // Everyday use — the classic real-world case: a loading spinner.
        item {
            GifBlock(label = "Everyday: a loading spinner") {
                OGImageView(
                    source = OGImageUrlType(SPINNER),
                    modifier = Modifier.size(120.dp),
                    cornerRadius = 8.dp,
                    contentScale = ContentScale.Fit,
                    onEventTriggered = { _, _ -> },
                    onError = { }
                )
            }
        }
    }
}

@Composable
private fun GifBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

// Small, public-domain / freely-licensed animated GIFs from Wikimedia Commons.
private const val HORSE =
    "https://upload.wikimedia.org/wikipedia/commons/e/e3/Animhorse.gif"
private const val SPINNER =
    "https://upload.wikimedia.org/wikipedia/commons/b/b1/Loading_icon.gif"
private const val CRADLE =
    "https://upload.wikimedia.org/wikipedia/commons/d/d3/Newtons_cradle_animation_book_2.gif"
