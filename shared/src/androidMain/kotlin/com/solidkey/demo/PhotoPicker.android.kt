package com.solidkey.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Largest edge (px) we keep for a picked photo. A raw gallery shot is often 12 MP+ (a multi-MB file
 * that decodes to ~48 MB in memory), which is what made several cropped objects in the game flicker.
 * A phone screen is ~1080px wide and the crop preview is a square a bit smaller, so 1280px is crisp
 * everywhere while cutting the file to a few hundred KB and the decoded bitmap ~10×.
 */
private const val MAX_PICK_DIM = 1280

/** WebP quality for the re-encoded pick — visually lossless for photos, far smaller than the raw JPEG. */
private const val WEBP_QUALITY = 80

@Composable
actual fun rememberPhotoPicker(
    onPicked: (PhotoSource) -> Unit,
    onError: (String) -> Unit,
): PhotoPickerHandle {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // The system Photo Picker hands back a temporary content:// URI. We don't copy the raw bytes —
    // gallery photos are huge, and every game object decodes its own copy, which flickered. Instead
    // we downscale + re-encode to a compact WebP saved as "<name>.webp" (the exact path
    // OGImageFileType(name, WEBP) resolves to), so it's a small, reusable file that decodes cheaply.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // user backed out — no-op
        scope.launch {
            val name = "picked_${System.currentTimeMillis()}"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = decodeDownscaled(context, uri)
                        ?: error("couldn't decode the picked image")
                    val out = File(context.filesDir, "$name.webp")
                    val ok = out.outputStream().use { output ->
                        bitmap.compress(webpFormat(), WEBP_QUALITY, output)
                    }
                    bitmap.recycle()
                    if (!ok) error("couldn't encode the picked image")
                }
            }
            result
                .onSuccess { onPicked(PhotoSource.LocalFile(name)) }
                .onFailure { onError("Couldn't load that photo: ${it.message}") }
        }
    }

    return remember(launcher) {
        object : PhotoPickerHandle {
            override fun launch() = launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}

/**
 * Decode the picked [uri] downsampled so its longest edge is ≤ [MAX_PICK_DIM].
 *
 * Pass 1 reads only the dimensions: with `inJustDecodeBounds = true`, `decodeStream` populates
 * `outWidth/outHeight` and *returns null by design* — so we must NOT treat that null as a failure
 * (the original bug did, failing every pick). Pass 2 decodes the real bitmap, downsampled when we
 * learned the size (some formats don't report bounds — then we decode at full size and rely on the
 * library loader's own downsample/cache). Returns null only if the stream can't open or the pixels
 * genuinely can't be decoded.
 */
private fun decodeDownscaled(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver

    // Pass 1 — bounds only (decodeStream returns null here on purpose).
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    // Pass 2 — decode, downsampled if the size is known.
    var sample = 1
    if (bounds.outWidth > 0 && bounds.outHeight > 0) {
        while (bounds.outWidth / sample > MAX_PICK_DIM || bounds.outHeight / sample > MAX_PICK_DIM) sample *= 2
    }
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}

@Suppress("DEPRECATION")
private fun webpFormat(): Bitmap.CompressFormat =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY
    else Bitmap.CompressFormat.WEBP
