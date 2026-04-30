package com.nicobutter.beaconchat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Material Design 3 color palette for BeaconChat theme.
 *
 * Dark theme color system with vibrant accents designed for a modern
 * optical communication application.
 */

// Legacy MD3 references (unused but kept for compatibility)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * BeaconChat custom dark color palette.
 *
 * Deep navy base with cyan/blue transmit accent and emerald receive accent.
 * Red emergency accent for maximum urgency visibility.
 */

// Backgrounds
val BeaconBackground    = Color(0xFF080D1A)   // Near-black navy
val BeaconSurface       = Color(0xFF111827)   // Dark surface card
val BeaconSurfaceVar    = Color(0xFF1C2538)   // Slightly lighter card variant

// Primary accent – transmit (electric blue)
val BeaconPrimary       = Color(0xFF4F8EF7)
val BeaconPrimaryDark   = Color(0xFF1A3870)
val BeaconOnPrimary     = Color(0xFFFFFFFF)

// Secondary accent – receive (emerald teal)
val BeaconSecondary     = Color(0xFF00C9A7)
val BeaconSecondaryDark = Color(0xFF00534A)
val BeaconOnSecondary   = Color(0xFFFFFFFF)

// Emergency
val BeaconEmergency     = Color(0xFFFF4444)
val BeaconEmergencyDark = Color(0xFF6B0000)

// Text
val BeaconOnBackground  = Color(0xFFE8EDF7)
val BeaconOnSurface     = Color(0xFFE8EDF7)
val BeaconTextMuted     = Color(0xFF7A8BA8)