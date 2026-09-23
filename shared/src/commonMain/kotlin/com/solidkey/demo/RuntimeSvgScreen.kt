package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
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
import kotlin.math.roundToInt

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
        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
