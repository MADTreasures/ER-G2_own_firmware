package ch.madtreasures.g2watch.desktop

import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The arrow sprite for a color-key surface: 0 is transparent, 1 is opaque black (it keeps the
 * outline readable over bright content), 255 is the bright outline. The tip is at (0, 0).
 */
object PointerSprite {
    private val SHAPE = arrayOf(
        "X",
        "XX",
        "X.X",
        "X..X",
        "X...X",
        "X....X",
        "X.....X",
        "X......X",
        "X.......X",
        "X........X",
        "X.....XXXXX",
        "X..X..X",
        "X.X X..X",
        "XX  X..X",
        "X    X..X",
        "     X..X",
        "      XX",
    )

    val width: Int = SHAPE.maxOf { it.length }
    val height: Int = SHAPE.size

    val pixels: ByteArray = ByteArray(width * height).also { out ->
        for ((y, row) in SHAPE.withIndex()) {
            for ((x, c) in row.withIndex()) {
                out[y * width + x] = when (c) {
                    'X' -> 255
                    '.' -> 1
                    else -> 0
                }.toByte()
            }
        }
    }

    /** Changes only if the shape changes; moving the sprite changes the surface geometry instead. */
    const val FINGERPRINT = "pointer:arrow-v1"
}

/** Pointer position in screen pixels, kept inside [bounds] (the part of the screen the wearer sees). */
class PointerPosition(private val bounds: Rect) {
    private var fx = bounds.x + bounds.w / 2f
    private var fy = bounds.y + bounds.h / 2f

    val x: Int get() = fx.roundToInt()
    val y: Int get() = fy.roundToInt()

    /** Moves by a delta in glasses pixels; true if the rounded position changed. */
    fun moveBy(dx: Float, dy: Float): Boolean {
        val oldX = x
        val oldY = y
        fx = (fx + dx).coerceIn(bounds.x.toFloat(), (bounds.right - 1).toFloat())
        fy = (fy + dy).coerceIn(bounds.y.toFloat(), (bounds.bottom - 1).toFloat())
        return x != oldX || y != oldY
    }

    fun center() {
        fx = bounds.x + bounds.w / 2f
        fy = bounds.y + bounds.h / 2f
    }
}

/**
 * Watch finger movement to glasses pixels, as tuned on the Pixel Watch in G2 Direct: slow strokes
 * position precisely, fast flicks cross the display.
 */
object PointerMotion {
    /** Glasses pixels per watch dp at speed 1.0 before acceleration. */
    const val BASE_GAIN = 2.6f
    const val MIN_SPEED = 0.3f
    const val MAX_SPEED = 4f

    /** [dxDp]/[dyDp]: finger movement in dp over [dtMs]; returns the glasses-pixel delta. */
    fun toGlasses(dxDp: Float, dyDp: Float, dtMs: Float, speed: Float): Pair<Float, Float> {
        val velocity = hypot(dxDp, dyDp) / dtMs.coerceAtLeast(1f)
        val accel = (0.55f + velocity * 1.2f).coerceIn(0.55f, 2.6f)
        val k = BASE_GAIN * speed.coerceIn(MIN_SPEED, MAX_SPEED) * accel
        return Pair(dxDp * k, dyDp * k)
    }
}
