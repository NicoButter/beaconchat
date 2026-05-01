package com.nicobutter.beaconchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.nicobutter.beaconchat.ui.screens.VibrationDetectorScreen
import com.nicobutter.beaconchat.ui.screens.WelcomeScreen
import com.nicobutter.beaconchat.ui.screens.EmergencyTransmissionScreen
import com.nicobutter.beaconchat.ui.screens.EmergencyType
import com.nicobutter.beaconchat.ui.screens.EmergencyMethod
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
                                        var emergencyType by remember { mutableStateOf<EmergencyType?>(null) }
                                        var emergencyMethod by remember { mutableStateOf(EmergencyMethod.LIGHT) }

                                Scaffold(
                                        modifier = Modifier.fillMaxSize(),
                                        bottomBar = {
                                                if (currentScreen != "welcome" && currentScreen != "emergency") {
                                                        NavigationBar(
                                                                containerColor = MaterialTheme.colorScheme.surface,
                                                                tonalElevation = 8.dp
                                                        ) {
                                                                val navItems = listOf(
                                                                        Triple("transmit", "🔦", "Transmit"),
                                                                        Triple("receive", "📷", "Receive"),
                                                                        Triple("mesh", "📡", "Mesh"),
                                                                        Triple("lightmap", "🎯", "Radar"),
                                                                        Triple("oscilloscope", "📊", "Scope"),
                                                                        Triple("settings", "⚙️", "Settings")
                                                                )
                                                                navItems.forEach { (screen, emoji, label) ->
                                                                        val interactionSource = remember { MutableInteractionSource() }
                                                                        val isPressed by interactionSource.collectIsPressedAsState()
                                                                        val scale by animateFloatAsState(
                                                                                targetValue = when {
                                                                                        isPressed -> 0.78f
                                                                                        currentScreen == screen -> 1.18f
                                                                                        else -> 1f
                                                                                },
                                                                                animationSpec = spring(
                                                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                                                        stiffness = Spring.StiffnessMedium
                                                                                ),
                                                                                label = "${screen}_scale"
                                                                        )
                                                                        NavigationBarItem(
                                                                                selected = currentScreen == screen,
                                                                                onClick = {
                                                                                        cleanupControllers()
                                                                                        currentScreen = screen
                                                                                },
                                                                                icon = {
                                                                                        Text(
                                                                                                text = emoji,
                                                                                                fontSize = 22.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                },
                                                                                label = { Text(label) },
                                                                                interactionSource = interactionSource
                                                                        )
                                                                }
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
                                                                onEmergencySOS = {
                                                                        cleanupControllers()
                                                                        emergencyType = EmergencyType.SOS
                                                                        emergencyMethod = EmergencyMethod.ALL
                                                                        currentScreen = "emergency"
                                                                },
                                                                onEmergencyHelp = {
                                                                        cleanupControllers()
                                                                        emergencyType = EmergencyType.HELP
                                                                        emergencyMethod = EmergencyMethod.ALL
                                                                        currentScreen = "emergency"
                                                                },
                                                                modifier =
                                                                        Modifier.padding(
                                                                                innerPadding
                                                                        )
                                                        )
                                                "emergency" ->
                                                        emergencyType?.let { type ->
                                                                EmergencyTransmissionScreen(
                                                                        emergencyType = type,
                                                                        method = emergencyMethod,
                                                                        flashlightController = flashlightController,
                                                                        vibrationController = vibrationController,
                                                                        soundController = soundController,
                                                                        morseEncoder = morseEncoder,
                                                                        onDismiss = {
                                                                                cleanupControllers()
                                                                                currentScreen = "welcome"
                                                                                emergencyType = null
                                                                        },
                                                                        modifier = Modifier.fillMaxSize()
                                                                )
                                                        }
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
                                                                lifecycleOwner = this@MainActivity,
                                                                onNavigateToVibrationDetector = {
                                                                        cleanupControllers()
                                                                        currentScreen = "vibrationDetector"
                                                                }
                                                        )
                                                "vibrationDetector" ->
                                                        VibrationDetectorScreen(
                                                                onNavigateBack = {
                                                                        cleanupControllers()
                                                                        currentScreen = "receive"
                                                                }
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
