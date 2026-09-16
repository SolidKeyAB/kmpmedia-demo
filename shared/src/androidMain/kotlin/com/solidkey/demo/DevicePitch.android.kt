package com.solidkey.demo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.atan2

// Tuning for the accelerometer → normalized-pitch mapping. The accelerometer's gravity vector gives
// the phone's tilt; we take the angle around the device X-axis (atan2(z, y)): ~0 when held upright in
// portrait, ~π/2 (1.57) when laid flat face-up. NEUTRAL is a natural "hold it up and look at it" angle,
// and ±SPAN around it maps to ∓1..±1. DEADZONE keeps a steady hold from drifting; ALPHA smooths jitter.
private const val PITCH_NEUTRAL = 0.85f   // ~49° from flat — comfortable upright-ish hold
private const val PITCH_SPAN = 0.70f      // ±40° of tilt covers the full depth range
private const val PITCH_DEADZONE = 0.06f
private const val SMOOTH_ALPHA = 0.18f

@Composable
actual fun deviceTiltSupported(): Boolean {
    val context = LocalContext.current
    return remember {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
}

@Composable
actual fun rememberDevicePitch(enabled: Boolean): State<Float> {
    val context = LocalContext.current
    val pitch = remember { mutableStateOf(0f) }

    DisposableEffect(enabled) {
        if (!enabled) {
            pitch.value = 0f
            return@DisposableEffect onDispose { }
        }
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sm == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }
        var smoothed = Float.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Gravity vector: x = right, y = toward the top edge, z = out of the screen.
                val ay = event.values[1]
                val az = event.values[2]
                // Angle around the X-axis: 0 when upright (y≈g), ~π/2 when flat (z≈g).
                val raw = atan2(az, ay)
                smoothed = if (smoothed.isNaN()) raw else smoothed + (raw - smoothed) * SMOOTH_ALPHA
                val delta = smoothed - PITCH_NEUTRAL
                // Tip the top BACK (toward flat) → delta grows → "rise"; tip forward → "dive".
                var n = (delta / PITCH_SPAN).coerceIn(-1f, 1f)
                if (abs(delta) < PITCH_DEADZONE) n = 0f
                pitch.value = n
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* unused */ }
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }
    return pitch
}
