package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile

/** iOS: write to `<NSDocumentDirectory>/<name>.gif` — the exact path `OGImageFileType(name, GIF)` reads. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberGifSaver(): GifSaver {
    val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
    return remember(dir) {
        GifSaver { name, bytes ->
            if (dir == null || bytes.isEmpty()) return@GifSaver false
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToFile("$dir/$name.gif", atomically = true)
        }
    }
}
