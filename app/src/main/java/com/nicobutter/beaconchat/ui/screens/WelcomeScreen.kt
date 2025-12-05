package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.nicobutter.beaconchat.R

/**
 * Welcome screen for BeaconChat application.
 *
 * The main landing screen that displays the app logo, branding, and provides
 * navigation options to transmit or receive messages. This screen serves as
 * the entry point for users to choose their communication mode.
 *
 * @param onNavigateToTransmit Callback invoked when user taps transmit button
 * @param onNavigateToReceive Callback invoked when user taps receive button
 * @param modifier Modifier for customizing the layout
 */
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
                // Logo de BeaconChat
                Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "BeaconChat Logo",
                        modifier = Modifier.size(120.dp)
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
