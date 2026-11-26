package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinemaDarkColorScheme = darkColorScheme(
    primary = CyanElectric,
    onPrimary = CyanDeep,
    primaryContainer = CyanContainer,
    onPrimaryContainer = CyanOnContainer,
    secondary = IndigoAccent,
    onSecondary = MidnightBg,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = IndigoOnContainer,
    tertiary = AmberWarning,
    onTertiary = MidnightBg,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberOnContainer,
    background = MidnightBg,
    onBackground = IcePrimary,
    surface = MidnightSurface,
    onSurface = IcePrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = SlateSecondary,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineSubtle,
    error = CoralError,
    onError = MidnightBg,
    errorContainer = CoralContainer,
    onErrorContainer = CoralOnContainer,
)

@Composable
fun TvRemoteLauncherTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinemaDarkColorScheme,
        typography = AppTypography,
        content = content,
    )
}
