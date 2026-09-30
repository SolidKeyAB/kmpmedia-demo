package com.solidkey.demo

import androidx.compose.runtime.Composable

/**
 * Writes GIF bytes to exactly where the library's `OGImageFileType(name, OGImageFormat.GIF)` will look
 * for them — `<appFilesDir>/<name>.gif` on Android, `<NSDocumentDirectory>/<name>.gif` on iOS — so the
 * Compositor screen can save a GIF it just generated in memory and then play it back through the real
 * platform GIF decoder (proving the export is a valid, decodable file). Demo-only, ZERO library change.
 */
fun interface GifSaver {
    /** Write [bytes] as `<name>.gif` in the app dir OGImageFileType resolves against. True on success. */
    fun save(name: String, bytes: ByteArray): Boolean
}

@Composable
expect fun rememberGifSaver(): GifSaver
