package com.solidkey.demo

import androidx.compose.runtime.Composable

/**
 * Writes MP4 bytes to exactly where the library's `OGVideoFileType(name)` will look for them —
 * `<appFilesDir>/<name>` on Android, `<NSDocumentDirectory>/<name>` on iOS — so the Compositor screen
 * can save an MP4 it just generated in memory (`OGComposition.exportMp4()`) and then play it back
 * through the real platform video player (`OGAVPlayer`), proving the export is a valid, decodable file.
 * The MP4 twin of [GifSaver]. Demo-only, ZERO library change.
 */
fun interface Mp4Saver {
    /** Write [bytes] as `<name>.mp4` in the dir `OGVideoFileType` resolves against. True on success. */
    fun save(name: String, bytes: ByteArray): Boolean
}

@Composable
expect fun rememberMp4Saver(): Mp4Saver
