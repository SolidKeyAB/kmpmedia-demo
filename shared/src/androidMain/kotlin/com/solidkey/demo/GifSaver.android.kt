package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Android: write to `<filesDir>/<name>.gif` — the exact path `OGImageFileType(name, GIF)` reads. */
@Composable
actual fun rememberGifSaver(): GifSaver {
    val dir = LocalContext.current.filesDir.absolutePath
    return remember(dir) {
        GifSaver { name, bytes ->
            runCatching { File("$dir/$name.gif").writeBytes(bytes) }.isSuccess
        }
    }
}
