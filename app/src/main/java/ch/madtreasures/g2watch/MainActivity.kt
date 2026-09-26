package ch.madtreasures.g2watch

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import ch.madtreasures.g2watch.glasses.Stage
import ch.madtreasures.g2watch.ui.DevicesScreen
import ch.madtreasures.g2watch.ui.LogScreen
import ch.madtreasures.g2watch.ui.MenuScreen
import ch.madtreasures.g2watch.ui.PermissionScreen
import ch.madtreasures.g2watch.ui.StatusScreen
import ch.madtreasures.g2watch.ui.TouchpadScreen

class MainActivity : ComponentActivity() {

    private val app get() = application as G2WatchApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScaffold(timeText = { TimeText() }) {
                    Root()
                }
            }
        }
    }

    private enum class Screen { DEVICES, STATUS, TOUCHPAD, MENU, LOG }

    @Composable
    private fun Root() {
        val glasses = app.glasses
        val desktop = app.desktop
        val scanner = app.scanner
        val state by glasses.state.collectAsStateWithLifecycle()
        val speed by desktop.speed.collectAsStateWithLifecycle()
        var permissionTick by remember { mutableIntStateOf(0) }
        val missing = remember(permissionTick) { missingPermissions() }
        var screen by rememberSaveable { mutableStateOf(Screen.DEVICES) }
        var logReturn by rememberSaveable { mutableStateOf(Screen.DEVICES) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionTick++ }
        val enableBtLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { permissionTick++ }

        // Keep the watch awake while it talks to the glasses: the touchpad stops working once
        // Wear OS dims into ambient mode. Not while the glasses charge or are out of reach
        // (that can last hours), and not without glasses: then the screen times out as usual.
        val keepAwake = when (state.stage) {
            Stage.CHECKING, Stage.CONNECTING, Stage.CONNECTED, Stage.DISCONNECTING -> true
            else -> false
        }
        LaunchedEffect(keepAwake) {
            if (keepAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Follow the connection: to the touchpad once the glasses show the desktop, to the status
        // page while checking and when something needs explaining.
        LaunchedEffect(state.stage) {
            when (state.stage) {
                Stage.CONNECTED -> if (screen == Screen.STATUS || screen == Screen.DEVICES) screen = Screen.TOUCHPAD
                Stage.CHECKING, Stage.CONNECTING -> if (screen == Screen.DEVICES) screen = Screen.STATUS
                Stage.INCOMPATIBLE, Stage.FAILED ->
                    if (screen == Screen.TOUCHPAD || screen == Screen.MENU || screen == Screen.DEVICES) screen = Screen.STATUS
                else -> Unit
            }
        }

        fun connect(title: String, right: String, left: String?) {
            if (missingPermissions().isNotEmpty()) {
                // Revoked since the list was shown: ask again first.
                permissionTick++
                screen = Screen.DEVICES
                return
            }
            scanner.stop()
            app.saveLastPair(title, right, left)
            glasses.connect(title, right, left)
            screen = Screen.STATUS
        }

        // Leaves a finished check or failure behind, so coming back does not show it again.
        fun openPreview() {
            if (!state.stage.hasSession && !state.stage.busy) glasses.disconnect()
            scanner.stop()
            screen = Screen.TOUCHPAD
        }

        when (screen) {
            Screen.DEVICES -> {
                if (missing.isNotEmpty()) {
                    PermissionScreen(
                        onRequest = { permissionLauncher.launch((missing + optionalPermissions()).toTypedArray()) },
                        onOpenSettings = {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                            )
                        },
                        onPreview = { openPreview() },
                    )
                    return
                }
                val pairs by scanner.pairs.collectAsStateWithLifecycle()
                val scanning by scanner.scanning.collectAsStateWithLifecycle()
                val scanError by scanner.error.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    if (scanner.bluetoothEnabled()) scanner.start() else scanner.refreshKnownDevices()
                }
                DevicesScreen(
                    pairs = pairs,
                    scanning = scanning,
                    bluetoothOn = scanner.bluetoothEnabled(),
                    scanError = scanError,
                    lastPair = app.lastPair(),
                    onScan = { scanner.start() },
                    onEnableBluetooth = {
                        try {
                            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                    },
                    onConnectPair = { pair ->
                        val right = pair.right ?: return@DevicesScreen
                        connect(pair.title, right.address, pair.left?.address)
                    },
                    onConnectLast = { last -> connect(last.title, last.right, last.left) },
                    onForgetLast = { app.forgetLastPair(); permissionTick++ },
                    onPreview = { openPreview() },
                    onLog = { logReturn = Screen.DEVICES; screen = Screen.LOG },
                )
            }

            Screen.STATUS -> StatusScreen(
                state = state,
                onCancel = { glasses.disconnect(); screen = Screen.DEVICES },
                onRetry = { app.lastPair()?.let { connect(it.title, it.right, it.left) } },
                onOpenTouchpad = { screen = Screen.TOUCHPAD },
                onPreview = { openPreview() },
                onLog = { logReturn = Screen.STATUS; screen = Screen.LOG },
            )

            Screen.TOUCHPAD -> {
                val frame by desktop.frame.collectAsStateWithLifecycle()
                TouchpadScreen(
                    frame = frame,
                    glasses = state,
                    speed = speed,
                    onMove = { dx, dy -> desktop.moveBy(dx, dy) },
                    onSpeed = { desktop.setSpeed(it) },
                    onClick = { desktop.click() },
                    onOpenMenu = { screen = Screen.MENU },
                )
            }

            Screen.MENU -> {
                BackHandler { screen = Screen.TOUCHPAD }
                MenuScreen(
                    state = state,
                    speed = speed,
                    onBack = { screen = Screen.TOUCHPAD },
                    onCloseWindow = { desktop.back(); screen = Screen.TOUCHPAD },
                    onCenter = { desktop.centerPointer(); screen = Screen.TOUCHPAD },
                    onSpeed = { desktop.setSpeed(it) },
                    onConnect = { screen = Screen.DEVICES },
                    onDisconnect = { glasses.disconnect() },
                    onLog = { logReturn = Screen.MENU; screen = Screen.LOG },
                )
            }

            Screen.LOG -> {
                val lines by glasses.log.collectAsStateWithLifecycle()
                BackHandler { screen = logReturn }
                LogScreen(lines = lines, onBack = { screen = logReturn })
            }
        }
    }

    private fun missingPermissions(): List<String> =
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

    /** Asked for together with Bluetooth, but the app works without it (no ongoing notification). */
    private fun optionalPermissions(): List<String> =
        listOf(Manifest.permission.POST_NOTIFICATIONS)
            .filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
}
