package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PolyForgeDarkScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF00262C),
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = SculptAmber,
    onSecondary = Color(0xFF2A1200),
    secondaryContainer = SculptAmberContainer,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = ManifoldEmerald,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = ManifoldEmeraldContainer,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    error = AxisRedX
)

val PolyForgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PolyForgeDarkScheme,
        typography = Typography,
        shapes = PolyForgeShapes,
        content = content
    )
}
