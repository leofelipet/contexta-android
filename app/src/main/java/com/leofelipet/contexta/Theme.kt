package com.leofelipet.contexta

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val Ink = Color(0xFF17211D)
internal val Muted = Color(0xFF66736D)
internal val Line = Color(0xFFDFE7E2)
internal val Paper = Color(0xFFF8FAF8)
internal val SurfaceWhite = Color(0xFFFFFFFF)
internal val Brand = Color(0xFF087F5B)
internal val BrandDark = Color(0xFF056044)
internal val BrandSoft = Color(0xFFE7F5EF)
internal val Chat = Color(0xFFF1EEE7)
internal val OutboundBubble = Color(0xFFD9FDD3)

private val ContextaColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandSoft,
    onPrimaryContainer = BrandDark,
    secondary = Color(0xFF12352B),
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    onSurfaceVariant = Muted,
    outline = Line,
    surfaceVariant = BrandSoft,
    error = Color(0xFFB91C1C),
    onError = Color.White,
)

@Composable
fun ContextaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ContextaColors, content = content)
}

@Composable
fun contextaFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    disabledTextColor = Muted,
    focusedBorderColor = Brand,
    unfocusedBorderColor = Line,
    focusedLabelColor = Brand,
    unfocusedLabelColor = Muted,
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    cursorColor = Brand,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
)
