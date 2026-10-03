package com.solidkey.demo

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * PoC transport for the "live agent-controlled game" idea (see
 * KMPMedia-internal-docs/LIVE_AGENT_GAMES_SPEC.md). An external agent pushes JSON patches onto this
 * bus and the running UFO Dodge loop collects them and mutates its director state, so the game
 * changes live with no rebuild. Here the bus is fed by MainActivity's broadcast receiver
 * (`adb ... am broadcast`), standing in for the nexum / Firebase realtime channel of the full design.
 * Platform-neutral: iOS simply never emits onto it (yet).
 */
object AgentPatchBus {
    // Small replay so a patch that arrives just before the game screen subscribes is still delivered.
    val patches = MutableSharedFlow<String>(replay = 4, extraBufferCapacity = 32)
    fun emit(json: String) { patches.tryEmit(json) }
}

/**
 * The outbound half of the live-agent channel: the running game publishes a periodic state snapshot
 * (score / hp / level / sprite count / current multipliers) here, and the platform transport forwards
 * it to the realtime channel so the agent can observe the game it is steering. Platform-neutral and
 * network-free — the Android transport PUTs each snapshot to nexum's RTDB `games/{ws}/{session}/state`;
 * iOS simply doesn't collect it yet.
 */
object AgentStateBus {
    // replay=1 so a transport that subscribes late still gets the latest snapshot immediately.
    val states = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 8)
    fun publish(json: String) { states.tryEmit(json) }
}

/**
 * One live tweak the agent can make to the running game. Flat and serialisable; validated (clamped)
 * where it is applied so a bad patch can never brick the game.
 */
data class GamePatch(
    val op: String,
    val spawnMul: Float? = null,   // op="rule":  hazard spawn-rate multiplier (>1 = faster)
    val speedMul: Float? = null,   // op="rule":  fall-speed multiplier (>1 = faster)
    val tint: String? = null,      // op="theme": hex colour wash over the field ("#RRGGBB" / "#AARRGGBB")
    val text: String? = null,      // op="banner": banner text to show
    val count: Int? = null,        // op="wave":  number of extra hazards to spawn right now
)

/**
 * Tiny tolerant parser for the flat PoC patch JSON, e.g. `{"op":"wave","count":6}`. Deliberately
 * dependency-free (no kotlinx-serialization plugin needed in the demo); the production nexum/Firebase
 * version would decode the same shape with the serializer the library already ships for OGAiVector.
 */
fun parseGamePatch(json: String): GamePatch? {
    val op = jsonStr(json, "op") ?: return null
    return GamePatch(
        op = op,
        spawnMul = jsonNum(json, "spawnMul"),
        speedMul = jsonNum(json, "speedMul"),
        tint = jsonStr(json, "tint"),
        text = jsonStr(json, "text"),
        count = jsonNum(json, "count")?.toInt(),
    )
}

private fun jsonStr(j: String, key: String): String? =
    Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(j)?.groupValues?.get(1)

private fun jsonNum(j: String, key: String): Float? =
    Regex("\"$key\"\\s*:\\s*(-?[0-9]*\\.?[0-9]+)").find(j)?.groupValues?.get(1)?.toFloatOrNull()

/** Parse "#RRGGBB" (given a translucent wash alpha) or "#AARRGGBB" (alpha as given) into a [Color]. */
fun parseHexColorOrNull(hex: String): Color? {
    val h = hex.trim().removePrefix("#")
    fun seg(i: Int) = h.substring(i, i + 2).toInt(16)
    return try {
        when (h.length) {
            6 -> Color(red = seg(0), green = seg(2), blue = seg(4), alpha = 0x55)
            8 -> Color(alpha = seg(0), red = seg(2), green = seg(4), blue = seg(6))
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
