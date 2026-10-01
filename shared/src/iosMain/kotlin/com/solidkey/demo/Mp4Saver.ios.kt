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

/** iOS: write to `<NSDocumentDirectory>/<name>.mp4` — the exact path `OGVideoFileType("<name>.mp4")` reads. */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberMp4Saver(): Mp4Saver {
    val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String
    return remember(dir) {
        Mp4Saver { name, bytes ->
            if (dir == null || bytes.isEmpty()) return@Mp4Saver false
            val data = bytes.usePinned { pinned ->
                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            }
            data.writeToFile("$dir/$name.mp4", atomically = true)
        }
    }
}
