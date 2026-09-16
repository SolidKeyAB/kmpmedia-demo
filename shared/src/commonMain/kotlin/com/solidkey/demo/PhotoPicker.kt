package com.solidkey.demo

import androidx.compose.runtime.Composable
import com.solidkey.painpoints.image.loading.OGImageFileType
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.image.loading.OGImageUrlType
import com.solidkey.painpoints.source.OGSourceType

/**
 * Where a photo comes from. The demo used to hand OGImageView only bundled sample resources; this
 * lets the crop editor AND the in-game object editor take a photo the user actually chose — either
 * from the device gallery ([LocalFile]) or from a pasted [Url], which is exactly what a real app
 * does. Each variant just builds the matching library source object OGImageView already understands
 * ([OGImageResourceFileType] / [OGImageUrlType] / [OGImageFileType]), so this is a demo-only change
 * with ZERO library change — the same spirit as the pan/zoom features.
 */
sealed class PhotoSource {
    /** Stable identity used for remember() keys and selection highlighting. */
    abstract val key: String

    /** Short human label for chips / badges. */
    abstract val label: String

    /** Build the library source OGImageView consumes (resource / URL / file). */
    abstract fun toSource(): OGSourceType

    /** A JPEG bundled in the app's drawables (the built-in sample photos). */
    data class Resource(val res: String, override val label: String) : PhotoSource() {
        override val key: String get() = "res:$res"
        override fun toSource(): OGSourceType = OGImageResourceFileType(res, OGImageFormat.JPEG)
    }

    /** Any http(s) image URL, loaded by the library's URL loader. */
    data class Url(val url: String) : PhotoSource() {
        override val key: String get() = "url:$url"
        override val label: String get() = "From URL"
        override fun toSource(): OGSourceType = OGImageUrlType(url)
    }

    /**
     * A photo the user picked from the device. The picker downscales it and re-encodes it into the
     * app's private files dir under [fileName] (no extension). The library's [OGImageFileType]
     * resolves it to `<filesDir>/<fileName>.<format.extension>`, so the picker MUST save the bytes
     * with that exact name + the [format] it declares here. Re-encoding keeps the picked file small
     * and quick to decode — the fix for the flicker several full-res gallery photos caused.
     *
     * The re-encode [format] is per-platform because encoders differ: **Android writes WebP**
     * (`Bitmap.compress`), while **iOS writes JPEG** (`UIImageJPEGRepresentation` — iOS decodes WebP
     * since iOS 14 but has no built-in WebP *encoder*). Either way the library downsamples, decodes
     * off the main thread and caches the result, so the picked-photo experience is identical on both.
     */
    data class LocalFile(
        val fileName: String,
        val format: OGImageFormat = OGImageFormat.WEBP,
        override val label: String = "My photo",
    ) : PhotoSource() {
        override val key: String get() = "file:$fileName"
        override fun toSource(): OGSourceType = OGImageFileType(fileName, format)
    }
}

/** A handle whose [launch] opens the device photo picker. Returned by [rememberPhotoPicker]. */
interface PhotoPickerHandle {
    fun launch()
}

/**
 * Remember a device photo picker. Calling [PhotoPickerHandle.launch] opens the platform gallery;
 * once the user picks an image it's saved locally and delivered as a [PhotoSource.LocalFile] via
 * [onPicked]. Cancellation is a no-op; failures report through [onError].
 *
 * - **Android:** uses the system Photo Picker ([ActivityResultContracts.PickVisualMedia]).
 * - **iOS:** uses [PHPickerViewController] (the system photo library). Fully wired — no platform
 *   deviation: both platforms downscale the pick, save it locally and hand back a [LocalFile].
 */
@Composable
expect fun rememberPhotoPicker(
    onPicked: (PhotoSource) -> Unit,
    onError: (String) -> Unit = {},
): PhotoPickerHandle
