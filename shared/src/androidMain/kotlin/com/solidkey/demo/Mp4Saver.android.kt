package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

/** Android: write to `<filesDir>/<name>.mp4` — the exact path `OGVideoFileType("<name>.mp4")` reads. */
@Composable
actual fun rememberMp4Saver(): Mp4Saver {
    val dir = LocalContext.current.filesDir.absolutePath
    return remember(dir) {
        Mp4Saver { name, bytes ->
            runCatching { File("$dir/$name.mp4").writeBytes(bytes) }.isSuccess
        }
    }
}
