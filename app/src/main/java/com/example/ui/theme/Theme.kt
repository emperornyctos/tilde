package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.LauncherSettings

@Immutable
data class LauncherThemeTokens(
    val background: Color,
    val surface: Color,
    val border: Color,
    val accent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val pillBg: Color,
    val pillText: Color,
    val borderWidth: Dp,
    val cardCornerRadius: Dp,
    val isLight: Boolean,
    val uppercaseHeaders: Boolean,
    val italicizeHeaders: Boolean
)

val LocalLauncherTokens = staticCompositionLocalOf {
    LauncherThemeTokens(
        background = DefaultTerminalBg,
        surface = DefaultTerminalSurface,
        border = DefaultTerminalBorder,
        accent = DefaultTerminalAccent,
        textPrimary = DefaultTerminalText,
        textMuted = DefaultTerminalMuted,
        pillBg = DefaultTerminalPillBg,
        pillText = DefaultTerminalPillText,
        borderWidth = 1.dp,
        cardCornerRadius = 2.dp,
        isLight = false,
        uppercaseHeaders = false,
        italicizeHeaders = true
    )
}

@Composable
fun TildeTheme(
    settings: LauncherSettings = LauncherSettings(),
    content: @Composable () -> Unit
) {
    val bg = Color(settings.backgroundColor)
    val surfaceRaw = Color(settings.surfaceColor)
    val surface = surfaceRaw.copy(alpha = settings.surfaceOpacity.coerceIn(0.25f, 1f))
    val border = Color(settings.borderColor)
    val accent = Color(settings.accentColor)
    val textPrimary = Color(settings.textColor)
    val textMuted = Color(settings.mutedTextColor)
    val pillBg = Color(settings.pillBackgroundColor)
    val pillText = Color(settings.pillTextColor)
    val corner = settings.cardCornerRadiusDp.dp

    val colorScheme = if (settings.isLightTheme) {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = pillBg,
            onPrimaryContainer = pillText,
            secondary = accent.copy(alpha = 0.8f),
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaw,
            onSurfaceVariant = textMuted,
            outline = border,
            outlineVariant = border.copy(alpha = 0.5f)
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = bg,
            primaryContainer = pillBg,
            onPrimaryContainer = pillText,
            secondary = accent.copy(alpha = 0.8f),
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceRaw,
            onSurfaceVariant = textMuted,
            outline = border,
            outlineVariant = border.copy(alpha = 0.5f)
        )
    }

    val tokens = LauncherThemeTokens(
        background = bg,
        surface = surface,
        border = border,
        accent = accent,
        textPrimary = textPrimary,
        textMuted = textMuted,
        pillBg = pillBg,
        pillText = pillText,
        borderWidth = settings.cardBorderWidthDp.dp,
        cardCornerRadius = corner,
        isLight = settings.isLightTheme,
        uppercaseHeaders = settings.uppercaseSectionHeaders,
        italicizeHeaders = settings.italicizeHeaders
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(corner),
        small = RoundedCornerShape(corner),
        medium = RoundedCornerShape(corner),
        large = RoundedCornerShape((corner.value + 2f).dp),
        extraLarge = RoundedCornerShape((corner.value + 4f).dp)
    )

    CompositionLocalProvider(LocalLauncherTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildLauncherTypography(settings),
            shapes = shapes,
            content = content
        )
    }
}
