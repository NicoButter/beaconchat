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

class MainActivity : ComponentActivity() {
        private lateinit var flashlightController: FlashlightController
        private lateinit var vibrationController: VibrationController
        private lateinit var soundController: SoundController
        private lateinit var meshController: BLEMeshController
        private lateinit var userPreferences: UserPreferences

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
                                                                                if (currentScreen == "lightmap") {
                                                                                        flashlightController.stop()
                                                                                }
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
                                                                                if (currentScreen == "lightmap") {
                                                                                        flashlightController.stop()
                                                                                }
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
                                                                                if (currentScreen == "lightmap") {
                                                                                        flashlightController.stop()
                                                                                }
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
                                                                                if (currentScreen == "lightmap") {
                                                                                        flashlightController.stop()
                                                                                }
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
                                                                                if (currentScreen == "lightmap") {
                                                                                        flashlightController.stop()
                                                                                }
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
                                                                        if (currentScreen == "lightmap") {
                                                                                flashlightController.stop()
                                                                        }
                                                                        currentScreen = "transmit"
                                                                },
                                                                onNavigateToReceive = {
                                                                        if (currentScreen == "lightmap") {
                                                                                flashlightController.stop()
                                                                        }
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

        override fun onPause() {
                super.onPause()
                // Ensure flashlight is cleaned up when app goes to background
                flashlightController.cleanup()
        }

        override fun onDestroy() {
                super.onDestroy()
                // Ensure flashlight is cleaned up when app is destroyed
                flashlightController.cleanup()
        }
}
