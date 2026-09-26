package ch.madtreasures.g2watch.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import ch.madtreasures.g2watch.FakeScheduler
import ch.madtreasures.g2watch.G2WatchApp
import ch.madtreasures.g2watch.desktop.AndroidTextPainter
import ch.madtreasures.g2watch.desktop.AppId
import ch.madtreasures.g2watch.desktop.DesktopController
import ch.madtreasures.g2watch.glasses.FirmwareRequirement
import ch.madtreasures.g2watch.glasses.GlassesState
import ch.madtreasures.g2watch.glasses.Stage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDateTime

/**
 * Pictures of the watch screens on a round 454 px face (Pixel Watch size class), for checking
 * the layout without a watch:
 *
 *     ./gradlew :app:testDebugUnitTest --tests '*WatchSnapshotTest*' -PsnapshotDir=$PWD/docs/bilder
 *
 * Skipped without -PsnapshotDir. Everything outside the round face is greyed out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-rDE-w227dp-h227dp-round-watch-xhdpi", application = android.app.Application::class)
class WatchSnapshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val dir = System.getProperty("snapshotDir")

    @Before
    fun needsSnapshotDir() = assumeTrue("no -PsnapshotDir", !dir.isNullOrBlank())

    private fun snapshot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme {
                AppScaffold(timeText = { TimeText() }) { content() }
            }
        }
        compose.waitForIdle()
        val shot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir!!).mkdirs()
        File(dir, "$name.png").outputStream().use { roundFace(shot).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun roundFace(shot: Bitmap): Bitmap {
        val out = shot.copy(Bitmap.Config.ARGB_8888, true)
        val outside = Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addRect(0f, 0f, out.width.toFloat(), out.height.toFloat(), Path.Direction.CW)
            addCircle(out.width / 2f, out.height / 2f, minOf(out.width, out.height) / 2f, Path.Direction.CW)
        }
        Canvas(out).drawPath(outside, Paint().apply { color = Color.rgb(60, 60, 60) })
        return out
    }

    private fun desktopFrame(open: AppId? = null): DesktopController {
        val scheduler = FakeScheduler()
        val controller = DesktopController(
            AndroidTextPainter(),
            scheduler,
            nowMs = { scheduler.now },
            now = { LocalDateTime.of(2026, 9, 25, 14, 5) },
        )
        controller.startClock()
        controller.updateStatus { it.copy(watchBattery = 76, glassesBattery = 81) }
        scheduler.runPending()
        val tile = controller.layout.tiles.first { it.first == (open ?: AppId.CLOCK) }.second
        controller.moveBy((tile.x + 60 - 320).toFloat(), (tile.y + 40 - 240).toFloat())
        scheduler.advanceBy(DesktopController.POINTER_INTERVAL_MS)
        if (open != null) {
            controller.click()
            scheduler.runPending()
        }
        return controller
    }

    private val connected = GlassesState(
        stage = Stage.CONNECTED,
        title = "G2 A1B2",
        battery = 81,
        firmware = FirmwareRequirement.check("2.3.0.24", "2.3.0.24", "Faceclaw/34"),
        framesSent = 1234,
        transmitMs = 42,
        lastInput = "Tipp (rechter Bügel)",
    )

    @Test
    fun touchpadConnected() {
        val frame = desktopFrame().frame.value
        snapshot("uhr-touchpad") { TouchpadScreen(frame, connected, 1f, { _, _ -> }, {}, {}, {}) }
    }

    @Test
    fun touchpadPreview() {
        val frame = desktopFrame(AppId.CLOCK).frame.value
        snapshot("uhr-vorschau") { TouchpadScreen(frame, GlassesState(), 1f, { _, _ -> }, {}, {}, {}) }
    }

    @Test
    fun incompatible() = snapshot("uhr-firmware-passt-nicht") {
        val verdict = FirmwareRequirement.check("2.3.0.24", "2.3.0.24", "")
        StatusScreen(
            GlassesState(stage = Stage.INCOMPATIBLE, title = "G2 A1B2", detail = verdict.message.orEmpty(), firmware = verdict),
            {}, {}, {}, {}, {},
        )
    }

    @Test
    fun checking() = snapshot("uhr-pruefung") {
        StatusScreen(
            GlassesState(stage = Stage.CHECKING, title = "G2 A1B2", detail = "Lese die Firmware-Version (rechter Bügel) …"),
            {}, {}, {}, {}, {},
        )
    }

    @Test
    fun devices() = snapshot("uhr-geraete") {
        DevicesScreen(
            pairs = emptyList(),
            scanning = false,
            bluetoothOn = true,
            scanError = null,
            lastPair = G2WatchApp.LastPair("G2 A1B2", "AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02"),
            onScan = {}, onEnableBluetooth = {}, onConnectPair = {}, onConnectLast = {}, onForgetLast = {},
            onPreview = {}, onLog = {},
        )
    }

    @Test
    fun menu() = snapshot("uhr-menue") {
        MenuScreen(connected, 1.2f, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun log() = snapshot("uhr-protokoll") {
        LogScreen(
            listOf(
                "14:05:01 Verbinde mit G2 A1B2 (R AA:BB:CC:DD:EE:01, L AA:BB:CC:DD:EE:02)",
                "14:05:03 security auth: right lens, bond state BONDED",
                "14:05:04 device-info: L=2.3.0.24 R=2.3.0.24 ext=[]",
                "14:05:04 Firmware: Original 2.3.0.24 – Die Brille hat die Original-Firmware " +
                    "(L=2.3.0.24 R=2.3.0.24). Diese App braucht Faceclaw-Firmware Revision 34.",
            ),
            {},
        )
    }

    @Test
    fun permission() = snapshot("uhr-berechtigung") {
        PermissionScreen({}, {}, {})
    }
}
