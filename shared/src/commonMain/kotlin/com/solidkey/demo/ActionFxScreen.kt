@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.fx.OGLightningView
import com.solidkey.painpoints.fx.OGLightnings
import com.solidkey.painpoints.fx.OGSlashView
import com.solidkey.painpoints.fx.OGSlashes
import com.solidkey.painpoints.fx.OGSpeedLinesView
import com.solidkey.painpoints.fx.OGSpeedLines
import com.solidkey.painpoints.fx.ogAfterImage
import com.solidkey.painpoints.fx.ogBloom
import com.solidkey.painpoints.fx.ogGlow
import com.solidkey.painpoints.particle.OGParticleView
import com.solidkey.painpoints.particle.OGParticles
import com.solidkey.painpoints.shape.OGParametricSpec

private val DARK = Color(0xFF07060D)

/**
 * ⚡ **Action FX pack** — the stylised motion-FX family from `com.solidkey.painpoints.fx`, dogfooded:
 * forked lightning (`OGLightningView`), manga speed/impact lines (`OGSpeedLinesView`), elemental
 * particle presets, the glow/after-image modifiers, and a composed "action beat" assembled from
 * several of them on one stage. Each effect is plain serializable data; all pure, 60fps, same on
 * Android & iOS.
 */
@Composable
fun ActionFxScreen() {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Action FX pack", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Stylised motion effects — breathing slashes, forked lightning, impact lines, elemental " +
                "particles, plus glow & after-image modifiers. Each is data-defined and composes on one " +
                "stage. Pure, 60fps, identical on Android & iOS.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        LightningSection()
        SpeedLinesSection()
        ElementalSection()
        ModifiersSection()
        BloomSection()
        ComposedSection()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LightningSection() {
    val names = remember { OGLightnings.presets.keys.toList() }
    var name by remember { mutableStateOf(names.first()) }
    var replay by remember { mutableStateOf(0) }
    val spec = remember(name) { OGLightnings.preset(name)!! }
    SectionTitle("⚡ Lightning · OGLightningView")
    Chips(names, name) { name = it; replay++ }
    Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(DARK), contentAlignment = Alignment.Center) {
        key(replay, name) { OGLightningView(spec, Modifier.fillMaxWidth().aspectRatio(1.2f)) }
    }
    Button(onClick = { replay++ }) { Text("↻ Re-strike") }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedLinesSection() {
    val names = remember { OGSpeedLines.presets.keys.toList() }
    var name by remember { mutableStateOf(names.first()) }
    val spec = remember(name) { OGSpeedLines.preset(name)!! }
    // FOCUS/MOTION read better on a light stage; IMPACT (black lines) on light too.
    val bg = if (name == "focus") DARK else Color(0xFFF2EFE9)
    SectionTitle("💥 Speed / impact lines · OGSpeedLinesView")
    Chips(names, name) { name = it }
    Box(Modifier.fillMaxWidth().aspectRatio(1.3f).background(bg), contentAlignment = Alignment.Center) {
        OGSpeedLinesView(spec, Modifier.fillMaxWidth().aspectRatio(1.3f))
        Text("subject", color = if (name == "focus") Color.White else Color(0xFF333333), fontSize = 14.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ElementalSection() {
    val names = remember { listOf("petals", "embers", "droplets", "leaves") }
    var name by remember { mutableStateOf(names.first()) }
    val spec = remember(name) { OGParticles.preset(name)!! }
    SectionTitle("🌸 Elemental particles · OGParticles presets")
    Chips(names, name) { name = it }
    Box(Modifier.fillMaxWidth().aspectRatio(1.2f).background(DARK), contentAlignment = Alignment.Center) {
        key(name) { OGParticleView(spec, Modifier.fillMaxWidth().aspectRatio(1.2f)) }
    }
}

@Composable
private fun ModifiersSection() {
    var glow by remember { mutableStateOf(true) }
    var after by remember { mutableStateOf(true) }
    val star = remember { OGParametricSpec(kind = "star", count = 5, innerRatio = 0.45f).toShape(0.1f) }
    SectionTitle("✨ Modifiers · Modifier.ogGlow / Modifier.ogAfterImage")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(glow, { glow = !glow }, { Text("Glow") })
        FilterChip(after, { after = !after }, { Text("After-image") })
    }
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).background(DARK), contentAlignment = Alignment.Center) {
        var m: Modifier = Modifier.size(90.dp)
        if (after) m = m.ogAfterImage(count = 6, step = Offset(-22f, 0f), maxAlpha = 0.45f)
        if (glow) m = m.ogGlow(color = Color(0xFF64FFDA), radius = 1.5f, intensity = 0.7f)
        Box(m.clip(star).background(Color(0xFF00BFA5)))
    }
    Text(
        "One star shape: ogGlow adds the additive halo; ogAfterImage draws fading trailing copies " +
            "(supply your motion trail). Both capture the content once per frame and re-draw it.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloomSection() {
    val radii = remember { listOf(14, 26, 40) }
    var radius by remember { mutableStateOf(26) }
    val hot = Color(0xFFFFE082) // warm gold — blooms vividly on the dark stage
    val star = remember { OGParametricSpec(kind = "star", count = 6, innerRatio = 0.5f).toShape(0.12f) }
    SectionTitle("🌟 Gaussian bloom · Modifier.ogBloom (opt-in)")
    Text(
        "Same star, left raw vs right through Modifier.ogBloom — a real platform gaussian blur adds a soft " +
            "luminous halo. Drag the radius.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        radii.forEach { r -> FilterChip(radius == r, { radius = r }, { Text("${r}dp") }) }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Raw star — no effect.
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DARK), contentAlignment = Alignment.Center) {
                Box(Modifier.size(64.dp).clip(star).background(hot))
            }
            Spacer(Modifier.height(4.dp))
            Text("raw", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
        // Same star through ogBloom — the gaussian node is larger than the star so the halo has room to spread.
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).background(DARK), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(150.dp).ogBloom(radius = radius.dp, intensity = 1f, color = hot),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(64.dp).clip(star).background(hot))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("ogBloom · gaussian", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "ogBloom runs the content through a REAL platform gaussian blur (Android RenderEffect / iOS Skia), " +
            "so the halo is smooth, not stacked scaled copies like ogGlow. Opt-in: needs Android 31+ (no-ops " +
            "below) and isn't pixel-identical across platforms — use ogGlow for the zero-dep frame-identical halo.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
}

@Composable
private fun ComposedSection() {
    var replay by remember { mutableStateOf(0) }
    SectionTitle("🎬 Composed 'action beat' (slash + impact lines + droplets)")
    Box(Modifier.fillMaxWidth().aspectRatio(1.3f).background(DARK), contentAlignment = Alignment.Center) {
        key(replay) {
            OGSpeedLinesView(OGSpeedLines.FOCUS, Modifier.fillMaxWidth().aspectRatio(1.3f))
            OGSlashView(OGSlashes.WATER, Modifier.fillMaxWidth().aspectRatio(1.3f))
            OGParticleView(OGParticles.DROPLETS, Modifier.fillMaxWidth().aspectRatio(1.3f))
        }
    }
    Button(onClick = { replay++ }) { Text("↻ Replay beat") }
    Spacer(Modifier.height(8.dp))
    Text(
        "Three effects on one stage — the dev assembles an anime action beat from a small vocabulary of " +
            "additive renderers. No engine, no shaders, no new dependency.",
        fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
}

@Composable
private fun SectionTitle(t: String) {
    Spacer(Modifier.height(4.dp))
    Text(t, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(names: List<String>, selected: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEach { n ->
            FilterChip(selected == n, { onPick(n) }, { Text(n.replaceFirstChar { it.uppercase() }) })
        }
    }
}
