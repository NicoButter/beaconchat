package com.nicobutter.beaconchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import com.nicobutter.beaconchat.ui.screens.ReceiverScreen
import com.nicobutter.beaconchat.ui.screens.TransmitterScreen
import com.nicobutter.beaconchat.ui.screens.WelcomeScreen
import com.nicobutter.beaconchat.ui.theme.BeaconChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔧 Reemplazo moderno de enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val flashlightController = FlashlightController(this)
        val morseEncoder = MorseEncoder()

        setContent {
            BeaconChatTheme {
                var currentScreen by remember { mutableStateOf("welcome") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen != "welcome") {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen == "transmit",
                                    onClick = { currentScreen = "transmit" },
                                    icon = { Text("🔦") },
                                    label = { Text("Transmit") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen == "receive",
                                    onClick = { currentScreen = "receive" },
                                    icon = { Text("📷") },
                                    label = { Text("Receive") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (currentScreen) {
                        "welcome" -> WelcomeScreen(
                            onNavigateToTransmit = { currentScreen = "transmit" },
                            onNavigateToReceive = { currentScreen = "receive" },
                            modifier = Modifier.padding(innerPadding)
                        )
                        "transmit" -> TransmitterScreen(
                            flashlightController = flashlightController,
                            morseEncoder = morseEncoder,
                            modifier = Modifier.padding(innerPadding)
                        )
                        "receive" -> ReceiverScreen(
                            modifier = Modifier.padding(innerPadding),
                            lifecycleOwner = this@MainActivity
                        )
                    }
                }
            }
        }
    }
}
