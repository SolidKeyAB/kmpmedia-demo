package com.solidkey.demo

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.cutout.OGChromaKeySegmenter
import com.solidkey.painpoints.cutout.autoCutoutPolygon
import com.solidkey.painpoints.shape.OGPolygonShape
import kotlinx.coroutines.Dispatchers

/**
 * Dogfoods 1.19.0 auto-cutout. We synthesise a "subject on a plain white background", then run the
 * zero-dependency chroma-key segmenter + contour tracer to turn the subject into a live OGPolygonShape
 * lasso — and clip the same image to it, so the white background is gone. (Plug an ML segmenter in for
 * real cluttered photos; the lib only ships the pluggable interface + the mask→lasso tracing.)
 */
@Composable
fun AutoCutoutScreen() {
    val subject = remember { buildSubjectOnWhite(256) }

    // Off the main thread: segment (white bg) → trace the subject outline → polygon.
    val polygon by produceState<OGPolygonShape?>(null, subject) {
        value = kotlinx.coroutines.withContext(Dispatchers.Default) {
            autoCutoutPolygon(
                image = subject,
                segmenter = OGChromaKeySegmenter(background = Color.White, tolerance = 0.12f),
                simplifyTolerance = 0.012f,
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Auto-cutout: the library turns a segmentation mask into a live polygon lasso. Here a " +
                "zero-dependency chroma-key segmenter removes the plain white background; for real " +
                "cluttered photos you plug in ML Kit / Vision / a cloud model via OGSegmenter.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
        Card(Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Labeled("Original\n(white background)") {
                    Box(Modifier.size(128.dp).background(Color(0xFFEDEDED))) {
                        Image(subject, contentDescription = "subject on white", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                    }
                }
                Labeled("Auto-cutout\n(background removed)") {
                    // A checker-ish gradient so the removed background is visibly gone.
                    Box(
                        Modifier.size(128.dp).background(
                            Brush.linearGradient(listOf(Color(0xFF4DD0E1), Color(0xFF7E57C2))),
                        ),
                    ) {
                        val p = polygon
                        if (p != null) {
                            Image(
                                subject,
                                contentDescription = "subject cut out",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize().clip(p),
                            )
                        }
                    }
                }
            }
        }
        Text(
            if (polygon != null) "Traced outline: ${polygon!!.points.size} points." else "Tracing…",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        content()
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/** A clear "subject" (orange blob + triangle hat) centred on a solid white background. */
private fun buildSubjectOnWhite(size: Int): ImageBitmap {
    val bmp = ImageBitmap(size, size)
    val canvas = Canvas(bmp)
    val paint = Paint()
    val s = size.toFloat()
    // white background
    paint.color = Color.White
    canvas.drawRect(0f, 0f, s, s, paint)
    // body: a big circle
    paint.color = Color(0xFFEF6C00)
    canvas.drawCircle(Offset(s * 0.5f, s * 0.58f), s * 0.28f, paint)
    // head: a triangle sitting on top
    paint.color = Color(0xFF8E24AA)
    val path = Path().apply {
        moveTo(s * 0.5f, s * 0.12f)
        lineTo(s * 0.70f, s * 0.42f)
        lineTo(s * 0.30f, s * 0.42f)
        close()
    }
    canvas.drawPath(path, paint)
    return bmp
}
