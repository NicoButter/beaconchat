package com.nicobutter.beaconchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.nicobutter.beaconchat.data.UserPreferences
import com.nicobutter.beaconchat.controller.EmergencyManager
import com.nicobutter.beaconchat.emitter.BleEmitter
import com.nicobutter.beaconchat.emitter.LightEmitter
import com.nicobutter.beaconchat.emitter.SoundEmitter
import com.nicobutter.beaconchat.emitter.VibrationEmitter
import com.nicobutter.beaconchat.mesh.BLEMeshController
import com.nicobutter.beaconchat.scanner.BleScanner
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import com.nicobutter.beaconchat.transceiver.SoundController
import com.nicobutter.beaconchat.transceiver.VibrationController
import com.nicobutter.beaconchat.ui.screens.LightMapScreen
import com.nicobutter.beaconchat.ui.screens.MeshScreen
import com.nicobutter.beaconchat.ui.screens.OscilloscopeScreen
import com.nicobutter.beaconchat.ui.screens.ReceiverScreen
import com.nicobutter.beaconchat.ui.screens.SettingsScreen
import com.nicobutter.beaconchat.ui.screens.SignalScannerScreen
import com.nicobutter.beaconchat.ui.screens.TransmitterScreen
import com.nicobutter.beaconchat.ui.screens.VibrationDetectorScreen
import com.nicobutter.beaconchat.ui.AppScreen
import com.nicobutter.beaconchat.ui.BeaconNavigation
import com.nicobutter.beaconchat.ui.screens.EmergencyTransmissionScreen
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
        private lateinit var emergencyManager: EmergencyManager
        private lateinit var bleScanner: BleScanner
        private lateinit var userPreferences: UserPreferences

        private fun cleanupControllers() {
                emergencyManager.stopEmergency()
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
                bleScanner = BleScanner(this)
                userPreferences = UserPreferences(this)
                val morseEncoder = MorseEncoder()
                emergencyManager = EmergencyManager(
                        lightEmitter = LightEmitter(flashlightController),
                        vibrationEmitter = VibrationEmitter(vibrationController),
                        soundEmitter = SoundEmitter(soundController),
                        bleEmitter = BleEmitter(this),
                        morseEncoder = morseEncoder,
                        scope = lifecycleScope
                )

                setContent {
                        BeaconChatTheme {
                                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                        BeaconNavigation(
                                                onLeaveScreen = ::cleanupControllers,
                                                emergencyContent = { type, mode, dismiss ->
                                                        EmergencyTransmissionScreen(type, mode, emergencyManager, dismiss,
                                                                Modifier.fillMaxSize().safeDrawingPadding())
                                                },
                                                featureContent = { screen, modifier, requestEmergency, navigate ->
                                                        when (screen) {
                                                                AppScreen.TRANSMIT -> TransmitterScreen(
                                                                        flashlightController, vibrationController, soundController,
                                                                        morseEncoder, userPreferences, requestEmergency, modifier)
                                                                AppScreen.RECEIVE -> ReceiverScreen(
                                                                        modifier = modifier,
                                                                        onNavigateToVibrationDetector = { navigate(AppScreen.VIBRATION_DETECTOR) })
                                                                AppScreen.VIBRATION_DETECTOR -> Box(modifier) {
                                                                        VibrationDetectorScreen(onNavigateBack = { navigate(AppScreen.RECEIVE) })
                                                                }
                                                                AppScreen.MESH -> MeshScreen(meshController, userPreferences, modifier)
                                                                AppScreen.LIGHTMAP -> LightMapScreen(flashlightController, modifier)
                                                                AppScreen.OSCILLOSCOPE -> OscilloscopeScreen(modifier)
                                                                AppScreen.SCAN -> SignalScannerScreen(bleScanner, modifier)
                                                                AppScreen.SETTINGS -> SettingsScreen(userPreferences, modifier)
                                                                else -> Unit // Institutional/Home/emergency routes belong to the UX shell.
                                                        }
                                                }
                                        )
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
