package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.transceiver.FlashlightController
import com.nicobutter.beaconchat.transceiver.MorseEncoder
import kotlinx.coroutines.launch

@Composable
fun TransmitterScreen(
        flashlightController: FlashlightController,
        morseEncoder: MorseEncoder,
        modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var isTransmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Text(text = "BeaconChat Transmitter", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Enter message") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTransmitting
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
                onClick = {
                    if (text.isNotBlank()) {
                        isTransmitting = true
                        scope.launch {
                            val timings = morseEncoder.encode(text)
                            flashlightController.transmit(timings)
                            isTransmitting = false
                        }
                    }
                },
                enabled = !isTransmitting && text.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
        ) { Text(if (isTransmitting) "Transmitting..." else "Send Message") }
    }
}
