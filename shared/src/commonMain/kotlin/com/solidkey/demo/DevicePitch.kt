package com.solidkey.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Device motion → a single "tilt" value, used by the UFO game's optional **Tilt** control scheme
 * (tip the phone to change the UFO's depth). Demo-only, ZERO library dependency — pure platform
 * sensor code (Android [android.hardware.SensorManager] accelerometer, iOS CoreMotion device motion).
 *
 * Kept deliberately tiny: the game only needs one normalized pitch number and a "can this device do
 * it?" check so it can fall back to Pinch on hardware without a usable motion sensor.
 */

/** True if this device exposes a usable motion sensor (so the game may offer the Tilt option). */
@Composable
expect fun deviceTiltSupported(): Boolean

/**
 * A live, normalized device **pitch** in `-1f..1f`, updated continuously while [enabled] is true and
 * pinned to `0f` when disabled:
 *  - `+1` = the top of the phone tipped **back/up** (toward you) → the game reads this as "rise / over".
 *  - `-1` = the top tipped **forward/down** (away)             → "dive / under".
 *
 * The provider applies a neutral offset (a natural upright hold reads ~0), a small deadzone, and
 * low-pass smoothing, so the value is steady enough to drive the UFO's depth directly.
 */
@Composable
expect fun rememberDevicePitch(enabled: Boolean): State<Float>
