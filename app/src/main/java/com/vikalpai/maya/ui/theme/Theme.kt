package com.vikalpai.maya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MayaColorScheme = darkColorScheme(
    background = MayaBackground,
    surface = MayaSurface,
    primary = MayaElectricBlue,
    secondary = MayaPurple,
    onBackground = MayaTextPrimary,
    onSurface = MayaTextPrimary
)

@Composable
fun MayaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MayaColorScheme,
        typography = MayaTypography,
        content = content
    )
}
