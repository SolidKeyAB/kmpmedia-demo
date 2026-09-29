package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.ai.OGAiVector
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.image.loading.OGSvgFileType
import com.solidkey.painpoints.image.loading.seedSvgFile
import com.solidkey.painpoints.image.svg.OGSVGView

/**
 * 🤖 AI vector — "describe → shape / patch." KMPMedia's vector primitives are just data, so a
 * language model can produce them. This screen dogfoods [OGAiVector]: it decodes a **model's
 * reply** (canned examples here, or paste your own) into a live polygon lasso clipping a photo,
 * and into an SVG runtime patch driving a gauge. The library makes no network call and bundles no
 * AI SDK — the app owns the model and the call; the library only defines the JSON contract
 * (`polygonPrompt`/`svgPatchPrompt`) and turns the reply back into primitives (`decodePolygon`/
 * `decodeSvgPatch`), tolerating the code fences and prose models add.
 */

private data class VectorExample(val label: String, val instruction: String, val json: String)

// "Replies" a model would return for each instruction. One in each group is deliberately wrapped
// in a ```json fence with prose, to show OGAiVector.extractJson coping with real model output.
private val POLYGON_EXAMPLES = listOf(
    VectorExample(
        "★ star", "the outline of a five-pointed star",
        """
        Sure — here is a five-pointed star lasso:
        ```json
        {"points":[{"x":0.5,"y":0.04},{"x":0.612,"y":0.346},{"x":0.938,"y":0.358},
        {"x":0.681,"y":0.559},{"x":0.77,"y":0.872},{"x":0.5,"y":0.69},{"x":0.23,"y":0.872},
        {"x":0.319,"y":0.559},{"x":0.062,"y":0.358},{"x":0.388,"y":0.346}]}
        ```
        """.trimIndent()
    ),
    VectorExample(
        "⚡ bolt", "a lightning bolt",
        """{"points":[{"x":0.50,"y":0.02},{"x":0.25,"y":0.50},{"x":0.44,"y":0.50},{"x":0.30,"y":0.98},{"x":0.75,"y":0.42},{"x":0.55,"y":0.42},{"x":0.66,"y":0.02}]}"""
    ),
    VectorExample(
        "◆ diamond", "a diamond",
        """{"points":[{"x":0.5,"y":0.05},{"x":0.95,"y":0.5},{"x":0.5,"y":0.95},{"x":0.05,"y":0.5}]}"""
    ),
)

private val PATCH_EXAMPLES = listOf(
    VectorExample(
        "amber 80%", "point the needle to 80% and turn the arc + dot amber",
        """
        Here's the patch:
        ```json
        {"overrides":{"needle":{"rotation":54,"rotationCx":100,"rotationCy":105},
        "arc":{"stroke":"#F59E0B"},"status":{"fill":"#F59E0B"}}}
        ```
        """.trimIndent()
    ),
    VectorExample(
        "redline", "redline it — needle to max and everything red",
        """{"overrides":{"needle":{"rotation":90,"rotationCx":100,"rotationCy":105,"fill":"#EF4444"},"arc":{"stroke":"#EF4444"},"status":{"fill":"#EF4444"}}}"""
    ),
    VectorExample(
        "cool 30%", "calm blue at 30%",
        """{"overrides":{"needle":{"rotation":-36,"rotationCx":100,"rotationCy":105},"arc":{"stroke":"#3B82F6"},"status":{"fill":"#3B82F6"}}}"""
    ),
)

// A gauge with three addressable nodes (id="arc" / "needle" / "status"), same as the Runtime-SVG
// screen — so a model patch keyed by those ids drives it live.
private val AI_GAUGE_SVG: String = """
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

@Composable
fun AiVectorScreen() {
    var gaugePath by remember { mutableStateOf<String?>(null) }
    seedSvgFile("ai_gauge.svg", AI_GAUGE_SVG) { gaugePath = it }

    var showPrompt by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "AI vector — describe → shape / patch",
            fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Text(
            "The lasso and the SVG patch are just data, so a model can produce them. Below, a model's " +
                "reply (canned — or paste your own) is decoded by OGAiVector into a live clip shape and a " +
                "live SVG patch. No network or AI SDK is in the library: it defines the JSON contract and " +
                "turns the reply back into primitives, tolerating the code fences models add.",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        FilterChip(
            selected = showPrompt,
            onClick = { showPrompt = !showPrompt },
            label = { Text(if (showPrompt) "Hide the prompt sent to the model" else "Show the prompt sent to the model") }
        )

        // ── describe → clip region ────────────────────────────────────────────────
        SectionHeader("1 · describe → clip region")
        VectorPlayground(
            examples = POLYGON_EXAMPLES,
            showPrompt = showPrompt,
            promptFor = { OGAiVector.polygonPrompt(it) },
        ) { json ->
            val shape = OGAiVector.decodePolygonOrNull(json)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (shape != null) {
                    OGImageView(
                        source = OGImageResourceFileType("sample_portrait", OGImageFormat.JPEG),
                        clipShape = shape,                       // ← the model's lasso, live
                        contentScale = ContentScale.Crop,
                        alignment = BiasAlignment(0f, 0f),
                        modifier = Modifier.size(240.dp),
                        onError = { },
                        onEventTriggered = { _, _ -> },
                    )
                } else {
                    ParseError()
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── describe → SVG patch ──────────────────────────────────────────────────
        SectionHeader("2 · describe → SVG patch")
        VectorPlayground(
            examples = PATCH_EXAMPLES,
            showPrompt = showPrompt,
            promptFor = { OGAiVector.svgPatchPrompt(it, nodeIds = listOf("needle", "arc", "status")) },
        ) { json ->
            val overrides = OGAiVector.decodeSvgPatchOrNull(json)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val path = gaugePath
                when {
                    path == null -> Text("Preparing…", fontSize = 13.sp)
                    overrides == null -> ParseError()
                    else -> OGSVGView(
                        source = OGSvgFileType(path),
                        width = 300f, height = 195f,
                        overrides = overrides,                   // ← the model's patch, live
                        onError = { },
                    )
                }
            }
        }
    }
}

/**
 * One "describe → …" playground: preset chips that load a canned model reply into an editable
 * field, the (optional) prompt OGAiVector would hand the model, and a live [preview] of whatever
 * the current JSON decodes to.
 */
@Composable
private fun VectorPlayground(
    examples: List<VectorExample>,
    showPrompt: Boolean,
    promptFor: (String) -> String,
    preview: @Composable (json: String) -> Unit,
) {
    var selected by remember { mutableStateOf(0) }
    var json by remember { mutableStateOf(examples[0].json) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        examples.forEachIndexed { i, ex ->
            FilterChip(
                selected = selected == i,
                onClick = { selected = i; json = ex.json },
                label = { Text(ex.label) },
            )
        }
    }
    Text(
        "prompt: \"${examples[selected].instruction}\"",
        fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    )

    if (showPrompt) {
        Mono(promptFor(examples[selected].instruction))
    }

    preview(json)

    Text("model reply (editable — paste your own):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    OutlinedTextField(
        value = json,
        onValueChange = { json = it },
        modifier = Modifier.fillMaxWidth().height(150.dp),
        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
    )
}

@Composable
private fun SectionHeader(text: String) =
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

@Composable
private fun ParseError() = Text(
    "Couldn't parse a shape/patch from that reply. Fix the JSON (decodeOrNull returned null).",
    fontSize = 13.sp, color = MaterialTheme.colorScheme.error
)

@Composable
private fun Mono(text: String) = Text(
    text,
    fontSize = 10.sp, fontFamily = FontFamily.Monospace,
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
)
