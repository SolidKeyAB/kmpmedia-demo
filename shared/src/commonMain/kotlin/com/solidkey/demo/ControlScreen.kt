package com.solidkey.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.control.OGControl
import com.solidkey.painpoints.control.OGControlSpec
import com.solidkey.painpoints.control.OGControls

/**
 * 🔘 OGControl — the whole hand-drawn button in one call: a fill, a cached roughened ink outline, a
 * tap + pressed-state (via ogButton), an optional seasonal garnish and a styled vector label. Dogfoods
 * com.solidkey.painpoints.control: a control is *configured* (an OGControlSpec), not hand-assembled.
 */
@Composable
fun ControlScreen() {
    var enabled by remember { mutableStateOf(true) }
    var taps by remember { mutableIntStateOf(0) }

    val accents = listOf(Color(0xFF54A69B), Color(0xFFE5733B), Color(0xFF7E57C2), Color(0xFF4E7FE0))
    val presets = remember { OGControls.presets.toList() } // plain / sketch / slime / snow

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionTitle("🔘 One-call controls · OGControl + OGControlSpec")
        Caption(
            "Each button below is a single OGControl — the fill, the roughened ink edge, the press effect, " +
                "the garnish and the vectorized label are all one skinnable spec. The label + edge are cached, " +
                "so only a moving garnish redraws: 60fps, identical on Android & iOS.",
        )

        presets.forEachIndexed { i, (name, spec) ->
            OGControl(
                label = "$name  ·  tap me",
                onClick = { taps++ },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = accents[i % accents.size],
                enabled = enabled,
                seed = i + 1,
                spec = spec,
            )
        }

        Text(
            "taps: $taps",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(enabled, { enabled = it })
            Text(
                if (enabled) "  enabled" else "  disabled (fill dims, label halves, ink fades, tap is a no-op)",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }

        Caption(
            "The live colours + the click stay runtime arguments; corner radius, press effect, ink edge, " +
                "gradient and garnish live in the OGControlSpec and serialize to a .control JSON a model can " +
                "author (OGControls.controlPrompt). Presets: plain / sketch / slime / snow.",
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun Caption(text: String) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
}
