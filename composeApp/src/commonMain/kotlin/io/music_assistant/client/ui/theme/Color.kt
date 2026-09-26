package io.music_assistant.client.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Chase's Retreat theme: mountain lodge palette. Forest green primary (the house flag),
// warm timber-brown secondary, and sky-blue tertiary, over warm cream/tan neutrals instead
// of a cool white/grey — the neutrals carry as much of the "mountain lodge" feel as the
// accent colors do, so the app doesn't read as a grey app with a few green icons.

// ---- Light ----
val primaryLight = Color(0xFF1E8E3E)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFB6F0B9)
val onPrimaryContainerLight = Color(0xFF07310E)
val secondaryLight = Color(0xFF8B6D3F)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFEFDDBB)
val onSecondaryContainerLight = Color(0xFF2B1D00)
val tertiaryLight = Color(0xFF3E7CA6)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFCFE7F5)
val onTertiaryContainerLight = Color(0xFF001E2E)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFEFEBDA)
val onBackgroundLight = Color(0xFF1D1C15)
val surfaceLight = Color(0xFFEFEBDA)
val onSurfaceLight = Color(0xFF1D1C15)
val surfaceVariantLight = Color(0xFFDDD7C4)
val onSurfaceVariantLight = Color(0xFF48453A)
val outlineLight = Color(0xFF7A7768)
val outlineVariantLight = Color(0xFFC0BAA0)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF32302A)
val inverseOnSurfaceLight = Color(0xFFF3F1E7)
val inversePrimaryLight = Color(0xFF7EDB86)
val surfaceDimLight = Color(0xFFD0C9B0)
val surfaceBrightLight = Color(0xFFEFEBDA)
val surfaceContainerLowestLight = Color(0xFFF5F2E6)
val surfaceContainerLowLight = Color(0xFFE9E4D0)
val surfaceContainerLight = Color(0xFFE3DDC6)
val surfaceContainerHighLight = Color(0xFFDDD7BE)
val surfaceContainerHighestLight = Color(0xFFD7D0B6)

// ---- Dark ----
val primaryDark = Color(0xFF7EDB86)
val onPrimaryDark = Color(0xFF07310E)
val primaryContainerDark = Color(0xFF13591F)
val onPrimaryContainerDark = Color(0xFFB6F0B9)
val secondaryDark = Color(0xFFD9BC8C)
val onSecondaryDark = Color(0xFF3D2E0F)
val secondaryContainerDark = Color(0xFF564322)
val onSecondaryContainerDark = Color(0xFFEFDDBB)
val tertiaryDark = Color(0xFF9ECBEA)
val onTertiaryDark = Color(0xFF00344A)
val tertiaryContainerDark = Color(0xFF1F4E68)
val onTertiaryContainerDark = Color(0xFFCFE7F5)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF19180F)
val onBackgroundDark = Color(0xFFE6E3D6)
val surfaceDark = Color(0xFF19180F)
val onSurfaceDark = Color(0xFFE6E3D6)
val surfaceVariantDark = Color(0xFF48453A)
val onSurfaceVariantDark = Color(0xFFCAC6B7)
val outlineDark = Color(0xFF938F80)
val outlineVariantDark = Color(0xFF48453A)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE6E3D6)
val inverseOnSurfaceDark = Color(0xFF32302A)
val inversePrimaryDark = Color(0xFF1E8E3E)
val surfaceDimDark = Color(0xFF19180F)
val surfaceBrightDark = Color(0xFF403E33)
val surfaceContainerLowestDark = Color(0xFF141309)
val surfaceContainerLowDark = Color(0xFF1D1C15)
val surfaceContainerDark = Color(0xFF221F16)
val surfaceContainerHighDark = Color(0xFF2C2920)
val surfaceContainerHighestDark = Color(0xFF37342A)

/**
 * Accent used for the "favorite" heart indicator across the app.
 * Bound to the [androidx.compose.material3.ColorScheme.tertiary] role so it harmonizes with
 * the scheme and tracks light/dark automatically.
 */
val favoriteTint: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.tertiary
