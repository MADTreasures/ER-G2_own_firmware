package ch.madtreasures.g2watch.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import ch.madtreasures.g2watch.desktop.DesktopFrame
import ch.madtreasures.g2watch.desktop.PointerMotion
import ch.madtreasures.g2watch.desktop.PointerSprite
import ch.madtreasures.g2watch.glasses.GlassesState
import ch.madtreasures.g2watch.glasses.Stage
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The desktop as the glasses show it, and the whole watch face as a relative touchpad: only
 * finger *movement* moves the pointer, so putting the finger down elsewhere never makes it jump.
 * A double tap anywhere is a click at the pointer, holding the finger still opens the menu, the
 * crown sets the pointer speed. Without glasses this is the whole app: the preview shows exactly
 * what would be sent.
 */
@Composable
fun TouchpadScreen(
    frame: DesktopFrame,
    glasses: GlassesState,
    speed: Float,
    onMove: (dx: Float, dy: Float) -> Unit,
    onSpeed: (Float) -> Unit,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    // pointerInput(Unit) lives as long as the screen; always use the latest values.
    val currentSpeed by rememberUpdatedState(speed)
    val move by rememberUpdatedState(onMove)
    val setSpeed by rememberUpdatedState(onSpeed)
    val click by rememberUpdatedState(onClick)
    val openMenu by rememberUpdatedState(onOpenMenu)
    var lastTapAt by remember { mutableLongStateOf(-10_000L) }
    var speedShownAt by remember { mutableLongStateOf(0L) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(speedShownAt) {
        if (speedShownAt != 0L) {
            delay(1_500)
            speedShownAt = 0L
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onRotaryScrollEvent { event ->
                setSpeed(currentSpeed + event.verticalScrollPixels / 500f)
                speedShownAt = System.currentTimeMillis()
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                relativeTouchpad(
                    onMove = { dx, dy, dtMs ->
                        val (gx, gy) = PointerMotion.toGlasses(dx / density, dy / density, dtMs, currentSpeed)
                        move(gx, gy)
                    },
                    onTap = { at ->
                        if (at - lastTapAt <= DOUBLE_TAP_MS) {
                            lastTapAt = -10_000L
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            click()
                        } else {
                            lastTapAt = at
                        }
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        openMenu()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val status = when {
                glasses.stage == Stage.IDLE -> "Vorschau – ohne Brille"
                glasses.stage == Stage.CONNECTED -> "Brille" + (glasses.battery?.let { " $it %" } ?: "") +
                    if (glasses.charging) " ⚡" else ""
                else -> glasses.stage.label
            }
            Text(status, fontSize = 12.sp, color = stageColor(glasses.stage), maxLines = 1)
            DesktopPreview(frame, Modifier.fillMaxWidth(PREVIEW_WIDTH))
            val hint = when {
                speedShownAt != 0L -> String.format(Locale.GERMANY, "Tempo %.1f×", speed)
                glasses.lastInput != null && glasses.stage == Stage.CONNECTED -> "Brille: ${glasses.lastInput}"
                else -> "2× tippen = Klick · Halten = Menü"
            }
            Text(
                hint,
                fontSize = 11.sp,
                color = if (speedShownAt != 0L) WarnOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * The visible band of the desktop in the glasses' green, quantised to their 16 shades, with the
 * pointer on top. The pointer is drawn at least at its real size so it stays findable.
 */
@Composable
fun DesktopPreview(frame: DesktopFrame, modifier: Modifier = Modifier) {
    val desktop = remember(frame.version) { frame.toImageBitmap() }
    val pointer = remember { pointerImageBitmap() }
    Canvas(modifier.aspectRatio(frame.band.w.toFloat() / frame.band.h)) {
        val scale = size.width / frame.band.w
        drawImage(
            desktop,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Low,
        )
        val pointerScale = maxOf(scale, 1f)
        drawImage(
            pointer,
            dstOffset = IntOffset(
                ((frame.pointerX - frame.band.x) * scale).roundToInt(),
                ((frame.pointerY - frame.band.y) * scale).roundToInt(),
            ),
            dstSize = IntSize(
                (PointerSprite.width * pointerScale).roundToInt(),
                (PointerSprite.height * pointerScale).roundToInt(),
            ),
            filterQuality = FilterQuality.None,
        )
    }
}

/** Gray value → the glasses' green, after dropping to the 4 bits the panel shows. */
private val GREEN = IntArray(256) { v ->
    val level = (v shr 4) * 17
    (0xFF shl 24) or ((0x7C * level / 255) shl 16) or ((0xFF * level / 255) shl 8) or (0xA0 * level / 255)
}

private fun DesktopFrame.toImageBitmap(): ImageBitmap {
    val argb = IntArray(band.w * band.h) { GREEN[pixels[it].toInt() and 0xFF] }
    return Bitmap.createBitmap(argb, band.w, band.h, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** The sprite with its color-key meaning: 0 transparent, 1 black, anything else lit. */
private fun pointerImageBitmap(): ImageBitmap {
    val argb = IntArray(PointerSprite.width * PointerSprite.height) {
        when (val v = PointerSprite.pixels[it].toInt() and 0xFF) {
            0 -> 0
            1 -> 0xFF000000.toInt()
            else -> GREEN[v]
        }
    }
    return Bitmap.createBitmap(argb, PointerSprite.width, PointerSprite.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Share of the watch width the preview uses; on a round face the corners of a wider one are cut. */
private const val PREVIEW_WIDTH = 0.9f

/** Finger must rest this long (without moving past the touch slop) to open the menu. */
private const val LONG_PRESS_MS = 900L

/** A touch shorter than this that stays within the touch slop is a tap. */
private const val TAP_MAX_MS = 300L

/** Two taps at most this far apart (end to end) make a double tap. */
private const val DOUBLE_TAP_MS = 400L

/**
 * Relative pointer tracking, from G2 Direct. Only deltas between successive events of the same
 * finger are reported; a new touch (or a second finger taking over) re-anchors without moving.
 * Movement within the touch slop is held back until the finger clearly moves, so a tap never
 * nudges the pointer; the held-back part is then sent along and nothing is lost.
 */
private suspend fun PointerInputScope.relativeTouchpad(
    onMove: (dx: Float, dy: Float, dtMs: Float) -> Unit,
    onTap: (upTimeMs: Long) -> Unit,
    onLongPress: () -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        down.consume()
        var tracked = down.id
        var last = down.position
        var lastTime = down.uptimeMillis
        var travelled = 0f
        var longPressed = false
        var multiFinger = false
        var heldX = 0f
        var heldY = 0f
        var heldMs = 0f
        var upTime = down.uptimeMillis
        while (true) {
            val waitingForLongPress = !longPressed && travelled <= slop
            val event = if (waitingForLongPress) {
                val remaining = LONG_PRESS_MS - (lastTime - down.uptimeMillis)
                withTimeoutOrNull(remaining.coerceAtLeast(1L)) { awaitPointerEvent() }
            } else {
                awaitPointerEvent()
            }
            if (event == null) {
                longPressed = true
                onLongPress()
                continue
            }
            val change = event.changes.firstOrNull { it.id == tracked }
            if (change == null || !change.pressed) {
                val other = event.changes.firstOrNull { it.pressed }
                if (other == null) {
                    upTime = change?.uptimeMillis ?: lastTime
                    break
                }
                // Another finger is still down: continue with it, re-anchored (no jump).
                multiFinger = true
                tracked = other.id
                last = other.position
                lastTime = other.uptimeMillis
                event.changes.forEach { it.consume() }
                continue
            }
            val delta = change.position - last
            val dt = (change.uptimeMillis - lastTime).coerceAtLeast(1L).toFloat()
            last = change.position
            lastTime = change.uptimeMillis
            if ((delta.x != 0f || delta.y != 0f) && !longPressed) {
                travelled += delta.getDistance()
                if (travelled <= slop) {
                    heldX += delta.x
                    heldY += delta.y
                    heldMs += dt
                } else {
                    onMove(heldX + delta.x, heldY + delta.y, heldMs + dt)
                    heldX = 0f
                    heldY = 0f
                    heldMs = 0f
                }
            }
            event.changes.forEach { it.consume() }
        }
        if (!longPressed && !multiFinger && travelled <= slop && upTime - down.uptimeMillis <= TAP_MAX_MS) {
            onTap(upTime)
        }
    }
}
