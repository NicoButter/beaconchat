package com.nicobutter.beaconchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
                                                                // Transmit
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "transmit",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "transmit"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "transmit") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "transmit_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "transmit") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "🔦",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Transmit",
                                                                                        fontWeight = if (currentScreen == "transmit") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
                                                                )
                                                                // Receive
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "receive",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "receive"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "receive") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "receive_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "receive") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "📷",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Receive",
                                                                                        fontWeight = if (currentScreen == "receive") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
                                                                )
                                                                // Mesh
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "mesh",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "mesh"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "mesh") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "mesh_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "mesh") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "📡",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Mesh",
                                                                                        fontWeight = if (currentScreen == "mesh") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
                                                                )
                                                                // Radar
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "lightmap",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "lightmap"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "lightmap") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "radar_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "lightmap") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "🎯",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Radar",
                                                                                        fontWeight = if (currentScreen == "lightmap") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
                                                                )
                                                                // Oscilloscope
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "oscilloscope",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "oscilloscope"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "oscilloscope") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "scope_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "oscilloscope") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "📊",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Scope",
                                                                                        fontWeight = if (currentScreen == "oscilloscope") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
                                                                )
                                                                // Settings
                                                                NavigationBarItem(
                                                                        selected = currentScreen == "settings",
                                                                        onClick = {
                                                                                cleanupControllers()
                                                                                currentScreen = "settings"
                                                                        },
                                                                        icon = {
                                                                                val scale by animateFloatAsState(
                                                                                        targetValue = if (currentScreen == "settings") 1.2f else 1f,
                                                                                        animationSpec = tween(durationMillis = 200),
                                                                                        label = "settings_scale"
                                                                                )
                                                                                Box {
                                                                                        if (currentScreen == "settings") {
                                                                                                Box(
                                                                                                        modifier = Modifier
                                                                                                                .size(48.dp)
                                                                                                                .background(
                                                                                                                        MaterialTheme.colorScheme.primaryContainer,
                                                                                                                        CircleShape
                                                                                                                )
                                                                                                )
                                                                                        }
                                                                                        Text(
                                                                                                text = "⚙️",
                                                                                                fontSize = 24.sp,
                                                                                                modifier = Modifier.scale(scale)
                                                                                        )
                                                                                }
                                                                        },
                                                                        label = { 
                                                                                Text(
                                                                                        "Settings",
                                                                                        fontWeight = if (currentScreen == "settings") FontWeight.Bold else FontWeight.Normal
                                                                                )
                                                                        }
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
