package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val EchoDarkColorScheme = darkColorScheme(
    primary = SignalAmber,
    onPrimary = Color(0xFF1A0800),
    primaryContainer = SignalAmberContainerDark,
    onPrimaryContainer = Color(0xFFFFD8C8),
    secondary = PhosphorEmerald,
    onSecondary = Color(0xFF002116),
    secondaryContainer = PhosphorContainerDark,
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = TapeGold,
    onTertiary = Color(0xFF251700),
    tertiaryContainer = TapeGoldContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = CrimsonContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    background = SlateVoid,
    onBackground = IvoryText,
    surface = SlateSurface,
    onSurface = IvoryText,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = SlateVoid,
    surfaceContainerLow = SlateSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHigh,
    outline = SlateOutline,
    outlineVariant = Color(0xFF282D38)
)

private val EchoLightColorScheme = lightColorScheme(
    primary = SignalAmberDark,
    onPrimary = Color.White,
    primaryContainer = SignalAmberContainerLight,
    onPrimaryContainer = Color(0xFF4D1800),
    secondary = PhosphorEmeraldDark,
    onSecondary = Color.White,
    secondaryContainer = PhosphorContainerLight,
    onSecondaryContainer = Color(0xFF023E29),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = TapeGoldContainerLight,
    onTertiaryContainer = Color(0xFF451A03),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = CrimsonContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
    background = WarmPaper,
    onBackground = CharcoalText,
    surface = BrushedWhite,
    onSurface = CharcoalText,
    surfaceVariant = SoftStone,
    onSurfaceVariant = MutedCharcoalText,
    surfaceContainerLowest = BrushedWhite,
    surfaceContainerLow = WarmPaper,
    surfaceContainer = SoftStone,
    surfaceContainerHigh = HighStone,
    outline = StoneOutline,
    outlineVariant = Color(0xFFDFDAD0)
)

val EchoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EchoDeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EchoDarkColorScheme
        else -> EchoLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EchoTypography,
        shapes = EchoShapes,
        content = content
    )
}
