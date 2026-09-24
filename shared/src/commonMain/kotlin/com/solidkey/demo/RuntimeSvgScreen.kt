package com.solidkey.demo

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.loading.OGSvgFileType
import com.solidkey.painpoints.image.loading.seedSvgFile
import com.solidkey.painpoints.image.svg.OGSVGView
import com.solidkey.painpoints.image.svg.OGSvgNodeOverride
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 🎨 Runtime-editable SVG. One `.svg` is loaded **once**; moving the slider only changes an
 * `overrides` map (per-node, keyed by `id`), and the same SVG redraws live — the needle rotates
 * and the arc/status recolour. No re-parse, and identical Compose Multiplatform code on Android
 * and iOS. This is the "SVG as a live template" idea: gauges, charts, progress rings, status icons.
 */
@Composable
fun RuntimeSvgScreen() {
    // A tiny gauge SVG with three addressable nodes: id="needle", id="arc", id="status".
    // Seeded to disk once so it loads cross-platform without bundling a resource.
    var gaugePath by remember { mutableStateOf<String?>(null) }
    seedSvgFile("runtime_gauge.svg", GAUGE_SVG) { gaugePath = it }

    var value by remember { mutableStateOf(30f) } // 0..100

    // Map the value to the needle angle (−90° left … +90° right) and a zone colour.
    val angle = -90f + (value / 100f) * 180f
    val zone = when {
        value < 50f -> Color(0xFF22C55E) // green
        value < 80f -> Color(0xFFF59E0B) // amber
        else -> Color(0xFFEF4444)        // red
    }
    val overrides = mapOf(
        "needle" to OGSvgNodeOverride(rotation = angle, rotationCx = 100f, rotationCy = 105f),
        "arc" to OGSvgNodeOverride(stroke = zone),
        "status" to OGSvgNodeOverride(fill = zone),
    )

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Runtime-editable SVG — one live template",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "The SVG is parsed once. The slider only changes an overrides map keyed by node id — " +
                "the needle rotates and the arc + status dot recolour, live. Same code on Android & iOS.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val path = gaugePath
            if (path != null) {
                OGSVGView(
                    source = OGSvgFileType(path),
                    width = 300f,
                    height = 195f,
                    overrides = overrides,
                    onError = { }
                )
            } else {
                Text("Preparing…", fontSize = 13.sp)
            }
        }

        Text(
            "Value: ${value.roundToInt()}",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Slider(value = value, onValueChange = { value = it }, valueRange = 0f..100f)

        // Quick presets — also handy for driving the screen from automation.
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 25, 50, 75, 100).forEach { preset ->
                Button(onClick = { value = preset.toFloat() }) { Text("$preset") }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "overrides = mapOf(\n  \"needle\" to OGSvgNodeOverride(rotation = angle, rotationCx = 100f, rotationCy = 105f),\n  \"arc\" to OGSvgNodeOverride(stroke = zoneColor),\n  \"status\" to OGSvgNodeOverride(fill = zoneColor),\n)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(20.dp))

        // ── Runtime path `d` override ─────────────────────────────────────────────
        // The SAME overrides map can now carry a replacement path `d`: the addressed
        // <path id="icon"> re-parses to new geometry live, without re-parsing the whole SVG.
        var iconPath by remember { mutableStateOf<String?>(null) }
        seedSvgFile("runtime_icon.svg", ICON_SVG) { iconPath = it }
        var iconKey by remember { mutableStateOf("play") }
        val iconOverrides = mapOf("icon" to OGSvgNodeOverride(pathData = ICON_PATHS.getValue(iconKey)))

        Text(
            "Change a node's path — runtime `d` override",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Same idea, now the geometry: the override carries a new path `d`, so the " +
                "<path id=\"icon\"> re-parses to a new shape live — no new node, no full re-parse. " +
                "The building block for path morphing.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val ip = iconPath
            if (ip != null) {
                OGSVGView(
                    source = OGSvgFileType(ip),
                    width = 140f,
                    height = 140f,
                    overrides = iconOverrides,
                    onError = { }
                )
            } else {
                Text("Preparing…", fontSize = 13.sp)
            }
        }
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ICON_LABELS.forEach { (key, label) ->
                Button(onClick = { iconKey = key }) { Text(label) }
            }
        }
        Text(
            "overrides = mapOf(\n  \"icon\" to OGSvgNodeOverride(pathData = playOrPauseOrStopD),\n)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(20.dp))

        // ── Path morphing — animated tween ────────────────────────────────────────
        // The payoff: a <path> tweens smoothly between two shapes. Both `d` strings are
        // parsed ONCE; a Compose infinite-transition drives morphProgress 0→1→0 and each
        // frame only interpolates floats — no re-parse, no allocation churn — so it holds
        // 60fps (same cost profile as the animated needle above).
        var morphPath by remember { mutableStateOf<String?>(null) }
        seedSvgFile("runtime_morph.svg", MORPH_SVG) { morphPath = it }
        val morphTransition = rememberInfiniteTransition(label = "morph")
        val morphT by morphTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "morphProgress"
        )
        val morphOverrides = mapOf(
            "blob" to OGSvgNodeOverride(pathDataTo = MORPH_POLYGON_D, morphProgress = morphT)
        )

        Text(
            "Morph a path — animated tween",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "The star relaxes into a ring and back, forever. The two shapes share the same command " +
                "structure; parsed once, the frame loop only lerps the points. Same code on Android & iOS.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val mp = morphPath
            if (mp != null) {
                OGSVGView(
                    source = OGSvgFileType(mp),
                    width = 180f,
                    height = 180f,
                    overrides = morphOverrides,
                    onError = { }
                )
            } else {
                Text("Preparing…", fontSize = 13.sp)
            }
        }
        Text(
            "morphProgress = ${(morphT * 100).roundToInt()}%",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "overrides = mapOf(\n  \"blob\" to OGSvgNodeOverride(pathDataTo = ringD, morphProgress = t),\n)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

// A minimal gauge: a coloured arc (id="arc"), tick marks, a needle (id="needle") pinned at the
// hub (100,105), a hub cap, and a status dot (id="status").
private val GAUGE_SVG: String = """
<svg width="200" height="130" viewBox="0 0 200 130" xmlns="http://www.w3.org/2000/svg">
  <path id="arc" d="M 24 105 A 76 76 0 0 1 176 105" fill="none" stroke="#22C55E" stroke-width="12"/>
  <line x1="24" y1="105" x2="36" y2="105" stroke="#94A3B8" stroke-width="3"/>
  <line x1="100" y1="27" x2="100" y2="39" stroke="#94A3B8" stroke-width="3"/>
  <line x1="176" y1="105" x2="164" y2="105" stroke="#94A3B8" stroke-width="3"/>
  <path id="needle" d="M 96 105 L 100 40 L 104 105 Z" fill="#0F172A"/>
  <circle id="hub" cx="100" cy="105" r="9" fill="#0F172A"/>
  <circle id="status" cx="100" cy="122" r="5" fill="#22C55E"/>
</svg>
""".trimIndent()

// A single addressable path (id="icon") on a soft disc. Its `d` is swapped at runtime by the
// override map — one <path> node, three geometries. Initial `d` = the ▶ play triangle.
private val ICON_SVG: String = """
<svg width="100" height="100" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="46" fill="#EEF2FF"/>
  <path id="icon" d="M35 25 L75 50 L35 75 Z" fill="#4F46E5"/>
</svg>
""".trimIndent()

// Replacement `d` strings for id="icon" — recognisable media glyphs (pause is two subpaths).
private val ICON_PATHS: Map<String, String> = mapOf(
    "play" to "M35 25 L75 50 L35 75 Z",
    "pause" to "M35 25 H47 V75 H35 Z M53 25 H65 V75 H53 Z",
    "stop" to "M30 30 H70 V70 H30 Z",
)

private val ICON_LABELS: List<Pair<String, String>> =
    listOf("play" to "▶ Play", "pause" to "❚❚ Pause", "stop" to "■ Stop")

// Builds a closed radial polygon `d` (M + (count-1)·L + Z) around (cx,cy). Varying only the
// per-vertex radius while keeping count/order fixed yields two structurally-identical paths, so
// they morph cleanly. A 10-vertex star (alternating radii) and a 10-vertex ring share this shape.
private fun radialPath(cx: Float, cy: Float, count: Int, radiusAt: (Int) -> Float): String {
    val sb = StringBuilder()
    for (i in 0 until count) {
        val a = (-90f + i * 360f / count).toDouble() * PI / 180.0
        val r = radiusAt(i)
        val x = cx + r * cos(a).toFloat()
        val y = cy + r * sin(a).toFloat()
        sb.append(if (i == 0) "M" else "L").append(' ').append(x).append(' ').append(y).append(' ')
    }
    sb.append("Z")
    return sb.toString()
}

// Morph endpoints: a spiky 5-point star ⇄ a near-round 10-gon (the "ring"). Same command count.
private val MORPH_STAR_D: String = radialPath(50f, 50f, 10) { if (it % 2 == 0) 42f else 17f }
private val MORPH_POLYGON_D: String = radialPath(50f, 50f, 10) { 38f }

// One addressable <path id="blob"> on a dark disc; its `d` starts as the star and tweens toward
// MORPH_POLYGON_D via the override's morphProgress.
private val MORPH_SVG: String = """
<svg width="100" height="100" viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
  <circle cx="50" cy="50" r="48" fill="#0B1220"/>
  <path id="blob" d="$MORPH_STAR_D" fill="#38BDF8"/>
</svg>
""".trimIndent()
