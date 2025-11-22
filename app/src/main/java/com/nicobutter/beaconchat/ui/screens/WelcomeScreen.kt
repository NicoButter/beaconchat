package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
        onNavigateToTransmit: () -> Unit,
        onNavigateToReceive: () -> Unit,
        modifier: Modifier = Modifier
) {
    Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Welcome to BeaconChat", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Secure Light Communication", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onNavigateToTransmit, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Start Transmitter")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateToReceive, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Start Receiver")
        }
    }
}
