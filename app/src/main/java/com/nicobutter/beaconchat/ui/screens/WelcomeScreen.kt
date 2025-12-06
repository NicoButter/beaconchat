package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * @param onEmergencySOS Callback invoked when user taps emergency SOS button
 * @param onEmergencyHelp Callback invoked when user taps emergency HELP button
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun WelcomeScreen(
        onNavigateToTransmit: () -> Unit,
        onNavigateToReceive: () -> Unit,
        onEmergencySOS: () -> Unit = {},
        onEmergencyHelp: () -> Unit = {},
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

                // Emergency buttons section
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                        Icon(
                                                Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                "EMERGENCIAS",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                        )
                                }

                                // Emergency SOS Button
                                Button(
                                        onClick = onEmergencySOS,
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFD32F2F),
                                                contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                ) {
                                        Text(
                                                "🆘 SOS - EMERGENCIA",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Emergency HELP Button
                                Button(
                                        onClick = onEmergencyHelp,
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                        colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFF6F00),
                                                contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                ) {
                                        Text(
                                                "⚠️ AUXILIO",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }

                                Text(
                                        "Transmite señal continua para drones y rescate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(top = 8.dp)
                                )
                        }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
