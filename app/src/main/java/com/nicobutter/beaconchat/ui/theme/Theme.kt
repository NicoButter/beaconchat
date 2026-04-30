package com.nicobutter.beaconchat.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Dark color scheme for BeaconChat theme.
 *
 * Deep navy background with electric blue and emerald teal accents,
 * optimized for a dark-first optical communication interface.
 */
private val DarkColorScheme = darkColorScheme(
    primary = BeaconPrimary,
    onPrimary = BeaconOnPrimary,
    secondary = BeaconSecondary,
    onSecondary = BeaconOnSecondary,
    tertiary = BeaconEmergency,
    background = BeaconBackground,
    surface = BeaconSurface,
    onBackground = BeaconOnBackground,
    onSurface = BeaconOnSurface
)

/**
 * Light color scheme for BeaconChat theme (alias to dark for consistency).
 */
private val LightColorScheme = DarkColorScheme

/**
 * BeaconChat Material Design 3 theme provider.
 *
 * Applies consistent theming across the entire application using Material Design 3
 * components. Currently forces light theme for optimal user experience and
 * supports dynamic colors on Android 12+ devices.
 *
 * @param darkTheme Whether to use dark theme (currently forced to false for consistency)
 * @param dynamicColor Whether to use dynamic colors from system theme (Android 12+ only)
 * @param content The composable content to be themed
 */
@Composable
fun BeaconChatTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}