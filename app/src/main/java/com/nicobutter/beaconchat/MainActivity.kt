package com.nicobutter.beaconchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.nicobutter.beaconchat.data.UserPreferences
import com.nicobutter.beaconchat.mesh.BLEMeshController
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import com.nicobutter.beaconchat.transceiver.SoundController
import com.nicobutter.beaconchat.transceiver.VibrationController
import com.nicobutter.beaconchat.ui.screens.LightMapScreen
import com.nicobutter.beaconchat.ui.screens.MeshScreen
import com.nicobutter.beaconchat.ui.screens.OscilloscopeScreen
import com.nicobutter.beaconchat.ui.screens.ReceiverScreen
import com.nicobutter.beaconchat.ui.screens.SettingsScreen
import com.nicobutter.beaconchat.ui.screens.TransmitterScreen
import com.nicobutter.beaconchat.ui.screens.WelcomeScreen
import com.nicobutter.beaconchat.ui.theme.BeaconChatTheme

/**
 * Main activity for BeaconChat application.
 *
 * This is the primary entry point and navigation hub for the BeaconChat app.
 * Manages the lifecycle of all hardware controllers (flashlight, vibration, sound)
 * and provides navigation between different communication screens including
 * transmission, reception, mesh networking, light mapping, oscilloscope analysis,
 * and settings. Implements proper resource cleanup to prevent hardware conflicts.
 */
class MainActivity : ComponentActivity() {
        private lateinit var flashlightController: FlashlightController
        private lateinit var vibrationController: VibrationController
        private lateinit var soundController: SoundController
        private lateinit var meshController: BLEMeshController
        private lateinit var userPreferences: UserPreferences

        /**
         * Cleans up all hardware controllers to prevent conflicts between screens.
         *
         * Ensures flashlight is turned off, vibration is stopped, and audio
         * resources are released before switching to a different screen.
         */
        private fun cleanupControllers() {
                flashlightController.cleanup()
                vibrationController.cleanup()
                soundController.cleanup()
        }

        /**
         * Initializes the activity and sets up the Compose UI with navigation.
         *
         * Configures window insets for edge-to-edge display, initializes all
         * hardware controllers and user preferences, and sets up the main
         * navigation structure with bottom navigation bar and screen routing.
         *
         * @param savedInstanceState Bundle containing the activity's previously saved state
         */
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                // 🔧 Reemplazo moderno de enableEdgeToEdge()
                WindowCompat.setDecorFitsSystemWindows(window, false)

                flashlightController = FlashlightController(this)
                vibrationController = VibrationController(this)
                soundController = SoundController()
                meshController = BLEMeshController(this)
                userPreferences = UserPreferences(this)
                val morseEncoder = MorseEncoder()

                setContent {
                        BeaconChatTheme {
                                Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                ) {
                                        var currentScreen by remember { mutableStateOf("welcome") }

                                Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        bottomBar = {
                                                if (currentScreen != "welcome") {
                                                        NavigationBar {
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "transmit",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "transmit"
                                                                        },
                                                                        icon = { Text("🔦") },
                                                                        label = { Text("Transmit") }
                                                                )
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "receive",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "receive"
                                                                        },
                                                                        icon = { Text("📷") },
                                                                        label = { Text("Receive") }
                                                                )
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "mesh",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "mesh"
                                                                        },
                                                                        icon = { Text("📡") },
                                                                        label = { Text("Mesh") }
                                                                )
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "lightmap",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "lightmap"
                                                                        },
                                                                        icon = { Text("🎯") },
                                                                        label = { Text("Radar") }
                                                                )
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "oscilloscope",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "oscilloscope"
                                                                        },
                                                                        icon = { Text("📊") },
                                                                        label = { Text("Scope") }
                                                                )
                                                                NavigationBarItem(
                                                                        selected =
                                                                                currentScreen ==
                                                                                        "settings",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen =
                                                                                        "settings"
                                                                        },
                                                                        icon = { Text("⚙️") },
                                                                        label = { Text("Settings") }
                                                                )
                                                        }
                                                }
                                        }
                                ) { innerPadding ->
                                        when (currentScreen) {
                                                "welcome" ->
                                                        WelcomeScreen(
                                                                onNavigateToTransmit = {
                                                                        cleanupControllers()
                                                                        currentScreen = "transmit"
                                                                },
                                                                onNavigateToReceive = {
                                                                        cleanupControllers()
                                                                        currentScreen = "receive"
                                                                },
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "transmit" ->
                                                        TransmitterScreen(
                                                                flashlightController =
                                                                        flashlightController,
                                                                vibrationController =
                                                                        vibrationController,
                                                                soundController = soundController,
                                                                morseEncoder = morseEncoder,
                                                                userPreferences = userPreferences,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "receive" ->
                                                        ReceiverScreen(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        ),
                                                                lifecycleOwner = this@MainActivity
                                                        )
                                                "mesh" ->
                                                        MeshScreen(
                                                                meshController = meshController,
                                                                userPreferences = userPreferences,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "lightmap" ->
                                                        LightMapScreen(
                                                                flashlightController = flashlightController,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "oscilloscope" ->
                                                        OscilloscopeScreen(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "settings" ->
                                                        SettingsScreen(
                                                                userPreferences = userPreferences,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                        }
                                }
                                }
                        }
                }
        }

        /**
         * Called when the activity is paused (goes to background).
         *
         * Ensures all hardware controllers are properly cleaned up when the
         * app loses focus to prevent battery drain and hardware conflicts.
         */
        override fun onPause() {
                super.onPause()
                // Ensure all controllers are cleaned up when app goes to background
                cleanupControllers()
        }

        /**
         * Called when the activity is being destroyed.
         *
         * Performs final cleanup of all hardware controllers to ensure
         * proper resource release when the app is terminated.
         */
        override fun onDestroy() {
                super.onDestroy()
                // Ensure all controllers are cleaned up when app is destroyed
                cleanupControllers()
        }
}
