package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MahadevLightColorScheme =
  lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = CrimsonDeep,
    secondary = SacredGold,
    onSecondary = Color.White,
    secondaryContainer = SacredGoldLight,
    onSecondaryContainer = HarvestAmber,
    tertiary = HarvestAmber,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = MaroonInk,
    surface = PureCardSurface,
    onSurface = MaroonInk,
    surfaceVariant = ParchmentSurface,
    onSurfaceVariant = WarmMutedInk,
    outline = WarmOutline,
    outlineVariant = WarmOutline,
  )

private val MahadevDarkColorScheme =
  darkColorScheme(
    primary = DarkCrimsonPrimary,
    onPrimary = Color(0xFF4E0002),
    primaryContainer = DarkCrimsonContainer,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = DarkGoldSecondary,
    onSecondary = Color(0xFF3E2B00),
    secondaryContainer = Color(0xFF563D00),
    onSecondaryContainer = Color(0xFFFFE09B),
    background = DarkEspressoBackground,
    onBackground = Color(0xFFF5EAE0),
    surface = DarkEspressoSurface,
    onSurface = Color(0xFFF5EAE0),
    surfaceVariant = Color(0xFF332622),
    onSurfaceVariant = Color(0xFFD5C4B8),
    outline = Color(0xFF524039),
  )

val MahadevShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun MahadevTradersTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) MahadevDarkColorScheme else MahadevLightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = MahadevShapes,
    content = content,
  )
}
