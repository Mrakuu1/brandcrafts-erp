package com.brandcrafts.erp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// BrandCrafts enterprise blue palette. These values are consumed only by the app theme.
internal val BrandBlue = Color(0xFF2563EB)
private val BrandBlueDark = Color(0xFFB7C9FF)
private val BrandBlueContainer = Color(0xFFDCEAFF)
private val BrandBlueContainerDark = Color(0xFF004A9B)

private val Slate = Color(0xFF475569)
private val SlateDark = Color(0xFFBCC7DC)
private val SlateContainer = Color(0xFFE2E8F0)
private val SlateContainerDark = Color(0xFF303B4B)

private val Info = Color(0xFF0284C7)
private val InfoDark = Color(0xFF7DD3FC)
private val InfoContainer = Color(0xFFDFF3FF)
private val InfoContainerDark = Color(0xFF004B70)

private val Error = Color(0xFFDC2626)
private val ErrorDark = Color(0xFFFFB4AB)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorContainerDark = Color(0xFF93000A)

private val LightBackground = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE8EDF5)
private val LightOutline = Color(0xFF707887)
private val LightOnSurface = Color(0xFF171C24)
private val LightOnSurfaceVariant = Color(0xFF424A58)

private val DarkBackground = Color(0xFF10141A)
private val DarkSurface = Color(0xFF171C24)
private val DarkSurfaceVariant = Color(0xFF303642)
private val DarkOutline = Color(0xFF8C94A3)
private val DarkOnSurface = Color(0xFFEFF1F8)
private val DarkOnSurfaceVariant = Color(0xFFC2C7D2)

internal val BrandCraftsLightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueContainer,
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = Color(0xFF0B1D31),
    tertiary = Info,
    onTertiary = Color.White,
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = Color(0xFF001E2E),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFC0C7D3),
    inverseSurface = Color(0xFF2C313A),
    inverseOnSurface = Color(0xFFF0F1F7),
    inversePrimary = BrandBlueDark,
    surfaceTint = BrandBlue,
)

internal val BrandCraftsDarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF002E67),
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = Color(0xFFDCEAFF),
    secondary = SlateDark,
    onSecondary = Color(0xFF273240),
    secondaryContainer = SlateContainerDark,
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = InfoDark,
    onTertiary = Color(0xFF00344E),
    tertiaryContainer = InfoContainerDark,
    onTertiaryContainer = Color(0xFFDFF3FF),
    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF424955),
    inverseSurface = Color(0xFFE0E2E9),
    inverseOnSurface = Color(0xFF2D313A),
    inversePrimary = BrandBlue,
    surfaceTint = BrandBlueDark,
)
