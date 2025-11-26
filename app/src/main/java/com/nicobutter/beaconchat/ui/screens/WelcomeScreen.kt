package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
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
                // Icon placeholder
                Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "BeaconChat Icon",
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "BeaconChat",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                        text = "Comunicación por Luz",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Botón Transmitir
                Button(
                        onClick = onNavigateToTransmit,
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                        Text("Transmitir Mensaje", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Recibir
                OutlinedButton(
                        onClick = onNavigateToReceive,
                        modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                        Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Recibir",
                                modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Recibir Mensaje", style = MaterialTheme.typography.titleMedium)
                }
        }
}
