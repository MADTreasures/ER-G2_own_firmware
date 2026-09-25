package ch.madtreasures.g2watch.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerTest {
    private val band = Rect(0, 96, 640, 288)

    @Test
    fun `starts in the middle of the visible band`() {
        val p = PointerPosition(band)
        assertEquals(320, p.x)
        assertEquals(240, p.y)
    }

    @Test
    fun `stays inside the band`() {
        val p = PointerPosition(band)
        p.moveBy(-10_000f, -10_000f)
        assertEquals(0, p.x)
        assertEquals(96, p.y)
        p.moveBy(10_000f, 10_000f)
        assertEquals(639, p.x)
        assertEquals(383, p.y)
    }

    @Test
    fun `sub-pixel moves add up and report only visible changes`() {
        val p = PointerPosition(band)
        assertFalse(p.moveBy(0.3f, 0f))
        assertTrue(p.moveBy(0.3f, 0f))
        assertEquals(321, p.x)
    }

    @Test
    fun `center returns to the middle`() {
        val p = PointerPosition(band)
        p.moveBy(100f, 50f)
        p.center()
        assertEquals(320, p.x)
        assertEquals(240, p.y)
    }

    @Test
    fun `slow strokes are precise and fast flicks cover distance`() {
        // 1 dp in 100 ms: barely any acceleration.
        val (slowX, _) = PointerMotion.toGlasses(1f, 0f, 100f, 1f)
        assertEquals(PointerMotion.BASE_GAIN * (0.55f + 0.01f * 1.2f), slowX, 0.001f)
        // 40 dp in 10 ms: acceleration at its cap.
        val (fastX, _) = PointerMotion.toGlasses(40f, 0f, 10f, 1f)
        assertEquals(40f * PointerMotion.BASE_GAIN * 2.6f, fastX, 0.001f)
    }

    @Test
    fun `speed is limited`() {
        val (x, _) = PointerMotion.toGlasses(1f, 0f, 100f, 100f)
        assertEquals(PointerMotion.BASE_GAIN * (0.55f + 0.01f * 1.2f) * PointerMotion.MAX_SPEED, x, 0.001f)
    }

    @Test
    fun `sprite uses only the color-key values`() {
        assertEquals(11, PointerSprite.width)
        assertEquals(17, PointerSprite.height)
        assertEquals(255, PointerSprite.pixels[0].toInt() and 0xFF) // the tip
        val values = PointerSprite.pixels.map { it.toInt() and 0xFF }.toSet()
        assertEquals(setOf(0, 1, 255), values)
    }
}
