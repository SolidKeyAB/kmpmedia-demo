package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import platform.CoreMotion.CMMotionManager
import kotlin.math.abs

// iOS CoreMotion reports attitude.pitch in radians: ~0 when the phone is flat face-up and ~π/2 when
// held upright in portrait. Tipping the top BACK (toward flat) DECREASES pitch, so we measure
// (NEUTRAL - pitch) to keep "tip top up = rise" consistent with the Android mapping. NEUTRAL is a
// natural upright-ish hold. (These are reasonable defaults — the exact feel is worth confirming on a
// real device, which a headless simulator can't reproduce.)
private const val PITCH_NEUTRAL = 1.05    // ~60° upright-ish hold
private const val PITCH_SPAN = 0.70       // ±40° covers the full depth range
private const val PITCH_DEADZONE = 0.06
private const val SMOOTH_ALPHA = 0.18

@Composable
actual fun deviceTiltSupported(): Boolean =
    remember { CMMotionManager().deviceMotionAvailable }

@Composable
actual fun rememberDevicePitch(enabled: Boolean): State<Float> {
    val pitch = remember { mutableStateOf(0f) }
    val manager = remember { CMMotionManager() }

    LaunchedEffect(enabled) {
        if (!enabled || !manager.deviceMotionAvailable) {
            pitch.value = 0f
            return@LaunchedEffect
        }
        manager.deviceMotionUpdateInterval = 1.0 / 60.0
        manager.startDeviceMotionUpdates()
        var smoothed = Double.NaN
        try {
            while (true) {
                val p = manager.deviceMotion?.attitude?.pitch
                if (p != null) {
                    smoothed = if (smoothed.isNaN()) p else smoothed + (p - smoothed) * SMOOTH_ALPHA
                    // Tip the top BACK (pitch decreases) → positive delta → "rise".
                    val delta = PITCH_NEUTRAL - smoothed
                    var n = (delta / PITCH_SPAN).coerceIn(-1.0, 1.0)
                    if (abs(delta) < PITCH_DEADZONE) n = 0.0
                    pitch.value = n.toFloat()
                }
                delay(16)
            }
        } finally {
            manager.stopDeviceMotionUpdates()
        }
    }
    return pitch
}
