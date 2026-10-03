package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.camera.OGCameraFacing
import com.solidkey.painpoints.camera.OGCameraPreview
import com.solidkey.painpoints.shape.OGShapeType
import com.solidkey.painpoints.shape.toShape

/** Runtime camera permission, bridged per platform (Android ActivityResult / iOS AVCaptureDevice). */
data class CameraPermission(val granted: Boolean, val request: () -> Unit)

@Composable
expect fun rememberCameraPermission(): CameraPermission

/**
 * Dogfoods 1.22.0: a live camera feed clipped to any shape (OGCameraPreview) — the AR-sticker
 * primitive. Toggle the shape (circle / triangle / diamond) and the camera (back / front). Zero
 * third-party dependency: Camera2 on Android, AVFoundation on iOS.
 */
@Composable
fun CameraScreen() {
    val permission = rememberCameraPermission()
    var facing by remember { mutableStateOf(OGCameraFacing.BACK) }
    var shapeName by remember { mutableStateOf("Circle") }
    var error by remember { mutableStateOf<String?>(null) }

    val shape: Shape = when (shapeName) {
        "Triangle" -> OGShapeType.TRIANGLE_UP.toShape()
        "Diamond" -> OGShapeType.DIAMOND.toShape()
        else -> CircleShape
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Live camera, clipped to any shape — the AR-sticker primitive. OGCameraPreview wires the " +
                "platform camera (Camera2 / AVFoundation, no third-party dependency) and masks the feed " +
                "to the shape you pick.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        if (!permission.granted) {
            Text("Camera permission is needed to show the live feed.", fontSize = 13.sp)
            Button(onClick = permission.request, modifier = Modifier.fillMaxWidth()) {
                Text("Grant camera permission")
            }
            return@Column
        }

        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            OGCameraPreview(
                modifier = Modifier.fillMaxSize(),
                shape = shape,
                facing = facing,
                onError = { error = it },
            )
        }

        Text("Shape", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row3 {
            for (name in listOf("Circle", "Triangle", "Diamond")) {
                FilterChip(selected = shapeName == name, onClick = { shapeName = name }, label = { Text(name) })
            }
        }
        Text("Camera", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Row3 {
            FilterChip(selected = facing == OGCameraFacing.BACK, onClick = { facing = OGCameraFacing.BACK }, label = { Text("Back") })
            FilterChip(selected = facing == OGCameraFacing.FRONT, onClick = { facing = OGCameraFacing.FRONT }, label = { Text("Front") })
        }
        error?.let { Text("⚠️ $it", color = Color(0xFFC62828), fontSize = 12.sp) }
    }
}

@Composable
private fun Row3(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
