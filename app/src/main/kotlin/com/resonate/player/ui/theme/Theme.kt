package com.resonate.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.resonate.player.data.prefs.ThemeMode

@Composable
fun ResonateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = false,
    materialYou: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    var colors = if (darkTheme) DarkResonateColors else LightResonateColors

    // §1.2: OLED black is a setting, not an aesthetic you can't escape.
    if (amoledBlack && darkTheme) {
        colors = colors.copy(
            ink = Color.Black,
            surface = Color(0xFF0B0B0F),
            surfaceRaised = Color(0xFF16161C),
            hairline = Color(0xFF232329),
        )
    }

    // §2.7: Material You fills the fallback accent slot with the system palette.
    if (materialYou && Build.VERSION.SDK_INT >= 31) {
        val context = LocalContext.current
        val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        colors = colors.copy(accent = dynamic.primary, onAccent = dynamic.onPrimary)
    }

    val typography = ResonateTypography()

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            secondary = colors.mint,
            onSecondary = colors.ink,
            background = colors.ink,
            onBackground = colors.bone,
            surface = colors.ink,
            onSurface = colors.bone,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.muted,
            surfaceContainerLowest = colors.ink,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surfaceRaised,
            surfaceContainerHighest = colors.surfaceRaised,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
            error = colors.pulse,
            onError = colors.ink,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = SurfaceLight,
            secondary = colors.mint,
            onSecondary = colors.bone,
            background = colors.ink,
            onBackground = colors.bone,
            surface = colors.ink,
            onSurface = colors.bone,
            surfaceVariant = colors.surfaceRaised,
            onSurfaceVariant = colors.muted,
            surfaceContainerLowest = colors.surface,
            surfaceContainerLow = colors.surface,
            surfaceContainer = colors.surface,
            surfaceContainerHigh = colors.surfaceRaised,
            surfaceContainerHighest = colors.surfaceRaised,
            outline = colors.hairline,
            outlineVariant = colors.hairline,
            error = colors.pulse,
            onError = SurfaceLight,
        )
    }

    CompositionLocalProvider(
        LocalResonateColors provides colors,
        LocalResonateTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography.toMaterial(),
            shapes = ResonateShapes,
            content = content,
        )
    }
}

/** Accessor mirroring MaterialTheme's pattern: `ResonateTheme.colors.accent`. */
object ResonateTheme {
    val colors: ResonateColors
        @Composable @ReadOnlyComposable get() = LocalResonateColors.current
    val type: ResonateTypography
        @Composable @ReadOnlyComposable get() = LocalResonateTypography.current
}
