package ch.madtreasures.g2watch.desktop

import ch.madtreasures.g2watch.FakeDisplay
import ch.madtreasures.g2watch.FakeScheduler
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import javax.imageio.ImageIO

/**
 * Writes PNGs of what the glasses would show, with the app's own renderer and Android's fonts:
 *
 *     ./gradlew :app:testDebugUnitTest --tests '*RenderSnapshotTest*' -PsnapshotDir=$PWD/docs/bilder
 *
 * Skipped without -PsnapshotDir.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class)
class RenderSnapshotTest {

    @Test
    fun render() {
        val dir = System.getProperty("snapshotDir")
        assumeTrue("no -PsnapshotDir", !dir.isNullOrBlank())
        val out = File(dir!!).apply { mkdirs() }

        val scheduler = FakeScheduler()
        val display = FakeDisplay()
        val controller = DesktopController(
            AndroidTextPainter(),
            scheduler,
            nowMs = { scheduler.now },
            now = { LocalDateTime.of(2026, 9, 25, 14, 5) },
        )
        controller.startClock()
        controller.updateStatus {
            it.copy(watchBattery = 76, glassesBattery = 81, connection = "Verbunden", firmware = "Faceclaw/34 · Basis 2.3.0.24")
        }
        controller.attach(display)
        scheduler.runPending()

        fun moveTo(x: Int, y: Int) {
            val frame = controller.frame.value
            controller.moveBy((x - frame.pointerX).toFloat(), (y - frame.pointerY).toFloat())
            scheduler.advanceBy(DesktopController.POINTER_INTERVAL_MS)
        }

        fun tile(app: AppId) = controller.layout.tiles.first { it.first == app }.second

        fun save(name: String) {
            val frame = controller.frame.value
            ImageIO.write(glassesImage(display.submitsOf("desktop").last().pixels, frame.pointerX, frame.pointerY), "png", File(out, "$name.png"))
        }

        moveTo(tile(AppId.CLOCK).x + 60, tile(AppId.CLOCK).y + 40)
        save("desktop-start")

        moveTo(tile(AppId.POINTER).x + 60, tile(AppId.POINTER).y + 40)
        controller.click()
        scheduler.runPending()
        val faster = controller.layout.buttons(AppId.POINTER).first { it.first == ButtonId.FASTER }.second
        moveTo(faster.x + 40, faster.y + 15)
        save("desktop-zeiger")
        controller.back()
        scheduler.runPending()

        for (app in listOf(AppId.CLOCK, AppId.HELP)) {
            moveTo(tile(app).x + 60, tile(app).y + 40)
            controller.click()
            scheduler.runPending()
            moveTo(620, 110)
            save("desktop-" + app.title.lowercase())
            controller.back()
            scheduler.runPending()
        }
    }

    /** The composite as the lens shows it: 16 shades of green, the pointer on top with its color key. */
    private fun glassesImage(desktop: ByteArray, pointerX: Int, pointerY: Int): BufferedImage {
        val w = DesktopLayout.SCREEN_WIDTH
        val h = DesktopLayout.SCREEN_HEIGHT
        fun green(v: Int): Int {
            val level = (v shr 4) * 17
            return ((0x7C * level / 255) shl 16) or ((0xFF * level / 255) shl 8) or (0xA0 * level / 255)
        }
        val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until h) for (x in 0 until w) image.setRGB(x, y, green(desktop[y * w + x].toInt() and 0xFF))
        for (y in 0 until PointerSprite.height) for (x in 0 until PointerSprite.width) {
            val v = PointerSprite.pixels[y * PointerSprite.width + x].toInt() and 0xFF
            val px = pointerX + x
            val py = pointerY + y
            if (v != 0 && px < w && py < h) image.setRGB(px, py, if (v == 1) 0 else green(v))
        }
        return image
    }
}
