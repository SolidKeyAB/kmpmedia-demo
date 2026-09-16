package com.solidkey.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solidkey.painpoints.image.OGImageView
import com.solidkey.painpoints.image.loading.OGImageFormat
import com.solidkey.painpoints.image.loading.OGImageResourceFileType
import com.solidkey.painpoints.image.loading.OGImageUrlType
import com.solidkey.painpoints.image.loading.OGSvgResourceFileType
import com.solidkey.painpoints.image.loading.OGSvgUrlType
import com.solidkey.painpoints.image.svg.OGSVGView
import com.solidkey.painpoints.image.svg.SVGScalingBehavior
import com.solidkey.painpoints.source.OGSourceType

/**
 * Feeds the library deliberately broken inputs and shows that onError fires
 * (rather than crashing or silently swallowing the failure).
 */
@Composable
fun EdgeCasesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Each row feeds a deliberately bad input to the library. A healthy library reports the failure through onError instead of crashing. (Network cases also fail offline — that still exercises the error path.)",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        SectionLabel("Images")
        ImageCase("Empty URL", "Blank source string.") { OGImageUrlType("") }
        ImageCase("Unreachable host", "DNS resolution fails.") { OGImageUrlType("https://no.such.host.invalid/x.png") }
        ImageCase("404 on a real host", "Host resolves, file is missing.") {
            OGImageUrlType("https://raw.githubusercontent.com/SolidKeyAB/kmpmedia/main/__nope__.png")
        }
        ImageCase("HTML served as an image", "Body isn't a decodable image.") { OGImageUrlType("https://example.com/") }
        ImageCase("Missing local resource", "Resource name that isn't bundled.") {
            OGImageResourceFileType("does_not_exist_123", OGImageFormat.PNG)
        }

        SectionLabel("SVG")
        SvgCase("Malformed SVG (HTML body)", "Parser gets non-SVG markup.") { OGSvgUrlType("https://example.com/") }
        SvgCase("Missing SVG resource", "Resource name that isn't bundled.") { OGSvgResourceFileType("does_not_exist_123") }
        SvgCase("Control: valid SVG", "Should load cleanly (needs network).") {
            OGSvgUrlType("https://dev.w3.org/SVG/tools/svgweb/samples/svg-files/atom.svg")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ImageCase(title: String, desc: String, makeSource: () -> OGSourceType) {
    val source = remember { makeSource() }
    var error by remember { mutableStateOf<String?>(null) }
    CaseCard(title, desc, error) {
        OGImageView(
            source = source,
            modifier = Modifier.fillMaxSize(),
            onError = { error = it },
            onEventTriggered = { _, _ -> }
        )
    }
}

@Composable
private fun SvgCase(title: String, desc: String, makeSource: () -> OGSourceType) {
    val source = remember { makeSource() }
    var error by remember { mutableStateOf<String?>(null) }
    CaseCard(title, desc, error) {
        OGSVGView(
            source = source,
            width = 200f,
            height = 200f,
            modifier = Modifier.fillMaxSize(),
            scalingBehavior = SVGScalingBehavior.FIT,
            onError = { error = it }
        )
    }
}

@Composable
private fun CaseCard(title: String, desc: String, error: String?, preview: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(84.dp)
                    .background(Color(0xFFEeEeEe))
            ) { preview() }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                Spacer(Modifier.height(4.dp))
                if (error != null) {
                    Text("⚠️ onError: $error", fontSize = 12.sp, color = Color(0xFFC62828))
                } else {
                    Text("… no error reported (loading or loaded)", fontSize = 12.sp, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}
