package com.nicobutter.beaconchat.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nicobutter.beaconchat.R
import com.nicobutter.beaconchat.ui.theme.*

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
                        .background(BeaconBackground)
        ) {
                // Glow orbs for depth
                Box(
                        modifier = Modifier
                                .size(300.dp)
                                .offset(x = (-60).dp, y = 60.dp)
                                .blur(120.dp)
                                .background(
                                        BeaconPrimary.copy(alpha = 0.25f),
                                        CircleShape
                                )
                )
                Box(
                        modifier = Modifier
                                .size(250.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 60.dp, y = (-80).dp)
                                .blur(100.dp)
                                .background(
                                        BeaconSecondary.copy(alpha = 0.20f),
                                        CircleShape
                                )
                )

                Column(
                        modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                ) {

                        // ── Header: Logo + branding ──────────────────────────────
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Logo con anillo de acento
                                Box(contentAlignment = Alignment.Center) {
                                        Box(
                                                modifier = Modifier
                                                        .size(130.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                                Brush.radialGradient(
                                                                        colors = listOf(
                                                                                BeaconPrimary.copy(alpha = 0.30f),
                                                                                Color.Transparent
                                                                        )
                                                                )
                                                        )
                                        )
                                        Image(
                                                painter = painterResource(id = R.drawable.logo),
                                                contentDescription = "BeaconChat Logo",
                                                modifier = Modifier.size(100.dp)
                                        )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                        text = "BeaconChat",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BeaconOnBackground,
                                        letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                        text = "Comunicación por Luz · Sin infraestructura",
                                        fontSize = 13.sp,
                                        color = BeaconTextMuted,
                                        textAlign = TextAlign.Center
                                )
                        }

                        // ── Main action cards ────────────────────────────────────
                        Column(
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                // TRANSMITIR
                                ActionCard(
                                        emoji = "🔦",
                                        label = "TRANSMITIR",
                                        description = "Enviar un mensaje con la linterna",
                                        gradient = Brush.horizontalGradient(
                                                colors = listOf(BeaconPrimaryDark, BeaconPrimary)
                                        ),
                                        accentColor = BeaconPrimary,
                                        onClick = onNavigateToTransmit
                                )

                                // DETECTAR
                                ActionCard(
                                        emoji = "📡",
                                        label = "DETECTAR",
                                        description = "Recibir mensaje por cámara",
                                        gradient = Brush.horizontalGradient(
                                                colors = listOf(BeaconSecondaryDark, BeaconSecondary)
                                        ),
                                        accentColor = BeaconSecondary,
                                        onClick = onNavigateToReceive
                                )
                        }

                        // ── Emergency row ────────────────────────────────────────
                        Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                                Text(
                                        text = "EMERGENCIA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        color = BeaconTextMuted
                                )

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        EmergencyButton(
                                                label = "SOS",
                                                modifier = Modifier.weight(1f),
                                                onClick = onEmergencySOS
                                        )
                                        EmergencyButton(
                                                label = "HELP",
                                                modifier = Modifier.weight(1f),
                                                onClick = onEmergencyHelp
                                        )
                                }
                        }
                }
        }
}

/**
 * Reusable full-width action card for primary navigation.
 *
 * Displays an emoji icon, a bold label, and a short description inside
 * a gradient rounded card. Tapping triggers [onClick].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
        emoji: String,
        label: String,
        description: String,
        gradient: Brush,
        accentColor: Color,
        onClick: () -> Unit
) {
        Card(
                onClick = onClick,
                modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                        listOf(accentColor.copy(alpha = 0.6f), Color.Transparent)
                                ),
                                shape = RoundedCornerShape(20.dp)
                        ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
                Box(
                        modifier = Modifier
                                .fillMaxSize()
                                .background(gradient)
                                .padding(horizontal = 20.dp)
                ) {
                        Row(
                                modifier = Modifier.align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                // Icono en caja con fondo semitransparente
                                Box(
                                        modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(text = emoji, fontSize = 28.sp)
                                }

                                Column {
                                        Text(
                                                text = label,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                                text = description,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.75f)
                                        )
                                }
                        }

                        // Flecha indicadora
                        Text(
                                text = "›",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.CenterEnd)
                        )
                }
        }
}

/**
 * Compact emergency button rendered as an outlined rounded chip.
 *
 * Uses a red-tinted surface so it stands out without being as prominent
 * as the main action cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencyButton(
        label: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
) {
        OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = BeaconEmergencyDark.copy(alpha = 0.50f),
                        contentColor = BeaconEmergency
                ),
                border = BorderStroke(1.dp, BeaconEmergency.copy(alpha = 0.70f))
        ) {
                Text(
                        text = "⚠ $label",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                )
        }
}
