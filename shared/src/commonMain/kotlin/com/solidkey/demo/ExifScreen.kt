package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageUrlType

/**
 * Dogfoods 1.17.0's EXIF auto-rotation. Each JPEG below carries a non-default EXIF orientation tag
 * (the raw pixels are stored sideways / upside down; the camera recorded how the phone was held).
 * Before 1.17 these decoded rotated; now OGImageView reads the tag and shows them upright, so the
 * baked-in "this side up" text in every photo reads correctly and the aspect ratio is right.
 *
 * Images are the standard recurser/exif-orientation-examples set (needs network).
 */
@Composable
fun ExifScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Every photo here is tagged with a non-default EXIF orientation. They should all appear " +
                "UPRIGHT with readable text — proof the decoder applies the orientation tag. (Needs network.)",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
        ExifCase("Orientation 1 — Normal (control)", "Landscape_1")
        ExifCase("Orientation 6 — Rotate 90° CW", "Landscape_6")
        ExifCase("Orientation 8 — Rotate 270° CW", "Portrait_8")
        ExifCase("Orientation 3 — Rotate 180°", "Landscape_3")
    }
}

private const val EXIF_BASE =
    "https://raw.githubusercontent.com/recurser/exif-orientation-examples/master/"

@Composable
private fun ExifCase(title: String, name: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            OGImageView(
                source = OGImageUrlType("$EXIF_BASE$name.jpg"),
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentDescription = title,
                onEventTriggered = { _, _ -> },
            )
        }
    }
}
