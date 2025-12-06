package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
        onNavigateToTransmit: () -> Unit,
        onNavigateToReceive: () -> Unit,
        onEmergencySOS: () -> Unit = {},
        onEmergencyHelp: () -> Unit = {},
        modifier: Modifier = Modifier
) {
        Box(
                modifier = modifier
                        .fillMaxSize()
                        .background(
                                brush = Brush.verticalGradient(
                                        colors = listOf(
                                                Color.Black,
                                                Color(0xFF1A1A1A),
                                                Color(0xFF2D2D2D),
                                                Color(0xFF404040)
                                        )
                                )
                        )
        ) {
                Column(
                        modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                        Spacer(modifier = Modifier.weight(1f))

                        // Logo grande
                        Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "BeaconChat Logo",
                                modifier = Modifier.size(252.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Nombre de la app
                        Text(
                                text = "BeaconChat",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = "Comunicación por Luz",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.weight(1.5f))

                        // Botones principales
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                // Botón Transmitir
                                Card(
                                        onClick = onNavigateToTransmit,
                                        modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                        colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF6200EE)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                        ) {
                                                Text(
                                                        text = "🔦",
                                                        fontSize = 64.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = "TRANSMITIR",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                )
                                        }
                                }

                                // Botón Detectar
                                Card(
                                        onClick = onNavigateToReceive,
                                        modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f),
                                        colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF03DAC6)
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                ) {
                                        Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                        ) {
                                                Text(
                                                        text = "📡",
                                                        fontSize = 64.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = "DETECTAR",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                )
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                }
        }
}
