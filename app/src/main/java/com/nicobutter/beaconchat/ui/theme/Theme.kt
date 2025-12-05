package com.nicobutter.beaconchat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Dark color scheme for BeaconChat theme.
 *
 * Uses Material Design's standard dark theme colors with purple and pink accents.
 * Currently not used as the app forces light theme for consistency.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * Light color scheme for BeaconChat theme.
 *
 * Custom light theme using BeaconChat's branded colors with deep purple primary
 * and teal secondary colors, optimized for readability and visual appeal.
 */
private val LightColorScheme = lightColorScheme(
    primary = BeaconPrimary,
    secondary = BeaconSecondary,
    tertiary = Pink40,
    background = BeaconBackground,
    surface = BeaconSurface,
    onPrimary = BeaconOnPrimary,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = BeaconOnBackground,
    onSurface = BeaconOnSurface
)

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> LightColorScheme  // Force light theme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}