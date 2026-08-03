package com.brandcrafts.erp.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Warm, compact consumer-style palette derived from the supplied light and dark references.
 * Components apply translucency from these semantic colors rather than hard-coding screen colors.
 */
internal val BrandOrange = Color(0xFFFF6B00)
internal val BrandOrangeDark = Color(0xFFFF7A00)
internal val BrandOrangeContainer = Color(0xFFFFE1CC)
internal val BrandOrangeContainerDark = Color(0xFF4A2108)

private val Ink = Color(0xFF17120E)
private val MutedInk = Color(0xFF6D625A)
private val WarmSurface = Color(0xFFFFFCFA)
private val WarmSurfaceVariant = Color(0xFFFFF3EA)
private val WarmOutline = Color(0xFFE7D8CC)

// Shared dark surfaces use the approved blue-navy hierarchy, not warm brown.
private val Night = Color(0xFF070D14)
private val NightSurface = Color(0xFF111A25)
private val NightSurfaceVariant = Color(0xFF16212E)
private val NightInk = Color(0xFFF8FAFC)
private val NightMutedInk = Color(0xFFB2BBC6)
private val NightOutline = Color(0xFF283646)

private val Success = Color(0xFF267C48)
private val SuccessContainer = Color(0xFFD9F5E2)
private val Info = Color(0xFF386FA4)
private val InfoContainer = Color(0xFFDCEBFF)
private val Error = Color(0xFFBA1A1A)
private val ErrorContainer = Color(0xFFFFDAD6)

internal val BrandCraftsLightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeContainer,
    onPrimaryContainer = Color(0xFF351000),
    secondary = Success,
    onSecondary = Color.White,
    secondaryContainer = SuccessContainer,
    onSecondaryContainer = Color(0xFF092114),
    tertiary = Info,
    onTertiary = Color.White,
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = Color(0xFF001D35),
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFEFD),
    onBackground = Ink,
    surface = WarmSurface,
    onSurface = Ink,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedInk,
    outline = WarmOutline,
    outlineVariant = Color(0xFFF3E9E2),
    inverseSurface = Color(0xFF302923),
    inverseOnSurface = Color(0xFFFFF7F1),
    inversePrimary = BrandOrangeDark,
    surfaceTint = BrandOrange,
)

internal val BrandCraftsDarkColorScheme = darkColorScheme(
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF351000),
    primaryContainer = BrandOrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFDCC6),
    secondary = Color(0xFF8AD9A5),
    onSecondary = Color(0xFF00391C),
    secondaryContainer = Color(0xFF00522A),
    onSecondaryContainer = Color(0xFFA7F5BF),
    tertiary = Color(0xFFA8C8FF),
    onTertiary = Color(0xFF00315A),
    tertiaryContainer = Color(0xFF004879),
    onTertiaryContainer = Color(0xFFD5E3FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ErrorContainer,
    background = Night,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedInk,
    outline = NightOutline,
    outlineVariant = Color(0xFF283646),
    inverseSurface = Color(0xFFFFEDE2),
    inverseOnSurface = Color(0xFF261B15),
    inversePrimary = BrandOrange,
    surfaceTint = BrandOrangeDark,
)
