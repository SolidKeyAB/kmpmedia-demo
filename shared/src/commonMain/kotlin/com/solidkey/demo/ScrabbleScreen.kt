package com.solidkey.demo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.solidkey.painpoints.audio.playing.OGAudioClip
import com.solidkey.painpoints.audio.playing.OGAudioSprite
import com.solidkey.painpoints.gesture.OGHitArea
import com.solidkey.painpoints.gesture.OGInteractionConfig
import com.solidkey.painpoints.gesture.ogInteractive
import com.solidkey.painpoints.gesture.rememberOGInteractionState
import com.solidkey.painpoints.source.OGSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

// ── The board ────────────────────────────────────────────────────────────────────────────────────
private const val N = 15                       // a real Scrabble board is 15×15
private const val TILE_FRACTION = 0.9f          // a tile fills 90% of its cell

// A letter tile. Standard Scrabble letter values — enough to spell the demo word below.
private data class LetterTile(val id: Int, val letter: Char, val value: Int)

// The rack spells the library's own name — drag K·M·P·M·E·D·I·A onto the board.
private val RACK: List<LetterTile> =
    listOf('K' to 5, 'M' to 3, 'P' to 3, 'M' to 3, 'E' to 1, 'D' to 2, 'I' to 1, 'A' to 1)
        .mapIndexed { i, (c, v) -> LetterTile(i, c, v) }

// Standard premium-square layout (row to col). Used only to paint the board so it reads as Scrabble.
private val TW = setOf(0 to 0, 0 to 7, 0 to 14, 7 to 0, 7 to 14, 14 to 0, 14 to 7, 14 to 14)
private val DW = setOf(
    1 to 1, 2 to 2, 3 to 3, 4 to 4, 10 to 10, 11 to 11, 12 to 12, 13 to 13,
    1 to 13, 2 to 12, 3 to 11, 4 to 10, 10 to 4, 11 to 3, 12 to 2, 13 to 1,
)
private val TL = setOf(
    1 to 5, 1 to 9, 5 to 1, 5 to 5, 5 to 9, 5 to 13,
    9 to 1, 9 to 5, 9 to 9, 9 to 13, 13 to 5, 13 to 9,
)
private val DL = setOf(
    0 to 3, 0 to 11, 2 to 6, 2 to 8, 3 to 0, 3 to 7, 3 to 14, 6 to 2, 6 to 6, 6 to 8, 6 to 12,
    7 to 3, 7 to 11, 8 to 2, 8 to 6, 8 to 8, 8 to 12, 11 to 0, 11 to 7, 11 to 14, 12 to 6, 12 to 8,
    14 to 3, 14 to 11,
)

private val CBoard = Color(0xFF14303B)      // deep teal frame behind the cells
private val CCell = Color(0xFF1C4350)       // base cell
private val CTW = Color(0xFFE05A47)         // triple word
private val CDW = Color(0xFFEC9AA0)         // double word
private val CTL = Color(0xFF2F80C9)         // triple letter
private val CDL = Color(0xFF7FC3E0)         // double letter
private val CGrid = Color(0xFF0E2630)
private val CHover = Color(0xFFFFD93D)      // the cell your tile will drop into
private val CTileFace = Color(0xFFF2D9A6)   // warm bone-coloured tile
private val CTileEdge = Color(0xFFCBA86A)
private val CTileInk = Color(0xFF3A2E17)

/**
 * **Scrabble tiles** — a drag-and-drop proof that KMPMedia's shipped interaction + audio primitives
 * are enough to build real tile gameplay, with ZERO library change. Drag a lettered tile from the
 * rack onto the 15×15 board: the live drag, momentum and spring are all one `Modifier.ogInteractive`;
 * the cell under the tile highlights as you move; on release the tile **snaps into the grid** and a
 * click plays from an `OGAudioSprite` slice. Drop it back on the rack to return it. Everything here
 * is plain commonMain Compose + the library — identical on Android & iOS.
 *
 * The library gives you: the gesture (ogInteractive / OGInteractionState / OGHitArea) and the sound
 * (OGAudioSprite / OGAudioClip). The *rules* — a 15×15 grid, cell occupancy, snap-to-cell — are the
 * ~30 lines of game logic you write yourself. That split is exactly the answer to "is KMPMedia a game
 * engine": it is the media + interaction layer; the rules stay yours.
 */
@Composable
fun ScrabbleScreen() {
    val scope = rememberCoroutineScope()

    // One tiny sfx.mp3 already ships with the demo; reuse two of its slices as place / reject clicks.
    val sfx = OGAudioSprite.create()
    DisposableEffect(Unit) {
        sfx.load(
            source = OGSource.Resource("sfx"),
            clips = listOf(
                OGAudioClip("place", startMs = 0, endMs = 280),   // bright chime = tile locks in
                OGAudioClip("reject", startMs = 400, endMs = 750), // low thud = bounced back
            ),
            onError = { },
        )
        onDispose { sfx.release() }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Scrabble tiles", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "Drag the lettered tiles from the rack onto the board — they snap into the grid and " +
                "click as they land. Spell K·M·P·M·E·D·I·A across the centre ★. Every bit of the " +
                "drag is one Modifier.ogInteractive; the click is an OGAudioSprite slice. The board, " +
                "the snapping and the occupancy rules are plain app code.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val boardDp = maxWidth
            val boardPx = with(density) { boardDp.toPx() }
            val cellPx = boardPx / N
            val tilePx = cellPx * TILE_FRACTION
            val gapPx = cellPx * 0.45f
            val rackPx = tilePx * 1.35f
            val fieldPx = boardPx + gapPx + rackPx

            // Rack home positions (px, measured from the field's top-left) — centred under the board.
            val spacingPx = (boardPx - RACK.size * tilePx) / (RACK.size + 1)
            val homes = remember(boardPx) {
                List(RACK.size) { i ->
                    Offset(
                        x = spacingPx + i * (tilePx + spacingPx),
                        y = boardPx + gapPx + (rackPx - tilePx) / 2f,
                    )
                }
            }

            // Mutable game state, re-seeded if the width changes (e.g. rotation).
            val anchors = remember(boardPx) {
                mutableStateMapOf<Int, Offset>().apply { homes.forEachIndexed { i, o -> put(i, o) } }
            }
            val occupant = remember(boardPx) { mutableStateMapOf<Int, Int>() } // cellIndex -> tileId
            val placedAt = remember(boardPx) { mutableStateMapOf<Int, Int>() }  // tileId -> cellIndex
            var hoverCell by remember(boardPx) { mutableStateOf<Int?>(null) }
            var draggingId by remember(boardPx) { mutableStateOf<Int?>(null) }

            fun cellAt(px: Offset): Int? {
                if (px.x < 0f || px.y < 0f || px.x >= boardPx || px.y >= boardPx) return null
                val col = (px.x / cellPx).toInt().coerceIn(0, N - 1)
                val row = (px.y / cellPx).toInt().coerceIn(0, N - 1)
                return row * N + col
            }

            fun cellTopLeft(idx: Int): Offset {
                val col = idx % N
                val row = idx / N
                val inset = (cellPx - tilePx) / 2f
                return Offset(col * cellPx + inset, row * cellPx + inset)
            }

            Box(modifier = Modifier.fillMaxWidth().height(with(density) { fieldPx.toDp() })) {

                // ── The board (one Canvas; repaints only when hoverCell changes) ──────────────────
                Canvas(modifier = Modifier.size(boardDp)) {
                    drawRect(CBoard, size = Size(boardPx, boardPx))
                    for (row in 0 until N) for (col in 0 until N) {
                        val rc = row to col
                        val color = when {
                            rc in TW -> CTW
                            row == 7 && col == 7 -> CDW
                            rc in DW -> CDW
                            rc in TL -> CTL
                            rc in DL -> CDL
                            else -> CCell
                        }
                        val pad = cellPx * 0.06f
                        drawRect(
                            color = color,
                            topLeft = Offset(col * cellPx + pad, row * cellPx + pad),
                            size = Size(cellPx - 2 * pad, cellPx - 2 * pad),
                        )
                    }
                    // Centre star.
                    drawPath(
                        path = star(Offset(7.5f * cellPx, 7.5f * cellPx), cellPx * 0.42f, cellPx * 0.17f),
                        color = Color(0xFF8A2B33),
                    )
                    // Grid lines.
                    for (i in 0..N) {
                        drawLine(CGrid, Offset(i * cellPx, 0f), Offset(i * cellPx, boardPx), 1f)
                        drawLine(CGrid, Offset(0f, i * cellPx), Offset(boardPx, i * cellPx), 1f)
                    }
                    // Hover highlight.
                    hoverCell?.let { idx ->
                        val col = idx % N
                        val row = idx / N
                        drawRect(
                            color = CHover,
                            topLeft = Offset(col * cellPx, row * cellPx),
                            size = Size(cellPx, cellPx),
                            style = Stroke(width = cellPx * 0.12f),
                        )
                    }
                }

                // ── Rack strip ────────────────────────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (boardPx + gapPx * 0.3f).roundToInt()) }
                        .size(boardDp, with(density) { (rackPx + gapPx * 0.7f).toDp() })
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A1C0E)),
                )

                // ── Tiles ───────────────────────────────────────────────────────────────────────
                val tileDp = with(density) { tilePx.toDp() }
                val letterSp = with(density) { (tilePx * 0.5f).toSp() }
                val valueSp = with(density) { (tilePx * 0.26f).toSp() }

                RACK.forEach { tile ->
                    val state = rememberOGInteractionState(
                        remember { OGInteractionConfig.DragOnly.copy(fling = false) },
                    )

                    Box(
                        modifier = Modifier
                            .offset {
                                val a = anchors[tile.id] ?: Offset.Zero
                                IntOffset(a.x.roundToInt(), a.y.roundToInt())
                            }
                            .zIndex(if (draggingId == tile.id) 2f else if (tile.id in placedAt) 1f else 0f)
                            .size(tileDp)
                            // The drag itself — library primitive, GPU-layer transform, 60fps.
                            .ogInteractive(state, hitArea = OGHitArea.RECT)
                            // A thin parallel detector just to learn WHEN the drag ends and to light
                            // the hovered cell. It never consumes events, so ogInteractive still drags.
                            .pointerInput(tile.id, boardPx) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    draggingId = tile.id
                                    var pressed = true
                                    while (pressed) {
                                        val e = awaitPointerEvent(PointerEventPass.Final)
                                        val a = anchors[tile.id] ?: Offset.Zero
                                        val centre = a + state.offset.value +
                                            Offset(tilePx / 2f, tilePx / 2f)
                                        hoverCell = cellAt(centre)
                                        if (e.changes.none { it.pressed }) pressed = false
                                    }
                                    // ---- DROP ----
                                    val a = anchors[tile.id] ?: Offset.Zero
                                    val curPx = a + state.offset.value
                                    val centre = curPx + Offset(tilePx / 2f, tilePx / 2f)
                                    val target = cellAt(centre)
                                    val free = target != null &&
                                        (occupant[target] == null || occupant[target] == tile.id)
                                    when {
                                        free -> {
                                            placedAt.remove(tile.id)?.let { occupant.remove(it) }
                                            val dest = cellTopLeft(target!!)
                                            anchors[tile.id] = dest
                                            occupant[target] = tile.id
                                            placedAt[tile.id] = target
                                            scope.launch {
                                                state.offset.snapTo(curPx - dest)
                                                state.offset.animateTo(
                                                    Offset.Zero,
                                                    spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                                                )
                                            }
                                            sfx.play("place")
                                        }
                                        // Dropped off the board (in the rack zone) → send it home.
                                        target == null && centre.y > boardPx -> {
                                            placedAt.remove(tile.id)?.let { occupant.remove(it) }
                                            val home = homes[tile.id]
                                            anchors[tile.id] = home
                                            scope.launch {
                                                state.offset.snapTo(curPx - home)
                                                state.offset.animateTo(
                                                    Offset.Zero,
                                                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium),
                                                )
                                            }
                                            sfx.play("reject")
                                        }
                                        // Invalid cell (occupied / off-grid) → bounce back where it was.
                                        else -> {
                                            scope.launch {
                                                state.offset.animateTo(
                                                    Offset.Zero,
                                                    spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium),
                                                )
                                            }
                                            sfx.play("reject")
                                        }
                                    }
                                    hoverCell = null
                                    draggingId = null
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        TileFace(tile, tileDp, letterSp, valueSp)
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))
        Text(
            "Under the hood: Modifier.ogInteractive applies the drag through one graphicsLayer with a " +
                "spring settle (com.solidkey.painpoints.gesture); OGAudioSprite fires a [start,end] " +
                "slice of a single sfx.mp3 on each drop (com.solidkey.painpoints.audio). Both ship in " +
                "kmpmedia-lib 1.15.0 and run the same on Android & iOS. The grid + snap-to-cell + " +
                "occupancy are ~30 lines of plain Compose state in this screen.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun TileFace(
    tile: LetterTile,
    tileDp: androidx.compose.ui.unit.Dp,
    letterSp: androidx.compose.ui.unit.TextUnit,
    valueSp: androidx.compose.ui.unit.TextUnit,
) {
    Box(
        modifier = Modifier
            .size(tileDp)
            .clip(RoundedCornerShape(tileDp * 0.16f))
            .background(CTileEdge)
            .padding(tileDp * 0.06f)
            .clip(RoundedCornerShape(tileDp * 0.12f))
            .background(CTileFace),
        contentAlignment = Alignment.Center,
    ) {
        Text(tile.letter.toString(), fontSize = letterSp, fontWeight = FontWeight.Bold, color = CTileInk)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Text(
                tile.value.toString(),
                fontSize = valueSp,
                fontWeight = FontWeight.SemiBold,
                color = CTileInk.copy(alpha = 0.8f),
                modifier = Modifier.padding(end = tileDp * 0.1f, bottom = tileDp * 0.04f),
            )
        }
    }
}

/** A 5-point star path centred at [c] — used for the board's centre square. */
private fun star(c: Offset, outer: Float, inner: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = c.x + (r * cos(a)).toFloat()
        val y = c.y + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
